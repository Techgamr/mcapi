package com.techgamr.mcapi.ctm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Block {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("occupied")
    private boolean occupied;
    @JsonProperty("reserved")
    private boolean reserved;
    @JsonProperty("segments")
    private List<Edge> segments;

    public Block() {}

    public Block(UUID id, boolean occupied, boolean reserved, List<Edge> segments) {
        this.id = id;
        this.occupied = occupied;
        this.reserved = reserved;
        this.segments = segments;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }

    public boolean isReserved() { return reserved; }
    public void setReserved(boolean reserved) { this.reserved = reserved; }

    public List<Edge> getSegments() { return segments; }
    public void setSegments(List<Edge> segments) { this.segments = segments; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return occupied == block.occupied &&
                reserved == block.reserved &&
                Objects.equals(id, block.id) &&
                Objects.equals(segments, block.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, occupied, reserved, segments);
    }

    @Override
    public String toString() {
        return "Block{" +
                "id=" + id +
                ", occupied=" + occupied +
                ", reserved=" + reserved +
                ", segments=" + segments +
                '}';
    }
}
