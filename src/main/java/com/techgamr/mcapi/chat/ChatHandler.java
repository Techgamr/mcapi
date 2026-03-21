package com.techgamr.mcapi.chat;

import com.mojang.logging.LogUtils;
import com.techgamr.mcapi.McApi;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = McApi.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChatHandler {
    private static final CircularFifoQueue<StoredChatMessage> queue = new CircularFifoQueue<>(100);

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        addMessage(new StoredChatMessage(event));
    }

    public static void addMessage(StoredChatMessage message) {
        queue.add(message);
    }

    /**
     * Get the most recent (newest) element
     */
    public static StoredChatMessage getMostRecentMessage() {
        return queue.isEmpty() ? null : queue.peek();
    }

    /**
     * Get all elements as a list
     */
    public static List<StoredChatMessage> getAllMessages() {
        return getAllMessages(-1);
    }

    public static List<StoredChatMessage> getAllMessages(int limit) {
        List<StoredChatMessage> allElements = new ArrayList<>(queue);
        if (limit >= 0) {
            int startIndex = Math.max(0, allElements.size() - limit);
            return allElements.subList(startIndex, allElements.size());
        } else {
            return allElements;
        }
    }
}
