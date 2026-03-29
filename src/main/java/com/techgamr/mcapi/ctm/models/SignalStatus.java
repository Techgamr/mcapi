package com.techgamr.mcapi.ctm.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class SignalStatus {
    @JsonProperty("signals")
    private List<Signal> signals;

    public SignalStatus() {}

    public SignalStatus(List<Signal> signals) {
        this.signals = signals;
    }

    public List<Signal> getSignals() { return signals; }
    public void setSignals(List<Signal> signals) { this.signals = signals; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignalStatus that = (SignalStatus) o;
        return Objects.equals(signals, that.signals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signals);
    }

    @Override
    public String toString() {
        return "SignalStatus{" +
                "signals=" + signals +
                '}';
    }
}
