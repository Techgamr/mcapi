package com.techgamr.mcapi.server;

import com.mojang.logging.LogUtils;
import com.techgamr.mcapi.McApi;
import com.techgamr.mcapi.utils.Utils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerConfig {
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve(McApi.MODID + "/config.json");
    private static final Logger LOGGER = LogUtils.getLogger();

    // config values
    public String host = "127.0.0.1";
    public int port = 3333;
    public boolean proxy_auth = false;

    public static ServerConfig readFromFile() {
        LOGGER.info("loading config");
        ServerConfig result = new ServerConfig();
        try (FileReader reader = new FileReader(CONFIG_FILE.toFile())) {
            result = Utils.GSON.fromJson(reader, ServerConfig.class);
            LOGGER.info("Successfully loaded config from file");
        } catch (IOException e) {
            LOGGER.error("Failed to read config, writing defaults", e);
            try {
                Files.createDirectories(CONFIG_FILE.getParent());
                Files.writeString(CONFIG_FILE, Utils.GSON.toJson(result));
            } catch (IOException f) {
                LOGGER.error("Failed to save config defaults", e);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config, using defaults", e);
        }
        return result;
    }
}
