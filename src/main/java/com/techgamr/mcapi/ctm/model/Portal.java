package com.techgamr.mcapi.ctm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Portal {
    @JsonProperty("from")
    private DimensionLocation from;
    @JsonProperty("to")
    private DimensionLocation to;

    public Portal() {}

    public Portal(DimensionLocation from, DimensionLocation to) {
        this.from = from;
        this.to = to;
    }

    public DimensionLocation getFrom() { return from; }
    public void setFrom(DimensionLocation from) { this.from = from; }

    public DimensionLocation getTo() { return to; }
    public void setTo(DimensionLocation to) { this.to = to; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Portal portal = (Portal) o;
        return Objects.equals(from, portal.from) &&
                Objects.equals(to, portal.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return "Portal{" +
                "from=" + from +
                ", to=" + to +
                '}';
    }
}
