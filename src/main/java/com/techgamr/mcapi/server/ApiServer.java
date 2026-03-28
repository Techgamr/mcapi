package com.techgamr.mcapi.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.mojang.logging.LogUtils;
import com.techgamr.mcapi.server.controllers.ChatController;
import com.techgamr.mcapi.server.controllers.CreateController;
import com.techgamr.mcapi.server.controllers.InfoController;
import com.techgamr.mcapi.server.controllers.PlayersController;
import io.javalin.Javalin;
import io.javalin.config.Key;
import io.javalin.json.JavalinJackson;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import static io.javalin.apibuilder.ApiBuilder.*;

public class ApiServer {
    private Javalin app;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Key<MinecraftServer> CTX_APPDATA_SERVER_KEY = new Key<>("mcserver");
    public static final Key<ServerConfig> CTX_APPDATA_CONFIG_KEY = new Key<>("config");

    public void start(MinecraftServer server) {
        ServerConfig serverCfg = ServerConfig.readFromFile();
        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(
                    new ObjectMapper().registerModule(new Jdk8Module()), true
            ));
            config.appData(CTX_APPDATA_SERVER_KEY, server);
            config.appData(CTX_APPDATA_CONFIG_KEY, serverCfg);
            config.routes.beforeMatched(Auth::handleAccess);
            config.routes.apiBuilder(() -> {
                config.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
//                        it.anyHost();
                        it.reflectClientOrigin = true;
                        it.maxAge = 3600;
                    });
                });
                get("/", ctx -> ctx.json("hello world!"), Role.OPEN);
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
