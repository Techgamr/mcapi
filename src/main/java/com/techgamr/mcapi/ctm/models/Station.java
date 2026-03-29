package com.techgamr.mcapi.ctm.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

public class Station {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("dimension")
    private String dimension;
    @JsonProperty("location")
    private Point location;
    @JsonProperty("angle")
    private double angle;
    @JsonProperty("assembling")
    private boolean assembling;

    public Station() {}

    public Station(UUID id, String name, String dimension, Point location, double angle, boolean assembling) {
        this.id = id;
        this.name = name;
        this.dimension = dimension;
        this.location = location;
        this.angle = angle;
        this.assembling = assembling;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; }

    public boolean isAssembling() { return assembling; }
    public void setAssembling(boolean assembling) { this.assembling = assembling; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Station station = (Station) o;
        return Double.compare(station.angle, angle) == 0 &&
                assembling == station.assembling &&
                Objects.equals(id, station.id) &&
                Objects.equals(name, station.name) &&
                Objects.equals(dimension, station.dimension) &&
                Objects.equals(location, station.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, dimension, location, angle, assembling);
    }

    @Override
    public String toString() {
        return "Station{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", dimension='" + dimension + '\'' +
                ", location=" + location +
                ", angle=" + angle +
                ", assembling=" + assembling +
                '}';
    }
}
