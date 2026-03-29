package com.techgamr.mcapi.ctm.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class Network {
    @JsonProperty("tracks")
    private List<Edge> tracks;
    @JsonProperty("portals")
    private List<Portal> portals;
    @JsonProperty("stations")
    private List<Station> stations;

    public Network() {}

    public Network(List<Edge> tracks, List<Portal> portals, List<Station> stations) {
        this.tracks = tracks;
        this.portals = portals;
        this.stations = stations;
    }

    public List<Edge> getTracks() { return tracks; }
    public void setTracks(List<Edge> tracks) { this.tracks = tracks; }

    public List<Portal> getPortals() { return portals; }
    public void setPortals(List<Portal> portals) { this.portals = portals; }

    public List<Station> getStations() { return stations; }
    public void setStations(List<Station> stations) { this.stations = stations; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Network network = (Network) o;
        return Objects.equals(tracks, network.tracks) &&
                Objects.equals(portals, network.portals) &&
                Objects.equals(stations, network.stations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tracks, portals, stations);
    }

    @Override
    public String toString() {
        return "Network{" +
                "tracks=" + tracks +
                ", portals=" + portals +
                ", stations=" + stations +
                '}';
    }
}
