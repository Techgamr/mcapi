package com.techgamr.mcapi.server.controllers;

import com.mojang.authlib.GameProfile;
import com.techgamr.mcapi.chat.ChatHandler;
import com.techgamr.mcapi.chat.StoredChatMessage;
import com.techgamr.mcapi.utils.Utils;
import com.techgamr.mcapi.server.Auth;
import com.techgamr.mcapi.server.ServerUtils;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.GameProfileCache;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ChatController {
    public static void getChat(Context ctx) {
        // try and get a specified length parameter
        var lenParam = ctx.queryParam("l");
        var requestedLength = -1;
        if (lenParam != null) {
            try {
                requestedLength = Integer.parseInt(lenParam);
            } catch (NumberFormatException e) {
                throw new BadRequestResponse("Length param l was not an integer");
            }
        }
        ctx.status(HttpStatus.OK).json(ChatHandler.getAllMessages(requestedLength));
    }

    public static void sendBroadcast(Context ctx) {
        UUID callingUuid = Auth.getUuid(ctx);
        String body = ctx.body();
        Component message = null;
        try {
            message = Utils.jsonToComponent(body);
        } catch (Exception e) {
            // do nothing
        }
        if (message == null) {
            message = Component.literal(body);
        }
        Component finalMessage = message;
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
            Component fullMessage = finalMessage;
            String playerName = null;
            if (callingUuid != null && !callingUuid.equals(Utils.NULL_UUID)) {
                String playerNameOrUuid = callingUuid.toString();
                GameProfileCache profileCache = srv.getProfileCache();
                if (profileCache != null) {
                    Optional<GameProfile> profile = profileCache.get(callingUuid);
                    if (profile.isPresent()) {
                        playerName = profile.get().getName();
                        playerNameOrUuid = playerName;
                    }
                }
                fullMessage = Component.literal("<" + playerNameOrUuid + " via API> ").append(fullMessage);
            }
            // add to the ChatHandler's list
            ChatHandler.addMessage(new StoredChatMessage(callingUuid, playerName, finalMessage, finalMessage.getString(), true));
            // broadcast to all players
            srv.getPlayerList().broadcastSystemMessage(fullMessage, false);
        }).thenAccept(pl -> ctx.status(HttpStatus.OK)).exceptionally(e -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            return null;
        }));
    }

    public static void sendMessage(Context ctx) {
        UUID callingUuid = Auth.getUuid(ctx);
        String playerParam = ctx.pathParam("player");
        UUID uuid = null;
        try {
            uuid = UUID.fromString(playerParam);
        } catch (Exception e) {
            // do nothing
        }
        Component message = null;
        try {
            message = Component.Serializer.fromJson(ctx.body());
        } catch (Exception e) {
            // do nothing
        }
        if (message == null) {
            message = Component.literal(ctx.body());
        }

        Component finalMessage = message;
        UUID finalUuid = uuid;
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
            Component fullMessage = finalMessage;
            if (callingUuid != null && !callingUuid.equals(Utils.NULL_UUID)) {
                String playerName = callingUuid.toString();
                GameProfileCache profileCache = srv.getProfileCache();
                if (profileCache != null) {
                    Optional<GameProfile> profile = profileCache.get(callingUuid);
                    if (profile.isPresent()) {
                        playerName = profile.get().getName();
                    }
                }
                fullMessage = Component.literal("<" + playerName + " via API> ").append(fullMessage);
            }
            if (finalUuid != null) {
                Objects.requireNonNull(srv.getPlayerList().getPlayer(finalUuid)).sendSystemMessage(fullMessage);
            } else {
                Objects.requireNonNull(srv.getPlayerList().getPlayerByName(playerParam)).sendSystemMessage(fullMessage);
            }
        }).thenAccept(pl -> ctx.status(HttpStatus.NO_CONTENT)));
    }
}
