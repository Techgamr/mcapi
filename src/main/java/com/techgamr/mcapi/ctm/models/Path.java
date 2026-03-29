package com.techgamr.mcapi.ctm.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Path {
    @JsonProperty("start")
    private Point start;
    @JsonProperty("firstControlPoint")
    private Point firstControlPoint;
    @JsonProperty("secondControlPoint")
    private Point secondControlPoint;
    @JsonProperty("end")
    private Point end;

    public Path() {}

    public Path(Point start, Point firstControlPoint, Point secondControlPoint, Point end) {
        this.start = start;
        this.firstControlPoint = firstControlPoint;
        this.secondControlPoint = secondControlPoint;
        this.end = end;
    }

    public Point getStart() { return start; }
    public void setStart(Point start) { this.start = start; }

    public Point getFirstControlPoint() { return firstControlPoint; }
    public void setFirstControlPoint(Point firstControlPoint) { this.firstControlPoint = firstControlPoint; }

    public Point getSecondControlPoint() { return secondControlPoint; }
    public void setSecondControlPoint(Point secondControlPoint) { this.secondControlPoint = secondControlPoint; }

    public Point getEnd() { return end; }
    public void setEnd(Point end) { this.end = end; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path path = (Path) o;
        return Objects.equals(start, path.start) &&
                Objects.equals(firstControlPoint, path.firstControlPoint) &&
                Objects.equals(secondControlPoint, path.secondControlPoint) &&
                Objects.equals(end, path.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, firstControlPoint, secondControlPoint, end);
    }

    @Override
    public String toString() {
        return "Path{" +
                "start=" + start +
                ", firstControlPoint=" + firstControlPoint +
                ", secondControlPoint=" + secondControlPoint +
                ", end=" + end +
                '}';
    }
}
