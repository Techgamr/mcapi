package com.techgamr.mcapi.ctm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class Signal {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("dimension")
    private String dimension;
    @JsonProperty("location")
    private Point location;
    @JsonProperty("forward")
    private SignalSide forward;
    @JsonProperty("reverse")
    private SignalSide reverse;

    public Signal() {}

    public Signal(UUID id, String dimension, Point location, SignalSide forward, SignalSide reverse) {
        this.id = id;
        this.dimension = dimension;
        this.location = location;
        this.forward = forward;
        this.reverse = reverse;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public SignalSide getForward() { return forward; }
    public void setForward(SignalSide forward) { this.forward = forward; }

    public SignalSide getReverse() { return reverse; }
    public void setReverse(SignalSide reverse) { this.reverse = reverse; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Signal signal = (Signal) o;
        return Objects.equals(id, signal.id) &&
                Objects.equals(dimension, signal.dimension) &&
                Objects.equals(location, signal.location) &&
                Objects.equals(forward, signal.forward) &&
                Objects.equals(reverse, signal.reverse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dimension, location, forward, reverse);
    }

    @Override
    public String toString() {
        return "Signal{" +
                "id=" + id +
                ", dimension='" + dimension + '\'' +
                ", location=" + location +
                ", forward=" + forward +
                ", reverse=" + reverse +
                '}';
    }
}
