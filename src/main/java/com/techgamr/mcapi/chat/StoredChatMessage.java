package com.techgamr.mcapi.chat;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.techgamr.mcapi.utils.ComponentJsonSerializer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.ServerChatEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record StoredChatMessage(
        @Nullable UUID playerUuid,
        @Nullable String playerUsername,
        @NotNull
        @JsonSerialize(using = ComponentJsonSerializer.class)
        Component message,
        @NotNull String rawMessage,
        boolean isViaApi
) {
    public StoredChatMessage(@NotNull ServerChatEvent event) {
        this(event.getPlayer().getUUID(), event.getUsername(), event.getMessage(), event.getRawText(), false);
    }
}
