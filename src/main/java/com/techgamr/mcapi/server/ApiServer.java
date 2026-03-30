package com.techgamr.mcapi.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.mojang.logging.LogUtils;
import com.techgamr.mcapi.ctm.TrackWatcher;
import com.techgamr.mcapi.server.controllers.ChatController;
import com.techgamr.mcapi.server.controllers.CreateController;
import com.techgamr.mcapi.server.controllers.InfoController;
import com.techgamr.mcapi.server.controllers.PlayersController;
import io.javalin.Javalin;
import io.javalin.config.Key;
import io.javalin.json.JavalinJackson;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;

import static io.javalin.apibuilder.ApiBuilder.*;

public class ApiServer {
    private Javalin app;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Key<MinecraftServer> CTX_APPDATA_SERVER_KEY = new Key<>("mcserver");
    public static final Key<ServerConfig> CTX_APPDATA_CONFIG_KEY = new Key<>("config");
    public static final Key<TrackWatcher> CTX_APPDATA_TRACK_WATCHER_KEY = new Key<>("track_watcher");

    public void start(MinecraftServer server) {
        ServerConfig serverCfg = ServerConfig.readFromFile();
        TrackWatcher trackWatcher = new TrackWatcher(serverCfg.track_watch_interval_millis);
        app = Javalin.create(config -> {
            // JSON config & plugins
            config.jsonMapper(new JavalinJackson(
                    new ObjectMapper().registerModule(new Jdk8Module()), true
            ));
            // App data
            config.appData(CTX_APPDATA_SERVER_KEY, server);
            config.appData(CTX_APPDATA_CONFIG_KEY, serverCfg);
            config.appData(CTX_APPDATA_TRACK_WATCHER_KEY, trackWatcher);
            // After stop, stop the track watcher
            config.events.serverStopped(trackWatcher::stop);
            config.events.serverStopFailed(trackWatcher::stop);
            // Add auth access handler
            config.routes.beforeMatched(Auth::handleAccess);
            // Routes
            config.routes.apiBuilder(() -> {
                config.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
                        it.anyHost();
//                        it.reflectClientOrigin = true;
                        it.maxAge = 3600;
                    });
                });
                get("/", ctx -> {
                    UUID uuid = Auth.getUuid(ctx);
                    ctx.json(Map.of("uuid", uuid == null ? NullNode.instance : uuid));
                }, Role.OPEN);
                if (serverCfg.proxy_auth) {
                    get("/auth/{hash}", Auth::handleAuthApi, Role.AUTHORISED_PROXY);
                }
                path("info", () -> {
                    get(InfoController::info, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    get("/server-icon", InfoController::serverIcon, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    get("/mod-logo/{modid}", InfoController::modLogo, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                });
                path("players", () -> {
                    get(PlayersController::listPlayers, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    path("me", () -> {
                        get(PlayersController::playerInfo, Role.USER_LOGGED_IN);
                    });
                    path("count", () -> {
                        get(PlayersController::countPlayers, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    });
                    path("{id}", () -> {
                        get(PlayersController::playerInfo, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    });
                });
                path("chat", () -> {
                    get(ChatController::getChat, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    post(ChatController::sendBroadcast, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    path("{player}", () -> {
                        post(ChatController::sendMessage, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    });
                });
                path("createmod", () -> {
                    path("stations", () -> {
                        get(CreateController::getAllStations, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    });
                    path("train", () -> {
                        get(CreateController::getTrains, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        post(CreateController::setTrain, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        path("{id}", () -> {
                            get(CreateController::getTrain, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                            post(CreateController::setTrain, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        });
                        path("by-name/{name}", () -> {
                            get(CreateController::getTrainByName, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        });
                    });
                    path("track", () -> {
                        get("/network", CreateController::trackNetwork, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        get("/signals", CreateController::trackSignals, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        get("/blocks", CreateController::trackBlocks, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        get("/trains", CreateController::trackTrains, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        sse("/network.rt", trackWatcher::networkSSE, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        sse("/signals.rt", trackWatcher::signalSSE, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        sse("/blocks.rt", trackWatcher::blockSSE, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                        sse("/trains.rt", trackWatcher::trainSSE, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    });
                });
            });
        }).start(serverCfg.host, serverCfg.port);
    }

    public void restart(MinecraftServer server) {
        LOGGER.info("Restarting Javalin server");
        stop();
        start(server);
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }
}
