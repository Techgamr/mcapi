package com.techgamr.mcapi.ctm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.simibubi.create.content.trains.signal.SignalBlock;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;

import java.util.Objects;
import java.util.UUID;

public class SignalSide {
    @JsonProperty("type")
    private SignalBlock.SignalType type;
    @JsonProperty("state")
    private SignalBlockEntity.SignalState state;
    @JsonProperty("angle")
    private double angle;
    @JsonProperty("block")
    private UUID block;

    public SignalSide() {}

    public SignalSide(SignalBlock.SignalType type, SignalBlockEntity.SignalState state, double angle, UUID block) {
        this.type = type;
        this.state = state;
        this.angle = angle;
        this.block = block;
    }

    public SignalBlock.SignalType getType() { return type; }
    public void setType(SignalBlock.SignalType type) { this.type = type; }

    public SignalBlockEntity.SignalState getState() { return state; }
    public void setState(SignalBlockEntity.SignalState state) { this.state = state; }

    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; }

    public UUID getBlock() { return block; }
    public void setBlock(UUID block) { this.block = block; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignalSide that = (SignalSide) o;
        return Double.compare(that.angle, angle) == 0 &&
                type == that.type &&
                state == that.state &&
                Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, state, angle, block);
    }

    @Override
    public String toString() {
        return "SignalSide{" +
                "type=" + type +
                ", state=" + state +
                ", angle=" + angle +
                ", block=" + block +
                '}';
    }
}
