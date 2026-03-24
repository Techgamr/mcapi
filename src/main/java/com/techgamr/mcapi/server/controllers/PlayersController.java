package com.techgamr.mcapi.server.controllers;

import com.fasterxml.jackson.databind.node.NullNode;
import com.mojang.authlib.GameProfile;
import com.techgamr.mcapi.server.Auth;
import com.techgamr.mcapi.server.ServerUtils;
import com.techgamr.mcapi.utils.Utils;
import io.javalin.http.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayersController {
    public static void countPlayers(Context ctx) {
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
            return Map.of("count", srv.getPlayerCount(), "maxPlayers", srv.getMaxPlayers());
        }).thenAccept(ctx::json).exceptionally(e -> {
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

    public static void playerInfo(Context ctx) {
        UUID callingUuid = Auth.getUuid(ctx);
        assert callingUuid != null;
        UUID uuid;
        try {
            String pathParamString;
            try {
                pathParamString = ctx.pathParam("id");
            } catch (IllegalArgumentException ignored) {
                pathParamString = null;
            }
            uuid = pathParamString != null ? UUID.fromString(pathParamString) : callingUuid;
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Invalid UUID format");
        }
        if (uuid.equals(Utils.NULL_UUID)) {
            throw new BadRequestResponse("Cannot fetch this player's info for an authorized proxy call");
        }
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
                            Optional<GameProfile> profile = Optional.empty();
                            GameProfileCache profileCache = srv.getProfileCache();
                            if (profileCache != null) {
                                profile = profileCache.get(uuid);
                                if (profile.isPresent()) {
                                    return Map.of(
                                            "uuid", uuid,
                                            "name", profile.map(x -> (Object) x.getName()).orElse(NullNode.instance)
                                    );
                                } else {
                                    return null;
                                }
                            } else {
                                return null;
                            }
                        })
                        .thenAccept(x -> {
                            if (x != null) {
                                ctx.status(HttpStatus.OK).json(x);
                            } else {
                                throw new NotFoundResponse();
                            }
                        })
        );
    }
}
