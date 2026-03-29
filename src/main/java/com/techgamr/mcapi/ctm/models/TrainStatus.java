package com.techgamr.mcapi.ctm.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class TrainStatus {
    @JsonProperty("trains")
    private List<CreateTrain> trains;

    public TrainStatus() {}

    public TrainStatus(List<CreateTrain> trains) {
        this.trains = trains;
    }

    public List<CreateTrain> getTrains() { return trains; }
    public void setTrains(List<CreateTrain> trains) { this.trains = trains; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainStatus that = (TrainStatus) o;
        return Objects.equals(trains, that.trains);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trains);
    }

    @Override
    public String toString() {
        return "TrainStatus{" +
                "trains=" + trains +
                '}';
    }
}
