package com.techgamr.mcapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApiKeyManager {
    private static final Path API_KEYS_FILE = FMLPaths.CONFIGDIR.get().resolve(McApi.MODID + "/apikeys.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, String> playerApiKeys = null;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SecureRandom random = new SecureRandom();

    private static String btoa(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

//    private static byte[] atob(String data) {
//        return Base64.getUrlDecoder().decode(data);
//    }

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
        playerApiKeys.put(btoa(apiKeyHash), playerUuid.toString());
        // save the DB
        saveToFile();
        // return base64 apikey to player
        return btoa(apiKey);
    }

//    @Nullable
//    public static UUID validateApiKey(String input) {
//        if (playerApiKeys == null) {
//            loadFromFile();
//        }
//        try {
//            return playerApiKeys.get(btoa(MessageDigest.getInstance("SHA-256").digest(atob(input))));
//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException(e);
//        }
//    }

    @Nullable
    public static String getUuid(String hashedApiKey) {
        if (playerApiKeys == null) {
            loadFromFile();
        }
        return playerApiKeys.get(hashedApiKey);
    }

    private static void saveToFile() {
        try {
            Files.createDirectories(API_KEYS_FILE.getParent());
            Files.writeString(API_KEYS_FILE, gson.toJson(playerApiKeys));
        } catch (IOException e) {
            LOGGER.error("Failed to save API keys", e);
        }
    }

    private static void loadFromFile() {
        try {
            if (Files.exists(API_KEYS_FILE)) {
                playerApiKeys = gson.fromJson(Files.readString(API_KEYS_FILE), new TypeToken<Map<String, String>>(){}.getType());
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
