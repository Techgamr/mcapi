package com.techgamr.mcapi.ctm.delta;

import com.fasterxml.jackson.annotation.JsonValue;

public enum JsonPatchOperationType {
    ADD,
    REMOVE,
    REPLACE,
    MOVE,
    COPY,
    TEST;

    @JsonValue
    public String toLowerCase() {
        return this.name().toLowerCase();
    }
}
