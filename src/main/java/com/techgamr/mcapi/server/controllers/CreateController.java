package com.techgamr.mcapi.server.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mojang.logging.LogUtils;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.techgamr.mcapi.utils.Utils;
import com.techgamr.mcapi.server.Auth;
import com.techgamr.mcapi.server.ServerUtils;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class CreateController {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void getAllStations(Context ctx) {
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
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
        }));
    }

    public static void getTrains(Context ctx) {
        UUID callingUuid = Auth.getUuid(ctx);
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
            return Create.RAILWAYS.trains.entrySet().stream()
                    .filter(v -> {
                        Train train = v.getValue();
                        return callingUuid.equals(Utils.NULL_UUID) || (train.owner != null && train.owner.equals(callingUuid));
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> trainToJson(v.getValue())));
        }).thenAccept(result -> ctx.status(HttpStatus.OK).json(result)).exceptionally(e -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            LOGGER.error("Error in createmod trains api", e);
            return null;
        }));
    }

    public static void setTrains(Context ctx) throws Exception {
        if (ServerUtils.isNotJsonContentType(ctx)) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Content-Type must be application/json"));
            return;
        }
        JsonNode jsonNode = Utils.OBJECT_MAPPER.readTree(ctx.body());
        if (!jsonNode.isObject()) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Root node must be object"));
        }
        ObjectNode rootNode = (ObjectNode) jsonNode;
        List<String> uuids = new ArrayList<>();
        rootNode.fieldNames().forEachRemaining(uuids::add);

        UUID callingUuid = Auth.getUuid(ctx);
        ctx.future(() -> ServerUtils.<@Nullable String>executeOnServer(ctx, srv -> {
            return Create.RAILWAYS.trains.entrySet().stream()
                    .filter(v -> {
                        Train train = v.getValue();
                        return (callingUuid.equals(Utils.NULL_UUID) || (train.owner != null && train.owner.equals(callingUuid)))
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
    }

    public static void getTrain(Context ctx) {
        UUID trainUuid;
        try {
            trainUuid = UUID.fromString(ctx.pathParam("id"));
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Train UUID is not valid"));
            return;
        }

        UUID callingUuid = Auth.getUuid(ctx);
        ctx.future(() -> ServerUtils.<@Nullable JsonNode>executeOnServer(ctx, srv -> {
            Optional<Train> train = Create.RAILWAYS.trains.entrySet().stream()
                    .filter(entry -> {
                        Train t = entry.getValue();
                        return (callingUuid.equals(Utils.NULL_UUID) || (t.owner != null && t.owner.equals(callingUuid)))
                                && entry.getKey().equals(trainUuid);
                    })
                    .map(Map.Entry::getValue)
                    .findFirst();
            return train.map(CreateController::trainToJson).orElse(null);
        }).thenAccept(result -> {
            if (result != null) ctx.status(HttpStatus.OK).json(result);
            else ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Train with this uuid not found"));
        }).exceptionally(e -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            LOGGER.error("Error in createmod trains api", e);
            return null;
        }));
    }

    public static void setTrain(Context ctx) throws Exception {
        if (ServerUtils.isNotJsonContentType(ctx)) {
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
        JsonNode jsonNode = Utils.OBJECT_MAPPER.readTree(ctx.body());

        UUID callingUuid = Auth.getUuid(ctx);
        ctx.future(() -> ServerUtils.<@Nullable String>executeOnServer(ctx, srv -> {
            return Create.RAILWAYS.trains.entrySet().stream()
                    .filter(entry -> {
                        Train t = entry.getValue();
                        return (callingUuid.equals(Utils.NULL_UUID) || (t.owner != null && t.owner.equals(callingUuid)))
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
    }

    public static void getTrainByName(Context ctx) {
        String trainName = ctx.pathParam("name");

        UUID callingUuid = Auth.getUuid(ctx);
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
            return Create.RAILWAYS.trains.entrySet().stream()
                    .filter(entry -> {
                        Train t = entry.getValue();
                        return (callingUuid.equals(Utils.NULL_UUID) || (t.owner != null && t.owner.equals(callingUuid)))
                                && entry.getValue().name.getString().equals(trainName);
                    })
                    .map(Map.Entry::getKey)
                    .toList();
        }).thenAccept(result -> ctx.status(HttpStatus.OK).json(result)).exceptionally(e -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            LOGGER.error("Error in createmod trains api", e);
            return null;
        }));
    }

    private static JsonNode trainToJson(Train train) {
        Schedule schedule = train.runtime.getSchedule();
        return Utils.OBJECT_MAPPER.valueToTree(Map.ofEntries(
                Map.entry("name", train.name.getString()),
                Map.entry("owner", train.owner != null ? train.owner : Utils.NULL_UUID),
                Map.entry("graph", train.graph != null ? train.graph.id : Utils.NULL_UUID),
                Map.entry("derailed", train.derailed),
                Map.entry("currentlyBackwards", train.currentlyBackwards),
                Map.entry("runtime", Map.ofEntries(
                        Map.entry("schedule", schedule != null ? Utils.nbtToJson(schedule.write()) : NullNode.instance),
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

    private static String jsonToTrain(Train train, JsonNode json) {
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
                        train.runtime.setSchedule(scheduleDataNode.isNull() ? null : Schedule.fromTag(Utils.jsonToNbt(scheduleDataNode.toString())), isAutoSchedule);
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
}
