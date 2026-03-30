package com.techgamr.mcapi.ctm.delta;

import com.fasterxml.jackson.databind.JsonNode;
import com.techgamr.mcapi.utils.Utils;
import io.javalin.http.sse.SseClient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {
    private volatile JsonNode currentData;

    private static class ConnectedClient {
        public UUID clientId;
        public SseClient emitter;
        public JsonNode lastSentState; // Track previous state
        public long lastUpdateTime;
        public boolean isDeltaClient;

        public ConnectedClient(UUID clientId, SseClient emitter, boolean isDeltaClient) {
            this.clientId = clientId;
            this.emitter = emitter;
            this.lastSentState = null;
            this.lastUpdateTime = System.currentTimeMillis();
            this.isDeltaClient = isDeltaClient;
        }
    }

    private final ConcurrentHashMap<UUID, ConnectedClient> clients =
            new ConcurrentHashMap<>();

    private void addClient(UUID clientId, SseClient emitter, boolean isDeltaClient) {
        clients.put(clientId, new ConnectedClient(clientId, emitter, isDeltaClient));
    }

    private ConnectedClient getClient(UUID clientId) {
        return clients.get(clientId);
    }

    private void removeClient(UUID clientId) {
        clients.remove(clientId);
    }

    @Contract(" -> new")
    private @NotNull List<ConnectedClient> getAllClients() {
        return new ArrayList<>(clients.values());
    }

    public void handleSubscribe(@NotNull SseClient emitter) {
        emitter.keepAlive();

        var clientId = UUID.randomUUID();
        Boolean isDelta = Optional.ofNullable(emitter.ctx().queryParam("delta")).map(x -> x.trim().equals("1")).orElse(false);
        this.addClient(clientId, emitter, isDelta);

        // Send initial full state
        if (currentData != null) {
            var data = currentData;

            ConnectedClient client = this.getClient(clientId);
            client.lastSentState = currentData.deepCopy();

            emitter.sendEvent(data);
        }

        emitter.onClose(() -> this.removeClient(clientId));
    }

    public void broadcastUpdate(Object newData) {
        broadcastUpdate(Utils.OBJECT_MAPPER.valueToTree(newData));
    }

    public void broadcastUpdate(JsonNode newData) {
        currentData = newData;
        for (ConnectedClient client : this.getAllClients()) {
            if (client.emitter.terminated()) {
                this.removeClient(client.clientId);
            } else {
                if (client.isDeltaClient && client.lastSentState != null) {
                    // Calculate and send delta
                    List<JsonPatchOperation> patches =
                            JsonDiffUtil.calculateDiff(client.lastSentState, newData);
                    client.emitter.sendEvent("delta", patches);
                } else {
                    // Send full state if client has no previous state
                    client.emitter.sendEvent(newData);
                }

                // Update client's tracked state
                client.lastSentState = newData.deepCopy();
                client.lastUpdateTime = System.currentTimeMillis();
            }
        }
    }
}
