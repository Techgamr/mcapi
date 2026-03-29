package com.techgamr.mcapi.ctm.math;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.techgamr.mcapi.ctm.TrackWatcherUtils;
import com.techgamr.mcapi.ctm.model.Edge;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public record BezierCurve(String dimension, Vec3 start, Vec3 controlPoint1, Vec3 controlPoint2,
                          Vec3 end) implements Track {
    public List<Vec3> getPoints() {
        return Arrays.asList(start, controlPoint1, controlPoint2, end);
    }

    @Override
    public Edge getSendable() {
        return new Edge(
                dimension,
                Arrays.asList(
                        TrackWatcherUtils.toSendable(start),
                        TrackWatcherUtils.toSendable(controlPoint1),
                        TrackWatcherUtils.toSendable(controlPoint2),
                        TrackWatcherUtils.toSendable(end)
                )
        );
    }

    @Override
    public TrackDivision divideAt(double position) {
        List<Vec3> points = getPoints();
        List<Vec3> points1 = TrackMath.multiLerp(points, position);
        List<Vec3> points2 = TrackMath.multiLerp(points1, position);
        List<Vec3> points3 = TrackMath.multiLerp(points2, position);

        Vec3 cp11 = points1.get(0);
        Vec3 cp12 = points2.get(0);
        Vec3 midpoint = points3.get(0);
        Vec3 cp21 = points2.get(1);
        Vec3 cp22 = points1.get(2);

        return new TrackDivision(
                new BezierCurve(dimension, start, cp11, cp12, midpoint),
                new BezierCurve(dimension, midpoint, cp21, cp22, end)
        );
    }

    public static BezierCurve from(BezierConnection conn, String dim) {
        Vec3 start = TrackWatcherUtils.getFirst(conn.starts);
        Vec3 end = TrackWatcherUtils.getSecond(conn.starts);
        Vec3 startAxis = TrackWatcherUtils.getFirst(conn.axes);
        Vec3 endAxis = TrackWatcherUtils.getSecond(conn.axes);

        return new BezierCurve(
                dim,
                start,
                TrackMath.add(start, TrackMath.multiply(startAxis, conn.getHandleLength())),
                TrackMath.add(end, TrackMath.multiply(endAxis, conn.getHandleLength())),
                end
        );
    }
}
