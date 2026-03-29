package com.techgamr.mcapi.ctm.math;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.techgamr.mcapi.ctm.TrackWatcherExtensions;
import com.techgamr.mcapi.ctm.models.Edge;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TrackMath {
    // Vec3 operators
    public static Vec3 add(Vec3 a, Vec3 b) {
        return new Vec3(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vec3 multiply(Vec3 v, double scale) {
        return new Vec3(v.x * scale, v.y * scale, v.z * scale);
    }

    public static Vec3 negate(Vec3 v) {
        return multiply(v, -1.0);
    }

    // Track interface and implementations
    public interface Track {
        TrackDivision divideAt(double position);
        Edge getSendable();
    }

    public static class TrackDivision {
        public final Track first;
        public final Track second;

        public TrackDivision(Track first, Track second) {
            this.first = first;
            this.second = second;
        }
    }

    public static class Line implements Track {
        public final String dimension;
        public final Vec3 start;
        public final Vec3 end;

        public Line(String dimension, Vec3 start, Vec3 end) {
            this.dimension = dimension;
            this.start = start;
            this.end = end;
        }

        @Override
        public Edge getSendable() {
            return new Edge(
                    dimension,
                    Arrays.asList(
                            TrackWatcherExtensions.toSendable(start),
                            TrackWatcherExtensions.toSendable(end)
                    )
            );
        }

        @Override
        public TrackDivision divideAt(double position) {
            Vec3 point = start.lerp(end, position);
            return new TrackDivision(
                    new Line(dimension, start, point),
                    new Line(dimension, point, end)
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Line line = (Line) o;
            return Objects.equals(dimension, line.dimension) &&
                    Objects.equals(start, line.start) &&
                    Objects.equals(end, line.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, start, end);
        }
    }

    public static List<Vec3> multiLerp(List<Vec3> points, double position) {
        List<Vec3> result = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            result.add(a.lerp(b, position));
        }
        return result;
    }

    public static class BezierCurve implements Track {
        public final String dimension;
        public final Vec3 start;
        public final Vec3 controlPoint1;
        public final Vec3 controlPoint2;
        public final Vec3 end;

        public BezierCurve(String dimension, Vec3 start, Vec3 controlPoint1, Vec3 controlPoint2, Vec3 end) {
            this.dimension = dimension;
            this.start = start;
            this.controlPoint1 = controlPoint1;
            this.controlPoint2 = controlPoint2;
            this.end = end;
        }

        public List<Vec3> getPoints() {
            return Arrays.asList(start, controlPoint1, controlPoint2, end);
        }

        @Override
        public Edge getSendable() {
            return new Edge(
                    dimension,
                    Arrays.asList(
                            TrackWatcherExtensions.toSendable(start),
                            TrackWatcherExtensions.toSendable(controlPoint1),
                            TrackWatcherExtensions.toSendable(controlPoint2),
                            TrackWatcherExtensions.toSendable(end)
                    )
            );
        }

        @Override
        public TrackDivision divideAt(double position) {
            List<Vec3> points = getPoints();
            List<Vec3> points1 = multiLerp(points, position);
            List<Vec3> points2 = multiLerp(points1, position);
            List<Vec3> points3 = multiLerp(points2, position);

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BezierCurve that = (BezierCurve) o;
            return Objects.equals(dimension, that.dimension) &&
                    Objects.equals(start, that.start) &&
                    Objects.equals(controlPoint1, that.controlPoint1) &&
                    Objects.equals(controlPoint2, that.controlPoint2) &&
                    Objects.equals(end, that.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, start, controlPoint1, controlPoint2, end);
        }

        public static BezierCurve from(BezierConnection conn, String dim) {
            Vec3 start = TrackWatcherExtensions.getFirst(conn.starts);
            Vec3 end = TrackWatcherExtensions.getSecond(conn.starts);
            Vec3 startAxis = TrackWatcherExtensions.getFirst(conn.axes);
            Vec3 endAxis = TrackWatcherExtensions.getSecond(conn.axes);

            return new BezierCurve(
                    dim,
                    start,
                    add(start, multiply(startAxis, conn.getHandleLength())),
                    add(end, multiply(endAxis, conn.getHandleLength())),
                    end
            );
        }
    }

    // Angle calculation
    public static double getAngle(Vec3 v) {
        return Math.round(Math.atan2(v.x, -v.z) * (180.0 / Math.PI));
    }

    // TrackEdgePoint extensions
    public static Vec3 locationOn(TrackEdgePoint point, TrackEdge edge) {
        double basePos = point.isPrimary(edge.node1) ? edge.getLength() - point.position : point.position;
        return edge.getPosition(null, basePos / edge.getLength());
    }

    public static Vec3 directionAt(TrackEdge edge, double position) {
        double step = 0.05 / edge.getLength();
        double t = position / edge.getLength();
        Vec3 ahead = edge.getPosition(null, Math.min(t + step, 1.0));
        Vec3 behind = edge.getPosition(null, Math.max(t - step, 0.0));
        return ahead.subtract(behind).normalize();
    }

    public static double angleOn(TrackEdgePoint point, TrackEdge edge) {
        double basePos = point.isPrimary(edge.node1) ? edge.getLength() - point.position : point.position;
        Vec3 vec = directionAt(edge, basePos);
        double angle = getAngle(vec);
        return point.isPrimary(edge.node1) ? angle + 180 : angle;
    }
}
