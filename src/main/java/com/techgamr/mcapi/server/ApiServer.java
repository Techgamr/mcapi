package com.techgamr.mcapi.server;

import com.mojang.logging.LogUtils;
import com.techgamr.mcapi.server.controllers.ChatController;
import com.techgamr.mcapi.server.controllers.CreateController;
import com.techgamr.mcapi.server.controllers.PlayersController;
import io.javalin.Javalin;
import io.javalin.config.Key;
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
            config.appData(CTX_APPDATA_SERVER_KEY, server);
            config.appData(CTX_APPDATA_CONFIG_KEY, serverCfg);
            config.routes.beforeMatched(Auth::handleAccess);
            config.routes.apiBuilder(() -> {
                get("/", ctx -> ctx.json("hello world!"), Role.OPEN);
                get("/auth/{hash}", Auth::handleAuthApi, Role.AUTHORISED_PROXY);
                path("players", () -> {
                    get(PlayersController::listPlayers, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
                    path("count", () -> {
                        get(PlayersController::countPlayers, Role.USER_LOGGED_IN, Role.AUTHORISED_PROXY);
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
