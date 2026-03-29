package com.techgamr.mcapi.ctm.math;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

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

    public static List<Vec3> multiLerp(List<Vec3> points, double position) {
        List<Vec3> result = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            result.add(a.lerp(b, position));
        }
        return result;
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
