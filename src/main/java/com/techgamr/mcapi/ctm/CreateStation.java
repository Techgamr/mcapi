package com.techgamr.mcapi.ctm;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.techgamr.mcapi.ctm.math.TrackMath;
import com.techgamr.mcapi.ctm.model.Station;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

public class CreateStation {
    private final GlobalStation internal;
    private final TrackEdge edge;

    public CreateStation(GlobalStation internal, TrackEdge edge) {
        this.internal = internal;
        this.edge = edge;
    }

    public UUID getId() {
        return internal.id;
    }

    public String getName() {
        return internal.name;
    }

    public String getDimension() {
        return TrackWatcherUtils.dimensionString(edge.node1.getLocation().dimension);
    }

    public Vec3 getLocation() {
        return TrackMath.locationOn(internal, edge);
    }

    public float getAngle() {
        return (float) TrackMath.angleOn(internal, edge);
    }

    public boolean isAssembling() {
        return internal.assembling;
    }

    public Station getSendable() {
        return new Station(
                getId(),
                getName(),
                getDimension(),
                TrackWatcherUtils.toSendable(getLocation()),
                getAngle(),
                isAssembling()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateStation that = (CreateStation) o;
        return Objects.equals(internal, that.internal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internal);
    }
}
