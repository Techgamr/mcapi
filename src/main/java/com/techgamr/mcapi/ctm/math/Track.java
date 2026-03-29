package com.techgamr.mcapi.ctm.math;

import com.techgamr.mcapi.ctm.model.Edge;

// Track interface and implementations
public interface Track {
    TrackDivision divideAt(double position);

    Edge getSendable();
}
