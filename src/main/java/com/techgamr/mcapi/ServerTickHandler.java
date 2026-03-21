package com.techgamr.mcapi;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = McApi.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerTickHandler {
    private record TickCallback(Predicate<TickEvent.ServerTickEvent> filter, Consumer<TickEvent.ServerTickEvent> action) {}

    private static final List<TickCallback> callbacks = new ArrayList<>();

    public static void registerCallback(Predicate<TickEvent.ServerTickEvent> filter, Consumer<TickEvent.ServerTickEvent> action) {
        callbacks.add(new TickCallback(filter, action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (int i = callbacks.size() - 1; i >= 0; i--) {
            TickCallback callback = callbacks.get(i);
            if (callback.filter.test(event)) {
                callback.action.accept(event);
                callbacks.remove(i);
            }
        }
    }
}
