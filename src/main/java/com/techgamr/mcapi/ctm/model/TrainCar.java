package com.techgamr.mcapi.ctm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TrainCar {
    @JsonProperty("id")
    private int id;
    @JsonProperty("leading")
    private DimensionLocation leading;
    @JsonProperty("trailing")
    private DimensionLocation trailing;
    @JsonProperty("portal")
    private Portal portal;

    public TrainCar() {}

    public TrainCar(int id, DimensionLocation leading, DimensionLocation trailing, Portal portal) {
        this.id = id;
        this.leading = leading;
        this.trailing = trailing;
        this.portal = portal;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public DimensionLocation getLeading() { return leading; }
    public void setLeading(DimensionLocation leading) { this.leading = leading; }

    public DimensionLocation getTrailing() { return trailing; }
    public void setTrailing(DimensionLocation trailing) { this.trailing = trailing; }

    public Portal getPortal() { return portal; }
    public void setPortal(Portal portal) { this.portal = portal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainCar trainCar = (TrainCar) o;
        return id == trainCar.id &&
                Objects.equals(leading, trainCar.leading) &&
                Objects.equals(trailing, trainCar.trailing) &&
                Objects.equals(portal, trainCar.portal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, leading, trailing, portal);
    }

    @Override
    public String toString() {
        return "TrainCar{" +
                "id=" + id +
                ", leading=" + leading +
                ", trailing=" + trailing +
                ", portal=" + portal +
                '}';
    }
}
