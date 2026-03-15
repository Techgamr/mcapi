package com.techgamr.mcapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;
import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApiServer {
    private Javalin app;
    private static final Logger LOGGER = LogUtils.getLogger();

    public void start(MinecraftServer server) {
        app = Javalin.create(config -> {
            initRoutes(config.routes, server);
        }).start("127.0.0.1", 3333);
    }

    public void restart(MinecraftServer server) {
        stop();
        start(server);
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    /**
     * Execute a closure that returns a value on the server thread.
     *
     * @param server The server to execute the task on.
     * @param task   Function that runs on the server thread
     * @param <T>    Return type of the function
     * @return Result of the function
     */
    private static <T> CompletableFuture<T> executeOnServer(MinecraftServer server, Function<MinecraftServer, T> task) {
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

    private static CompletableFuture<Void> executeOnServer(MinecraftServer server, Consumer<MinecraftServer> task) {
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

    private static final String UUID_HEADER = "X-Mcapi-Uuid";

    private static UUID getUuidForContext(Context ctx) {
        try {
            return UUID.fromString(Objects.requireNonNull(ctx.header(UUID_HEADER)));
        } catch (Exception e) {
            return NULL_UUID;
        }
    }

    private static boolean isNotJsonContentType(Context ctx) {
        String contentType = ctx.contentType();
        return contentType == null || !contentType.contains("application/json");
    }

    private void initRoutes(RoutesConfig routes, MinecraftServer server) {
        routes.get("/", ctx -> ctx.json("hello world!"));

        // Auth API
        // This is to be validated on an external proxy
        routes.get("/auth/{hash}", ctx -> {
            try {
                String uuid = ApiKeyManager.getUuid(ctx.pathParam("hash"));
                if (uuid == null) {
                    ctx.status(HttpStatus.NOT_FOUND);
                } else {
                    ctx.status(HttpStatus.OK).json(Map.of("uuid", uuid));
                }
            } catch (Exception e) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });

        // Player API
        routes.get("/players/count", ctx -> {
            ctx.future(() -> executeOnServer(server, MinecraftServer::getPlayerCount).thenAccept(count -> ctx.json(Map.of("count", count))).exceptionally(e -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
                return null;
            }));
        });
        routes.get("/players/list", ctx -> {
            ctx.future(() -> executeOnServer(server, MinecraftServer::getPlayerList).thenAccept(pl -> ctx.json(pl.getPlayers().stream().map(p -> Map.ofEntries(Map.entry("name", p.getName().getString()), Map.entry("uuid", p.getUUID()))).collect(Collectors.toList()))).exceptionally(e -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
                return null;
            }));
        });

        // Chat API
        routes.post("/chat", ctx -> {
            String body = ctx.body();
            Component message = Component.Serializer.fromJson(body);
            if (message == null) {
                message = Component.literal(body);
            }
            Component finalMessage = message;
            ctx.future(() -> executeOnServer(server, (Consumer<MinecraftServer>) srv -> srv.getPlayerList().broadcastSystemMessage(finalMessage, false)).thenAccept(pl -> ctx.status(HttpStatus.OK)).exceptionally(e -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
                return null;
            }));
        });
        routes.post("/chat/{player}", ctx -> {
            String playerParam = ctx.pathParam("player");
            UUID uuid = null;
            try {
                uuid = UUID.fromString(playerParam);
            } catch (Exception e) {
                // do nothing
            }
            Component message = null;
            Exception messageEx = null;
            try {
                message = Component.Serializer.fromJson(ctx.body());
            } catch (Exception e) {
                messageEx = e;
            }
            if (message == null) {
                ctx.status(400).json(Map.of("error", messageEx != null ? messageEx.getMessage() : "You must provide a valid text component string"));
            }

            Component finalMessage = message;
            UUID finalUuid = uuid;
            ctx.future(() -> executeOnServer(server, srv -> {
                if (finalUuid != null) {
                    Objects.requireNonNull(srv.getPlayerList().getPlayer(finalUuid)).sendSystemMessage(finalMessage);
                } else {
                    Objects.requireNonNull(srv.getPlayerList().getPlayerByName(playerParam)).sendSystemMessage(finalMessage);
                }
            }).thenAccept(pl -> ctx.status(HttpStatus.OK)).exceptionally(e -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
                return null;
            }));
        });

        // ** Create Railways API **
        // Stations
        routes.get("/createmod/stations", ctx -> ctx.future(() -> executeOnServer(server, srv -> {
            Map<String, Map<String, String>> result = new HashMap<>();
            for (Map.Entry<UUID, TrackGraph> uuidTrackGraphEntry : Create.RAILWAYS.trackNetworks.entrySet()) {
                TrackGraph net = uuidTrackGraphEntry.getValue();
                for (TrackNodeLocation nodeLoc : net.getNodes()) {
                    TrackNode node = net.locateNode(nodeLoc);
                    for (TrackEdge edge : net.getConnectionsFrom(node).values()) {
                        for (TrackEdgePoint pt : edge.getEdgeData().getPoints()) {
                            if (pt instanceof GlobalStation) {
                                result.put(pt.id.toString(), Map.ofEntries(Map.entry("name", ((GlobalStation) pt).name), Map.entry("graph", net.id.toString())));
                            }
                        }
                    }
                }
            }
            return result;
        }).thenAccept(result -> ctx.status(HttpStatus.OK).json(result)).exceptionally(e -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            return null;
        })));

        // Trains: bulk API
        routes.get("/createmod/train", ctx -> {
            UUID callingUuid = getUuidForContext(ctx);
            ctx.future(() -> executeOnServer(server, srv -> {
                return Create.RAILWAYS.trains.entrySet().stream()
                        .filter(v -> {
                            Train train = v.getValue();
                            return callingUuid.equals(NULL_UUID) || (train.owner != null && train.owner.equals(callingUuid));
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, v -> trainToJson(v.getValue())));
            }).thenAccept(result -> ctx.status(HttpStatus.OK).json(result)).exceptionally(e -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
                LOGGER.error("Error in createmod trains api", e);
                return null;
            }));
        });
        routes.post("/createmod/train", ctx -> {
            if (isNotJsonContentType(ctx)) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Content-Type must be application/json"));
                return;
            }
            JsonNode jsonNode = OBJECT_MAPPER.readTree(ctx.body());
            if (!jsonNode.isObject()) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Root node must be object"));
            }
            ObjectNode rootNode = (ObjectNode) jsonNode;
            List<String> uuids = new ArrayList<>();
            rootNode.fieldNames().forEachRemaining(uuids::add);

            UUID callingUuid = getUuidForContext(ctx);
            ctx.future(() -> ApiServer.<@Nullable String>executeOnServer(server, srv -> {
                return Create.RAILWAYS.trains.entrySet().stream()
                        .filter(v -> {
                            Train train = v.getValue();
                            return (callingUuid.equals(NULL_UUID) || (train.owner != null && train.owner.equals(callingUuid)))
                                    && uuids.contains(v.getKey().toString());
                        })
                        .map(train -> jsonToTrain(train.getValue(), rootNode.get(train.getKey().toString())))
                        .collect(Collectors.collectingAndThen(
                                Collectors.joining(" "),
                                str -> str.isEmpty() ? null : str
                        ));
            }).thenAccept(result -> {
                if (result == null) ctx.status(HttpStatus.NO_CONTENT);
                else
                    ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", result));
            }).exceptionally(e -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
                LOGGER.error("Error in createmod trains api", e);
                return null;
            }));
        });

        // Trains: by-id API
        routes.get("/createmod/train/{id}", ctx -> {
            UUID trainUuid;
            try {
                trainUuid = UUID.fromString(ctx.pathParam("id"));
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Train UUID is not valid"));
                return;
            }

            UUID callingUuid = getUuidForContext(ctx);
            ctx.future(() -> ApiServer.<@Nullable JsonNode>executeOnServer(server, srv -> {
                Optional<Train> train = Create.RAILWAYS.trains.entrySet().stream()
                        .filter(entry -> {
                            Train t = entry.getValue();
                            return (callingUuid.equals(NULL_UUID) || (t.owner != null && t.owner.equals(callingUuid)))
                                    && entry.getKey().equals(trainUuid);
                        })
                        .map(Map.Entry::getValue)
                        .findFirst();
                return train.map(this::trainToJson).orElse(null);
            }).thenAccept(result -> {
                if (result != null) ctx.status(HttpStatus.OK).json(result);
                else ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Train with this uuid not found"));
            }).exceptionally(e -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
                LOGGER.error("Error in createmod trains api", e);
                return null;
            }));
        });
        routes.post("/createmod/train/{id}", ctx -> {
            if (isNotJsonContentType(ctx)) {
                ctx.status(400);
                ctx.json(Collections.singletonMap("error", "Content-Type must be application/json"));
                return;
            }
            UUID trainUuid;
            try {
                trainUuid = UUID.fromString(ctx.pathParam("id"));
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Train UUID is not valid"));
                return;
            }
            LOGGER.info("POST for train uuid {}", trainUuid);
            JsonNode jsonNode = OBJECT_MAPPER.readTree(ctx.body());

            UUID callingUuid = getUuidForContext(ctx);
            ctx.future(() -> ApiServer.<@Nullable String>executeOnServer(server, srv -> {
                return Create.RAILWAYS.trains.entrySet().stream()
                        .filter(entry -> {
                            Train t = entry.getValue();
                            return (callingUuid.equals(NULL_UUID) || (t.owner != null && t.owner.equals(callingUuid)))
                                    && entry.getKey().equals(trainUuid);
                        })
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .map(train -> jsonToTrain(train, jsonNode))
                        .orElse("Train with given uuid not found");
            }).thenAccept(result -> {
                if (result == null || result.isEmpty()) ctx.status(HttpStatus.NO_CONTENT);
                else
                    ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", result));
            }).exceptionally(e -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
                LOGGER.error("Error in createmod trains api", e);
                return null;
            }));
        });

        // Utilities
        routes.get("/createmod/train/by-name/{name}", ctx -> {
            String trainName = ctx.pathParam("name");

            UUID callingUuid = getUuidForContext(ctx);
            ctx.future(() -> executeOnServer(server, srv -> {
                return Create.RAILWAYS.trains.entrySet().stream()
                        .filter(entry -> {
                            Train t = entry.getValue();
                            return (callingUuid.equals(NULL_UUID) || (t.owner != null && t.owner.equals(callingUuid)))
                                    && entry.getValue().name.getString().equals(trainName);
                        })
                        .map(Map.Entry::getKey)
                        .toList();
            }).thenAccept(result -> ctx.status(HttpStatus.OK).json(result)).exceptionally(e -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
                LOGGER.error("Error in createmod trains api", e);
                return null;
            }));
        });
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final UUID NULL_UUID = new UUID(0L, 0L);

    private JsonNode trainToJson(Train train) {
        Schedule schedule = train.runtime.getSchedule();
        return OBJECT_MAPPER.valueToTree(Map.ofEntries(
                Map.entry("name", train.name.getString()),
                Map.entry("owner", train.owner != null ? train.owner : NULL_UUID),
                Map.entry("graph", train.graph != null ? train.graph.id : NULL_UUID),
                Map.entry("derailed", train.derailed),
                Map.entry("currentlyBackwards", train.currentlyBackwards),
                Map.entry("runtime", Map.ofEntries(
                        Map.entry("schedule", schedule != null ? nbtToJson(schedule.write()) : NullNode.instance),
                        Map.entry("completed", train.runtime.completed),
                        Map.entry("currentEntry", train.runtime.currentEntry),
                        Map.entry("currentTitle", train.runtime.currentTitle),
                        Map.entry("paused", train.runtime.paused),
                        Map.entry("ticksInTransit", train.runtime.ticksInTransit),
                        Map.entry("isAutoSchedule", train.runtime.isAutoSchedule),
                        Map.entry("state", train.runtime.state)
                ))
        ));
    }

    private String jsonToTrain(Train train, JsonNode json) {
        LOGGER.info("got here");
        try {
            if (json.has("runtime")) {
                JsonNode runtimeNode = json.get("runtime");
                JsonNode scheduleDataNode = runtimeNode.get("schedule");
                JsonNode isAutoScheduleNode = runtimeNode.get("isAutoSchedule");
                JsonNode currentEntryNode = runtimeNode.get("currentEntry");
                JsonNode pausedNode = runtimeNode.get("paused");
                if (scheduleDataNode != null) {
                    boolean isAutoSchedule = runtimeNode.has("isAutoSchedule") && isAutoScheduleNode.booleanValue();
                    try {
                        train.runtime.setSchedule(scheduleDataNode.isNull() ? null : Schedule.fromTag(jsonToNbt(scheduleDataNode.toString())), isAutoSchedule);
                    } catch (Exception e) {
                        // do nothing
                    }
                } else if (isAutoScheduleNode != null) {
                    train.runtime.isAutoSchedule = isAutoScheduleNode.booleanValue();
                }
                if (pausedNode != null) {
                    train.runtime.paused = pausedNode.booleanValue();
                }
                if (currentEntryNode != null) {
                    train.runtime.currentEntry = currentEntryNode.asInt();
                }
            }
            return "";
        } catch (Exception e) {
            LOGGER.error("jsonToTrain failed", e);
            return "jsonToTrain failed: " + e.getMessage();
        }
    }

    private JsonNode nbtToJson(CompoundTag tag) {
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

    private CompoundTag jsonToNbt(String jsonString) {
        try {
            return CompoundTag.CODEC
                    .parse(JsonOps.INSTANCE, JsonParser.parseString(jsonString))
                    .getOrThrow(false, s -> LOGGER.error("Error deserialising NBT: {}", s));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
