package com.techgamr.mcapi.server;

import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import com.techgamr.mcapi.McApi;
import com.techgamr.mcapi.Utils;
import io.javalin.http.*;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class Auth {
    private static final String UUID_HEADER = "X-Mcapi-Uuid";
    private static final Path API_KEYS_FILE = FMLPaths.CONFIGDIR.get().resolve(McApi.MODID + "/apikeys.json");
    private static Map<String, String> playerApiKeys = null;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SecureRandom random = new SecureRandom();

    public static String generateApiKey(UUID playerUuid) {
        LOGGER.info("Regenerating apikey for {}", playerUuid);
        if (playerApiKeys == null) {
            loadFromFile();
        }
        // generate apikey
        byte[] apiKey = new byte[32];
        random.nextBytes(apiKey);
        // generate and save hash
        byte[] apiKeyHash;
        try {
            apiKeyHash = MessageDigest.getInstance("SHA-256").digest(apiKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        // remove existing keys from the DB
        playerApiKeys.values().removeIf(value -> value.equals(playerUuid.toString()));
        // add the new key to the DB
        playerApiKeys.put(Utils.btoa(apiKeyHash), playerUuid.toString());
        // save the DB
        saveToFile();
        // return base64 apikey to player
        return Utils.btoa(apiKey);
    }

    public static @Nullable String getUuid(String hashedApiKey) {
        if (playerApiKeys == null) {
            loadFromFile();
        }
        return playerApiKeys.get(hashedApiKey);
    }

    /**
     * Get the UUID for a given request context.
     *
     * @return null if this request is not authenticated with a UUID,
     * a Utils.NULL_UUID if authenticated but no player is specifically attached (sent by the proxy),
     * or the player's UUID if authenticated to a specific player.
     */
    public static @Nullable UUID getUuid(@NotNull Context ctx) {
        var serverConfig = ctx.appData(ApiServer.CTX_APPDATA_CONFIG_KEY);
        if (serverConfig.proxy_auth) {
            try {
                return UUID.fromString(Objects.requireNonNull(ctx.header(UUID_HEADER)));
            } catch (Exception e) {
                return null;
            }
        } else {
            try {
                var authHeader = ctx.header(Header.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return null;
                }
                authHeader = authHeader.substring(7); // Remove "Bearer " prefix
                var hashedApiKey = Utils.btoa(MessageDigest.getInstance("SHA-256")
                        .digest(Utils.atob(authHeader)));
                var uuid = UUID.fromString(Objects.requireNonNull(getUuid(hashedApiKey)));
                if (uuid.equals(Utils.NULL_UUID)) {
                    LOGGER.error("null UUID found in auth file");
                    return null;
                }
                else {
                    return uuid;
                }
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * The before handler in requests for authentication.
     */
    public static void handleAccess(@NotNull Context ctx) {
        var permittedRoles = ctx.routeRoles();
        var uuid = getUuid(ctx);
        if (
                permittedRoles.contains(Role.OPEN) ||
                        permittedRoles.contains(Role.USER_LOGGED_IN) && uuid != null && !uuid.equals(Utils.NULL_UUID) ||
                        permittedRoles.contains(Role.AUTHORISED_PROXY) && uuid != null && uuid.equals(Utils.NULL_UUID)
        ) {
            return; // allow access
        }
        throw new UnauthorizedResponse();
    }

    /**
     * The Proxy Auth API endpoint handler.
     */
    public static void handleAuthApi(@NotNull Context ctx) {
        var config = ctx.appData(ApiServer.CTX_APPDATA_CONFIG_KEY);
        if (!config.proxy_auth) {
            LOGGER.error("Proxy Auth API endpoint called with proxy auth disabled");
            throw new InternalServerErrorResponse();
        }

        String uuid = Auth.getUuid(ctx.pathParam("hash"));
        if (uuid == null) {
            throw new NotFoundResponse();
        } else {
            ctx.status(HttpStatus.OK).json(Map.of("uuid", uuid));
        }
    }

    private static void saveToFile() {
        try {
            Files.createDirectories(API_KEYS_FILE.getParent());
            Files.writeString(API_KEYS_FILE, Utils.GSON.toJson(playerApiKeys));
        } catch (IOException e) {
            LOGGER.error("Failed to save API keys", e);
        }
    }

    private static void loadFromFile() {
        try {
            if (Files.exists(API_KEYS_FILE)) {
                playerApiKeys = Utils.GSON.fromJson(Files.readString(API_KEYS_FILE), new TypeToken<Map<String, String>>() {
                }.getType());
                if (playerApiKeys == null) {
                    throw new IOException("playerApiKeys == null");
                }
            } else {
                throw new IOException("Api keys file does not exist");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load API keys", e);
        } finally {
            if (playerApiKeys == null) {
                playerApiKeys = new HashMap<>();
            }
        }
    }
}
