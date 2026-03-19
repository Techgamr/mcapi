package com.techgamr.mcapi.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.Base64;
import java.util.UUID;

public class Utils {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final UUID NULL_UUID = new UUID(0L, 0L);
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String btoa(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static byte[] atob(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    public static JsonNode nbtToJson(CompoundTag tag) {
        try {
            return OBJECT_MAPPER
                    .readTree(
                            CompoundTag.CODEC
                                    .encodeStart(JsonOps.INSTANCE, tag)
                                    .getOrThrow(false, s -> LOGGER.error("Error serialising NBT: {}", s))
                                    .toString()
                    );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CompoundTag jsonToNbt(String jsonString) {
        try {
            return CompoundTag.CODEC
                    .parse(JsonOps.INSTANCE, JsonParser.parseString(jsonString))
                    .getOrThrow(false, s -> LOGGER.error("Error deserialising NBT: {}", s));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Component jsonToComponent(String jsonString) {
        try {
            return Component.Serializer.fromJson(jsonString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode componentToJson(Component component) {
        try {
            return OBJECT_MAPPER
                    .readTree(
                            Component.Serializer.toStableJson(component)
                    );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
