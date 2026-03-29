package com.techgamr.mcapi.ctm;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.signal.SignalBlock;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.techgamr.mcapi.ctm.math.Track;
import com.techgamr.mcapi.ctm.math.TrackDivision;
import com.techgamr.mcapi.ctm.math.TrackMath;
import com.techgamr.mcapi.ctm.model.Signal;
import com.techgamr.mcapi.ctm.model.SignalSide;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class CreateSignal {
    private final SignalBoundary internal;
    private final TrackEdge edge;
    private Track forwardSegment;
    private Track reverseSegment;

    public CreateSignal(SignalBoundary internal, TrackEdge edge) {
        this.internal = internal;
        this.edge = edge;

        TrackDivision divided = TrackWatcherUtils.getPath(edge).divideAt(internal.position / edge.getLength());
        this.forwardSegment = divided.first();
        this.reverseSegment = divided.second();
    }

    public UUID getId() {
        return internal.id;
    }

    public String getDimension() {
        return edge.node1.getLocation().dimension.toString();
    }

    public Vec3 getLocation() {
        return TrackMath.locationOn(internal, edge);
    }

    public float getForwardAngle() {
        return (float) TrackMath.angleOn(internal, edge);
    }

    public float getReverseAngle() {
        return getForwardAngle() + 180;
    }

    @Nullable
    public SignalBlock.SignalType getForwardType() {
        return internal.types.getFirst();
    }

    @Nullable
    public SignalBlock.SignalType getReverseType() {
        return internal.types.getSecond();
    }

    @Nullable
    public SignalBlockEntity.SignalState getForwardState() {
        return internal.cachedStates.getFirst();
    }

    @Nullable
    public SignalBlockEntity.SignalState getReverseState() {
        return internal.cachedStates.getSecond();
    }

    @Nullable
    public SignalEdgeGroup getForwardGroup() {
        return TrackWatcher.RR.signalEdgeGroups.get(internal.groups.getFirst());
    }

    @Nullable
    public SignalEdgeGroup getReverseGroup() {
        return TrackWatcher.RR.signalEdgeGroups.get(internal.groups.getSecond());
    }

    public Signal getSendable() {
        SignalSide forward = null;
        if (getForwardState() != null && getForwardState() != SignalBlockEntity.SignalState.INVALID) {
            forward = new SignalSide(
                    getForwardType(),
                    getForwardState(),
                    getForwardAngle(),
                    getForwardGroup() != null ? getForwardGroup().id : null
            );
        }

        SignalSide reverse = null;
        if (getReverseState() != null && getReverseState() != SignalBlockEntity.SignalState.INVALID) {
            reverse = new SignalSide(
                    getReverseType(),
                    getReverseState(),
                    getReverseAngle(),
                    getReverseGroup() != null ? getReverseGroup().id : null
            );
        }

        return new Signal(
                getId(),
                getDimension(),
                TrackWatcherUtils.toSendable(getLocation()),
                forward,
                reverse
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateSignal that = (CreateSignal) o;
        return Objects.equals(internal, that.internal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internal);
    }
}
