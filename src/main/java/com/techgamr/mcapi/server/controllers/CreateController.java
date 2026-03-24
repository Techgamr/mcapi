package com.techgamr.mcapi.server.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.mojang.logging.LogUtils;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.techgamr.mcapi.ServerTickHandler;
import com.techgamr.mcapi.utils.Utils;
import com.techgamr.mcapi.server.Auth;
import com.techgamr.mcapi.server.ServerUtils;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static boolean filterUUID(@NotNull UUID callingUuid, Map.@NotNull Entry<UUID, Train> v) {
        Train train = v.getValue();
        return callingUuid.equals(Utils.NULL_UUID) || (train.owner != null && train.owner.equals(callingUuid));
    }

    public static void getTrains(Context ctx) {
        UUID callingUuid = Auth.getUuid(ctx);
        assert callingUuid != null;
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
            return Create.RAILWAYS.trains.entrySet().stream()
                    .filter(v -> filterUUID(callingUuid, v))
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> trainToJson(v.getValue())));
        }).thenAccept(result -> ctx.status(HttpStatus.OK).json(result)).exceptionally(e -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
            LOGGER.error("Error in createmod trains api", e);
            return null;
        }));
    }

    public static void setTrain(Context ctx) {
        JsonNode rootNode = ctx.bodyAsClass(JsonNode.class);
        if (!rootNode.isObject()) {
            throw new BadRequestResponse("Root node must be object");
        }

        // fetch the path param UUID or null if it is not provided
        UUID pathParamUuid;
        try {
            String pathParamString;
            try {
                pathParamString = ctx.pathParam("id");
            } catch (IllegalArgumentException ignored) {
                pathParamString = null;
            }
            pathParamUuid = pathParamString != null ? UUID.fromString(pathParamString) : null;
        } catch (IllegalArgumentException ignored) {
            throw new BadRequestResponse("Invalid UUID format");
        }

        record TrainRequestData(UUID uuid, JsonNode json, Train train) {
        }

        UUID callingUuid = Auth.getUuid(ctx);
        assert callingUuid != null;

        (pathParamUuid == null ?
                rootNode.properties().stream()
                        .map(entry -> {
                            UUID uuid = UUID.fromString(entry.getKey());
                            return new TrainRequestData(uuid, entry.getValue(), Create.RAILWAYS.trains.get(uuid));
                        }) :
                Stream.of(new TrainRequestData(pathParamUuid, rootNode, Create.RAILWAYS.trains.get(pathParamUuid)))
        )
                .filter(obj -> filterUUID(callingUuid, new AbstractMap.SimpleEntry<>(obj.uuid, obj.train)) && obj.train != null)
                .forEach(obj -> {
                    var updateOnEntryField = obj.json.get("updateOnEntry");
                    int[] updateOnEntryArr;
                    if (updateOnEntryField != null && updateOnEntryField.canConvertToInt()) {
                        updateOnEntryArr = new int[]{updateOnEntryField.asInt()};
                    } else if (updateOnEntryField != null && updateOnEntryField.isArray() && !updateOnEntryField.isEmpty()) {
                        updateOnEntryArr = new int[updateOnEntryField.size()];
                        for (int i = 0; i < updateOnEntryField.size(); i++) {
                            if (!updateOnEntryField.get(i).canConvertToInt()) {
                                throw new BadRequestResponse("non-integer in updateOnEntry array");
                            }
                            updateOnEntryArr[i] = updateOnEntryField.get(i).asInt();
                        }
                    } else if (updateOnEntryField != null && updateOnEntryField.equals(NullNode.instance)) {
                        updateOnEntryArr = new int[0];
                    } else if (updateOnEntryField != null) {
                        throw new BadRequestResponse("updateOnEntry is not array or number");
                    } else { // null
                        updateOnEntryArr = new int[0];
                    }
                    var updateOnEntry = Arrays.stream(updateOnEntryArr).boxed().toList();
                    ServerTickHandler.registerCallback(e -> updateOnEntry.isEmpty() || obj.train.runtime.schedule == null || (updateOnEntry.contains(obj.train.runtime.currentEntry) && obj.train.runtime.state == ScheduleRuntime.State.POST_TRANSIT), event -> {
                        try {
                            if (obj.json.has("runtime")) {
                                JsonNode runtimeNode = obj.json.get("runtime");
                                JsonNode scheduleDataNode = runtimeNode.get("schedule");
                                JsonNode isAutoScheduleNode = runtimeNode.get("isAutoSchedule");
                                JsonNode currentEntryNode = runtimeNode.get("currentEntry");
                                JsonNode pausedNode = runtimeNode.get("paused");
                                if (scheduleDataNode != null) {
                                    boolean isAutoSchedule = runtimeNode.has("isAutoSchedule") && isAutoScheduleNode.booleanValue();
                                    Schedule schedule = scheduleDataNode.isNull() ? null : Schedule.fromTag(Utils.jsonToNbt(scheduleDataNode.toString()));
                                    // enable in-place schedule changes without changing progress when no updateOnEntry is specified and savedProgress == 0 (default)
                                    if (updateOnEntry.isEmpty() && schedule != null && schedule.savedProgress == 0) {
                                        schedule.savedProgress = obj.train.runtime.currentEntry;
                                    }
                                    obj.train.runtime.setSchedule(schedule, isAutoSchedule);
                                } else if (isAutoScheduleNode != null) {
                                    obj.train.runtime.isAutoSchedule = isAutoScheduleNode.booleanValue();
                                }
                                if (pausedNode != null) {
                                    obj.train.runtime.paused = pausedNode.booleanValue();
                                }
                                if (currentEntryNode != null) {
                                    obj.train.runtime.currentEntry = currentEntryNode.asInt();
                                }
                            }
                            LOGGER.info("Successfully updated train with uuid {}", obj.uuid);
                        } catch (Exception e) {
                            LOGGER.error("Failed to update train with uuid {}", obj.uuid, e);
                        }
                    });
                });

        ctx.status(HttpStatus.ACCEPTED);
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
        assert callingUuid != null;
        ctx.future(() -> ServerUtils.<@Nullable JsonNode>executeOnServer(ctx, srv -> {
            Optional<Train> train = Create.RAILWAYS.trains.entrySet().stream()
                    .filter(entry -> filterUUID(callingUuid, entry) && entry.getKey().equals(trainUuid))
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

    public static void getTrainByName(Context ctx) {
        String trainName = ctx.pathParam("name");

        UUID callingUuid = Auth.getUuid(ctx);
        assert callingUuid != null;
        ctx.future(() -> ServerUtils.executeOnServer(ctx, srv -> {
            return Create.RAILWAYS.trains.entrySet().stream()
                    .filter(entry -> filterUUID(callingUuid, entry) && entry.getValue().name.getString().equals(trainName))
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
                Map.entry("icon", train.icon.getId().toString()),
                Map.entry("runtime", Map.ofEntries(
                        Map.entry("schedule", schedule != null ? Utils.nbtToJson(schedule.write()) : NullNode.instance),
                        Map.entry("completed", train.runtime.completed),
                        Map.entry("currentEntry", train.runtime.currentEntry),
                        Map.entry("currentTitle", train.runtime.currentTitle),
                        Map.entry("paused", train.runtime.paused),
                        Map.entry("ticksInTransit", train.runtime.ticksInTransit),
                        Map.entry("isAutoSchedule", train.runtime.isAutoSchedule),
                        Map.entry("state", train.runtime.state),
                        Map.entry("destination", train.navigation.destination == null ? NullNode.instance : Map.ofEntries(
                                Map.entry("id", train.navigation.destination.id),
                                Map.entry("name", train.navigation.destination.name),
                                Map.entry("distanceStartedAt", train.navigation.distanceStartedAt),
                                Map.entry("distanceToDestination", train.navigation.distanceToDestination)
                        ))
                ))
        ));
    }
}
