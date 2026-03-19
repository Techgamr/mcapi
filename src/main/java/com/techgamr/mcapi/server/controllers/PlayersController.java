package com.techgamr.mcapi.server.controllers;

import com.techgamr.mcapi.server.ServerUtils;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.stream.Collectors;

public class PlayersController {
    public static void countPlayers(Context ctx) {
        ctx.future(() -> ServerUtils.executeOnServer(ctx, MinecraftServer::getPlayerCount).thenAccept(count -> ctx.json(Map.of("count", count))).exceptionally(e -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            return null;
        }));
    }

    public static void listPlayers(Context ctx) {
        ctx.future(() -> ServerUtils.executeOnServer(ctx, MinecraftServer::getPlayerList).thenAccept(pl -> ctx.json(pl.getPlayers().stream().map(p -> Map.ofEntries(Map.entry("name", p.getName().getString()), Map.entry("uuid", p.getUUID()))).collect(Collectors.toList()))).exceptionally(e -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            return null;
        }));
    }
}
