package com.techgamr.mcapi.server;

import io.javalin.config.Key;
import io.javalin.http.Context;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServerUtils {
    public static MinecraftServer getServer(Context ctx) {
        return ctx.appData(ApiServer.CTX_APPDATA_SERVER_KEY);
    }

    public static boolean isNotJsonContentType(Context ctx) {
        String contentType = ctx.contentType();
        return contentType == null || !contentType.contains("application/json");
    }

    /**
     * Execute a closure that returns a value on the server thread.
     *
     * @param server The server to execute the task on.
     * @param task   Function that runs on the server thread
     * @param <T>    Return type of the function
     * @return Result of the function
     */
    public static <T> CompletableFuture<T> executeOnServer(MinecraftServer server, Function<MinecraftServer, T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        server.execute(() -> {
            try {
                T result = task.apply(server);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static <T> CompletableFuture<T> executeOnServer(Context ctx, Function<MinecraftServer, T> task) {
        return executeOnServer(getServer(ctx), task);
    }

    public static CompletableFuture<Void> executeOnServer(MinecraftServer server, Consumer<MinecraftServer> task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        server.execute(() -> {
            try {
                task.accept(server);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static CompletableFuture<Void> executeOnServer(Context ctx, Consumer<MinecraftServer> task) {
        return executeOnServer(getServer(ctx), task);
    }
}
