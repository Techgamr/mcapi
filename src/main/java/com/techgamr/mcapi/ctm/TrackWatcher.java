package com.techgamr.mcapi.ctm;

import com.mojang.logging.LogUtils;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.techgamr.mcapi.ctm.math.TrackDivision;
import com.techgamr.mcapi.ctm.model.BlockStatus;
import com.techgamr.mcapi.ctm.model.Edge;
import com.techgamr.mcapi.ctm.model.Network;
import com.techgamr.mcapi.ctm.model.Portal;
import com.techgamr.mcapi.ctm.model.SignalStatus;
import com.techgamr.mcapi.ctm.model.TrainStatus;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.techgamr.mcapi.ctm.math.Track;

public class TrackWatcher {
    private volatile boolean stopping = false;
    private final Thread watchThread;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final GlobalRailwayManager RR = Create.RAILWAYS;

    public TrackWatcher() {
        this(500L);
    }

    public TrackWatcher(long watchIntervalMillis) {
        watchThread = new Thread(() -> {
            try {
                watchLoop(watchIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "TrackMap watcher");
        watchThread.start();
    }

    public void stop() {
        stopping = true;
        if (watchThread != null) {
            try {
                watchThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        stopping = false;
    }

    private void watchLoop(long watchIntervalMillis) throws InterruptedException {
        while (!stopping) {
            try {
                update();
            } catch (Exception e) {
                LOGGER.warn("Exception during track watcher update loop", e);
            }
            //noinspection BusyWait
            Thread.sleep(watchIntervalMillis);
        }
    }

    private final Set<TrackNode> nodes = new HashSet<>();
    private final Set<TrackEdge> edges = new HashSet<>();
    private final Set<CreateSignal> signals = new HashSet<>();
    private final Set<CreateStation> stations = new HashSet<>();
    private final Set<Train> trains = new HashSet<>();
    private final Map<UUID, CreateSignalBlock> blocks = new ConcurrentHashMap<>();

    public Collection<Portal> portalsInBlock(UUID block) {
        CreateSignalBlock signalBlock = blocks.get(block);
        return signalBlock != null ? signalBlock.portals : Collections.emptyList();
    }

    public Network getNetwork() {
        List<Edge> trackEdges = new ArrayList<>();
        List<Portal> portalEdges = new ArrayList<>();

        for (TrackEdge edge : edges) {
            Object sendable = TrackWatcherUtils.getEdgeSendable(edge);
            if (sendable instanceof Edge) {
                trackEdges.add((Edge) sendable);
            } else if (sendable instanceof Portal) {
                portalEdges.add((Portal) sendable);
            }
        }

        return new Network(
                trackEdges,
                portalEdges,
                stations.stream().map(CreateStation::getSendable).toList()
        );
    }

    public SignalStatus getSignalStatus() {
        return new SignalStatus(
                signals.stream().map(CreateSignal::getSendable).toList()
        );
    }

    public BlockStatus getBlockStatus() {
        return new BlockStatus(
                blocks.values().stream().map(CreateSignalBlock::getSendable).toList()
        );
    }

    public TrainStatus getTrainStatus() {
        return new TrainStatus(
                trains.stream().map(train -> TrackWatcherUtils.getTrainSendable(this, train)).toList()
        );
    }

    private void update() {
        Map<TrackGraph, Set<TrackEdge>> networkEdges = new HashMap<>();
        Set<TrackNode> thisNodes = new HashSet<>();
        Set<TrackEdge> thisEdges = new HashSet<>();
        Set<CreateSignal> thisSignals = new HashSet<>();
        Set<CreateStation> thisStations = new HashSet<>();

        for (Map.Entry<?, TrackGraph> entry : RR.trackNetworks.entrySet()) {
            TrackGraph net = entry.getValue();

            // Track topology
            List<TrackNode> netNodes = new ArrayList<>();
            for (TrackNodeLocation nodeId : net.getNodes()) {
                netNodes.add(net.locateNode(nodeId));
            }

            Set<TrackEdge> netEdges = new HashSet<>();
            for (TrackNode node : netNodes) {
                Map<?, TrackEdge> connections = net.getConnectionsFrom(node);
                netEdges.addAll(connections.values());
            }

            thisNodes.addAll(netNodes);
            thisEdges.addAll(netEdges);
            networkEdges.put(net, netEdges);

            // Signals and stations
            for (TrackEdge edge : thisEdges) {
                for (Object pt : edge.getEdgeData().getPoints()) {
                    if (pt instanceof GlobalStation) {
                        thisStations.add(new CreateStation((GlobalStation) pt, edge));
                    } else if (pt instanceof SignalBoundary) {
                        thisSignals.add(new CreateSignal((SignalBoundary) pt, edge));
                    }
                }
            }
        }

        replaceSet(nodes, thisNodes);
        replaceSet(edges, thisEdges);
        replaceSet(signals, thisSignals);
        replaceSet(stations, thisStations);

        // Signal blocks / track occupancy
        Map<UUID, CreateSignalBlock> thisBlocks = new HashMap<>();
        for (Map.Entry<?, SignalEdgeGroup> entry : RR.signalEdgeGroups.entrySet()) {
            SignalEdgeGroup grp = entry.getValue();
            thisBlocks.put(grp.id, new CreateSignalBlock(grp));
        }

        for (Map.Entry<?, TrackGraph> entry : RR.trackNetworks.entrySet()) {
            TrackGraph net = entry.getValue();
            Set<TrackEdge> netEdges = networkEdges.get(net);

            if (netEdges == null) {
                continue;
            }

            for (TrackEdge edge : netEdges) {
                if (edge.isInterDimensional()) {
                    Object sendable = TrackWatcherUtils.getEdgeSendable(edge);
                    if (sendable instanceof Portal portal) {
                        UUID blockId = edge.getEdgeData().getEffectiveEdgeGroupId(net);
                        CreateSignalBlock block = thisBlocks.get(blockId);
                        if (block != null) {
                            block.portals.add(portal);
                        }
                    }
                } else {
                    if (edge.getEdgeData().hasSignalBoundaries()) {
                        List<SignalBoundary> signalList = new ArrayList<>();
                        for (Object pt : edge.getEdgeData().getPoints()) {
                            if (pt instanceof SignalBoundary) {
                                signalList.add((SignalBoundary) pt);
                            }
                        }

                        signalList.sort((s1, s2) -> {
                            double pos1 = s1.isPrimary(edge.node2) ? s1.position : edge.getLength() - s1.position;
                            double pos2 = s2.isPrimary(edge.node2) ? s2.position : edge.getLength() - s2.position;
                            return Double.compare(pos1, pos2);
                        });

                        if (signalList.isEmpty()) {
                            continue;
                        }

                        Track path = TrackWatcherUtils.getPath(edge);
                        List<Track> segments = new ArrayList<>();

                        // First segment
                        float firstPos = (float) (signalList.get(0).isPrimary(edge.node2)
                                ? signalList.get(0).position
                                : edge.getLength() - signalList.get(0).position);
                        TrackDivision firstDivide = path.divideAt(firstPos / edge.getLength());
                        segments.add(firstDivide.first());

                        // Middle segments
                        for (int i = 0; i < signalList.size() - 1; i++) {
                            SignalBoundary leftSig = signalList.get(i);
                            SignalBoundary rightSig = signalList.get(i + 1);

                            float rightPos = (float) (rightSig.isPrimary(edge.node2)
                                    ? rightSig.position
                                    : edge.getLength() - rightSig.position);
                            TrackDivision rightDivide = path.divideAt(rightPos / edge.getLength());
                            Track rest = rightDivide.first();

                            float leftPos = (float) (leftSig.isPrimary(edge.node2)
                                    ? leftSig.position
                                    : edge.getLength() - leftSig.position);
                            TrackDivision leftDivide = rest.divideAt(leftPos / edge.getLength());
                            segments.add(leftDivide.second());
                        }

                        // Last segment
                        float lastPos = (float) (signalList.get(signalList.size() - 1).isPrimary(edge.node2)
                                ? signalList.get(signalList.size() - 1).position
                                : edge.getLength() - signalList.get(signalList.size() - 1).position);
                        TrackDivision lastDivide = path.divideAt(lastPos / edge.getLength());
                        segments.add(lastDivide.second());

                        // Assign segments to blocks
                        for (int i = 0; i < signalList.size(); i++) {
                            Track leftSeg = segments.get(i);
                            Track rightSeg = segments.get(i + 1);
                            SignalBoundary sig = signalList.get(i);

                            UUID leftBlockId = sig.getGroup(edge.node1);
                            CreateSignalBlock leftBlock = thisBlocks.get(leftBlockId);
                            if (leftBlock != null) {
                                leftBlock.segments.add(leftSeg);
                            }

                            UUID rightBlockId = sig.getGroup(edge.node2);
                            CreateSignalBlock rightBlock = thisBlocks.get(rightBlockId);
                            if (rightBlock != null) {
                                rightBlock.segments.add(rightSeg);
                            }
                        }
                    } else {
                        UUID blockId = edge.getEdgeData().getEffectiveEdgeGroupId(net);
                        CreateSignalBlock block = thisBlocks.get(blockId);
                        if (block != null) {
                            block.segments.add(TrackWatcherUtils.getPath(edge));
                        }
                    }
                }
            }
        }

        blocks.clear();
        blocks.putAll(thisBlocks);

        // Trains
        trains.clear();
        trains.addAll(RR.trains.values());
    }

    private <T> void replaceSet(Set<T> target, Set<T> source) {
        TrackWatcherUtils.replaceWith(target, source);
    }
}
