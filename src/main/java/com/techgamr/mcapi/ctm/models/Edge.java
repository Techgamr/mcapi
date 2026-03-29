package com.techgamr.mcapi.ctm.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class Edge {
    @JsonProperty("dimension")
    private String dimension;
    @JsonProperty("path")
    private List<Point> path;

    public Edge() {}

    public Edge(String dimension, List<Point> path) {
        this.dimension = dimension;
        this.path = path;
    }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public List<Point> getPath() { return path; }
    public void setPath(List<Point> path) { this.path = path; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(dimension, edge.dimension) &&
                Objects.equals(path, edge.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, path);
    }

    @Override
    public String toString() {
        return "Edge{" +
                "dimension='" + dimension + '\'' +
                ", path=" + path +
                '}';
    }
}
