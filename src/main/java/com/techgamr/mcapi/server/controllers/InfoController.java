package com.techgamr.mcapi.server.controllers;

import com.techgamr.mcapi.server.Auth;
import com.techgamr.mcapi.server.ServerUtils;
import com.techgamr.mcapi.utils.Utils;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.NotFoundResponse;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class InfoController {
    public static void info(Context ctx) {
        UUID callingUuid = Auth.getUuid(ctx);
        if (callingUuid != null && callingUuid.equals(Utils.NULL_UUID)) {
            callingUuid = null;
        }
        UUID uuid = callingUuid;
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
            // This player
            Map<String, ? extends Serializable> playerData;
            if (uuid != null) {
                playerData = Optional.ofNullable(srv.getProfileCache())
                        .flatMap(c -> c.get(uuid))
                        .map(profile -> Map.of(
                                "uuid", uuid,
                                "name", profile.getName()
                        ))
                        .orElse(Map.of());
            } else {
                playerData = Map.of();
            }
            // Online players
            var onlinePlayers = Map.of(
                    "players", srv
                            .getPlayerList()
                            .getPlayers()
                            .stream()
                            .map(p -> Map.ofEntries(Map.entry("name", p.getName().getString()), Map.entry("uuid", p.getUUID())))
                            .toList(),
                    "playerCount", srv.getPlayerCount(),
                    "maxPlayers", srv.getMaxPlayers()
            );
            // Mods
            var modList = ModList
                    .get()
                    .getMods()
                    .stream()
                    .map(mod -> Map.of(
                            "modid", mod.getModId(),
                            "displayName", mod.getDisplayName(),
                            "description", mod.getDescription().trim(),
                            "url", mod.getConfig().getConfigElement("displayURL"),
                            "authors", mod.getConfig().getConfigElement("authors"),
                            "version", mod.getVersion().toString()
                    ))
                    .toList();
            return Map.of(
                    "mods", modList,
                    "player", playerData,
                    "onlinePlayers", onlinePlayers,
                    "motd", srv.getMotd()
            );
        }).thenAccept(res -> {
            ctx.status(HttpStatus.OK).json(res);
        }));
    }

    public static void modLogo(Context ctx) throws IOException {
        var logoPath = ModList.get().getModContainerById(ctx.pathParam("modid")).flatMap(m -> {
            IModInfo mi = m.getModInfo();
            return mi.getLogoFile().map(lf -> mi.getOwningFile().getFile().findResource(lf));
        }).orElse(null);
        if (logoPath == null) {
            throw new NotFoundResponse();
        } else try {
            ctx
                    .header("Cache-Control", "public, max-age=86400") // 1 day
                    .contentType(Objects.requireNonNullElse(Files.probeContentType(logoPath), "application/octet-stream"))
                    .status(HttpStatus.OK).result(Files.newInputStream(logoPath));
        } catch (FileNotFoundException e) {
            throw new NotFoundResponse();
        }
    }

    public static void serverIcon(Context ctx) {
        InputStream s;
        String contentType;
        try {
            Path path = FMLPaths.GAMEDIR.get().resolve("server-icon.png");
            s = Files.newInputStream(path);
            contentType = Objects.requireNonNullElse(Files.probeContentType(path), "application/octet-stream");
        } catch (IOException e) {
            s = MinecraftServer.class.getResourceAsStream("/assets/minecraft/textures/misc/unknown_server.png");
            if (s == null) {
                throw new InternalServerErrorResponse();
            }
            contentType = "image/png";
        }
        ctx
                .header("Cache-Control", "public, max-age=86400") // 1 day
                .contentType(contentType)
                .status(HttpStatus.OK).result(s);
    }
}
