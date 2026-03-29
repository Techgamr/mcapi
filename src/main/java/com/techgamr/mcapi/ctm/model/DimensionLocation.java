package com.techgamr.mcapi.ctm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class DimensionLocation {
    @JsonProperty("dimension")
    private String dimension;
    @JsonProperty("location")
    private Point location;

    public DimensionLocation() {}

    public DimensionLocation(String dimension, Point location) {
        this.dimension = dimension;
        this.location = location;
    }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DimensionLocation that = (DimensionLocation) o;
        return Objects.equals(dimension, that.dimension) &&
                Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, location);
    }

    @Override
    public String toString() {
        return "DimensionLocation{" +
                "dimension='" + dimension + '\'' +
                ", location=" + location +
                '}';
    }
}
