package com.techgamr.mcapi.ctm;

import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.techgamr.mcapi.ctm.math.Track;
import com.techgamr.mcapi.ctm.model.Block;
import com.techgamr.mcapi.ctm.model.Portal;

import java.util.*;

public class CreateSignalBlock {
    private final SignalEdgeGroup internal;
    public final List<Track> segments = new ArrayList<>();
    public final Set<Portal> portals = new HashSet<>();

    public CreateSignalBlock(SignalEdgeGroup internal) {
        this.internal = internal;
    }

    public UUID getId() {
        return internal.id;
    }

    public boolean isOccupied() {
        return TrackWatcher.RR.trains.values().stream()
                .anyMatch(train -> train.occupiedSignalBlocks.containsKey(getId()));
    }

    public boolean isReserved() {
        return TrackWatcher.RR.trains.values().stream()
                .anyMatch(train -> train.reservedSignalBlocks.contains(getId()));
    }

    public Block getSendable() {
        return new Block(
                getId(),
                isOccupied(),
                isReserved(),
                segments.stream()
                        .map(Track::getSendable)
                        .toList()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateSignalBlock that = (CreateSignalBlock) o;
        return Objects.equals(internal, that.internal) &&
                Objects.equals(segments, that.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internal);
    }
}
