package com.techgamr.mcapi.ctm;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.techgamr.mcapi.ctm.math.BezierCurve;
import com.techgamr.mcapi.ctm.math.Line;
import com.techgamr.mcapi.ctm.math.Track;
import com.techgamr.mcapi.ctm.model.CreateTrain;
import com.techgamr.mcapi.ctm.model.DimensionLocation;
import com.techgamr.mcapi.ctm.model.Point;
import com.techgamr.mcapi.ctm.model.Portal;
import com.techgamr.mcapi.ctm.model.TrainCar;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class TrackWatcherUtils {
    public static <T> void replaceWith(Set<T> target, Collection<T> other) {
        target.retainAll(other);
        target.addAll(other);
    }

    public static <T> T getFirst(Couple<T> couple) {
        return couple.get(true);
    }

    public static <T> T getSecond(Couple<T> couple) {
        return couple.get(false);
    }

    public static Point toSendable(Vec3 vec3) {
        return new Point(vec3.x, vec3.y, vec3.z);
    }

    public static String dimensionString(ResourceKey<Level> resourceKey) {
        var location = resourceKey.location();
        return location.getNamespace() + ":" + location.getPath();
    }

    public static Point trackNodeLocationSendable(TrackNodeLocation location) {
        return toSendable(location.getLocation());
    }

    public static Track getPath(TrackEdge edge) {
        if (edge.isTurn()) {
            return BezierCurve.from(edge.getTurn(), TrackWatcherUtils.dimensionString(edge.node1.getLocation().dimension));
        } else {
            return new Line(
                    TrackWatcherUtils.dimensionString(edge.node1.getLocation().dimension),
                    edge.node1.getLocation().getLocation(),
                    edge.node2.getLocation().getLocation()
            );
        }
    }

    public static DimensionLocation getDimensionLocation(TrackNode node) {
        return new DimensionLocation(
                TrackWatcherUtils.dimensionString(node.getLocation().dimension),
                toSendable(node.getLocation().getLocation())
        );
    }

    public static Object getEdgeSendable(TrackEdge edge) {
        if (edge.isInterDimensional()) {
            return new Portal(
                    getDimensionLocation(edge.node1),
                    getDimensionLocation(edge.node2)
            );
        } else {
            return getPath(edge).getSendable();
        }
    }

    @Nullable
    public static DimensionLocation getTravellingPointSendable(TravellingPoint point) {
        if (point.node1 == null || point.edge == null) {
            return null;
        }
        return new DimensionLocation(
                TrackWatcherUtils.dimensionString(point.node1.getLocation().dimension),
                toSendable(point.getPosition(null))
        );
    }

    public static TrainCar getCarriageSendable(TrackWatcher watcher, Carriage carriage) {
        Portal portal = null;

        if (carriage.getLeadingPoint() != null && carriage.getTrailingPoint() != null) {
            List<Portal> portals = new ArrayList<>();
            for (var blockId : carriage.train.occupiedSignalBlocks.keySet()) {
                portals.addAll(watcher.portalsInBlock(blockId));
            }

            String leadingDim = carriage.getLeadingPoint().node1 != null
                    ? TrackWatcherUtils.dimensionString(carriage.getLeadingPoint().node1.getLocation().dimension)
                    : null;
            String trailingDim = carriage.getTrailingPoint().node1 != null
                    ? TrackWatcherUtils.dimensionString(carriage.getTrailingPoint().node1.getLocation().dimension)
                    : null;

            for (Portal p : portals) {
                if (p.getFrom().getDimension().equals(leadingDim) && p.getTo().getDimension().equals(trailingDim)) {
                    portal = p;
                    break;
                }
            }
        }

        return new TrainCar(
                carriage.id,
                carriage.getLeadingPoint() != null ? getTravellingPointSendable(carriage.getLeadingPoint()) : null,
                carriage.getTrailingPoint() != null ? getTravellingPointSendable(carriage.getTrailingPoint()) : null,
                portal
        );
    }

    public static CreateTrain getTrainSendable(TrackWatcher watcher, Train train) {
        List<TrainCar> cars = new ArrayList<>();
        for (Carriage carriage : train.carriages) {
            cars.add(getCarriageSendable(watcher, carriage));
        }

        return new CreateTrain(
                train.id,
                train.name.getString(),
                null,
                cars,
                train.speed < 0,
                train.speed == 0.0
        );
    }
}
