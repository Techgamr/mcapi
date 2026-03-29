package com.techgamr.mcapi.ctm.math;

import com.techgamr.mcapi.ctm.TrackWatcherUtils;
import com.techgamr.mcapi.ctm.model.Edge;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public record Line(String dimension, Vec3 start, Vec3 end) implements Track {
    @Override
    public Edge getSendable() {
        return new Edge(
                dimension,
                Arrays.asList(
                        TrackWatcherUtils.toSendable(start),
                        TrackWatcherUtils.toSendable(end)
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
}
