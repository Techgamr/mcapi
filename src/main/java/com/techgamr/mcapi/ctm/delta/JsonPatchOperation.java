package com.techgamr.mcapi.ctm.delta;

/**
 * RFC 6902 format
 */
public class JsonPatchOperation {
    public JsonPatchOperationType op;
    public String path;
    public Object value;

    public JsonPatchOperation(JsonPatchOperationType op, String path, Object value) {
        this.op = op;
        this.path = path;
        this.value = value;
    }
}
