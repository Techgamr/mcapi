package com.techgamr.mcapi.ctm.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CreateTrain {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("owner")
    private String owner;
    @JsonProperty("cars")
    private List<TrainCar> cars;
    @JsonProperty("backwards")
    private boolean backwards;
    @JsonProperty("stopped")
    private boolean stopped;

    public CreateTrain() {}

    public CreateTrain(UUID id, String name, String owner, List<TrainCar> cars, boolean backwards, boolean stopped) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.cars = cars;
        this.backwards = backwards;
        this.stopped = stopped;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public List<TrainCar> getCars() { return cars; }
    public void setCars(List<TrainCar> cars) { this.cars = cars; }

    public boolean isBackwards() { return backwards; }
    public void setBackwards(boolean backwards) { this.backwards = backwards; }

    public boolean isStopped() { return stopped; }
    public void setStopped(boolean stopped) { this.stopped = stopped; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateTrain that = (CreateTrain) o;
        return backwards == that.backwards &&
                stopped == that.stopped &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(cars, that.cars);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, owner, cars, backwards, stopped);
    }

    @Override
    public String toString() {
        return "CreateTrain{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", cars=" + cars +
                ", backwards=" + backwards +
                ", stopped=" + stopped +
                '}';
    }
}
