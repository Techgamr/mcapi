package com.techgamr.mcapi.server.controllers;

import com.mojang.logging.LogUtils;
import com.techgamr.mcapi.Utils;
import com.techgamr.mcapi.server.Auth;
import com.techgamr.mcapi.server.ServerUtils;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.InternalServerErrorResponse;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatController {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void sendBroadcast(Context ctx) {
        UUID callingUuid = Auth.getUuid(ctx);
        String body = ctx.body();
        Component message = null;
        try {
            message = Component.Serializer.fromJson(body);
        } catch (Exception e) {
            // do nothing
        }
        if (message == null) {
            message = Component.literal(body);
        }
        Component finalMessage = message;
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
            ServerPlayer callingPlayer = callingUuid == null || callingUuid.equals(Utils.NULL_UUID) ? null : srv.getPlayerList().getPlayer(callingUuid);
            if (callingPlayer == null) {
                srv.getPlayerList().broadcastSystemMessage(finalMessage, false);
            } else {
                // TODO: send the message as the player
            }
        }).thenAccept(pl -> ctx.status(HttpStatus.OK)).exceptionally(e -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            return null;
        }));
    }

    public static void sendMessage(Context ctx) {
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
            if (finalUuid != null) {
                Objects.requireNonNull(srv.getPlayerList().getPlayer(finalUuid)).sendSystemMessage(finalMessage);
            } else {
                Objects.requireNonNull(srv.getPlayerList().getPlayerByName(playerParam)).sendSystemMessage(finalMessage);
            }
        }).thenAccept(pl -> ctx.status(HttpStatus.NO_CONTENT)));
    }
}
