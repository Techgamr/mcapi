package com.techgamr.mcapi.ctm.delta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public class JsonDiffUtil {
    public static List<JsonPatchOperation> calculateDiff(JsonNode previous, JsonNode current) {
        List<JsonPatchOperation> patches = new ArrayList<>();
        calculateDiffRecursive(previous, current, "", patches);
        return patches;
    }

    private static void calculateDiffRecursive(JsonNode prev, JsonNode curr, String path, List<JsonPatchOperation> patches) {
        // If types differ or one is null
        if (prev == null && curr != null) {
            patches.add(new JsonPatchOperation(JsonPatchOperationType.ADD, path, curr));
            return;
        }
        if (prev != null && curr == null) {
            patches.add(new JsonPatchOperation(JsonPatchOperationType.REMOVE, path, null));
            return;
        }
        if (prev == null && curr == null) {
            return;
        }

        // Both are objects
        if (prev.isObject() && curr.isObject()) {
            ObjectNode prevObj = (ObjectNode) prev;
            ObjectNode currObj = (ObjectNode) curr;

            // Check for removed or modified fields
            prevObj.fieldNames().forEachRemaining(field -> {
                String newPath = path + "/" + field;
                if (!currObj.has(field)) {
                    patches.add(new JsonPatchOperation(JsonPatchOperationType.REMOVE, newPath, null));
                } else {
                    calculateDiffRecursive(prevObj.get(field), currObj.get(field), newPath, patches);
                }
            });

            // Check for added fields
            currObj.fieldNames().forEachRemaining(field -> {
                if (!prevObj.has(field)) {
                    String newPath = path + "/" + field;
                    patches.add(new JsonPatchOperation(JsonPatchOperationType.ADD, newPath, currObj.get(field)));
                }
            });
            return;
        }

        // Both are arrays
        if (prev.isArray() && curr.isArray()) {
            int prevSize = prev.size();
            int currSize = curr.size();

            // For simplicity, if sizes differ significantly, mark as replace
            if (Math.abs(prevSize - currSize) > 10) {
                patches.add(new JsonPatchOperation(JsonPatchOperationType.REPLACE, path, curr));
                return;
            }

            // Otherwise, check each element
            for (int i = 0; i < Math.min(prevSize, currSize); i++) {
                calculateDiffRecursive(prev.get(i), curr.get(i), path + "/" + i, patches);
            }

            // Handle size differences
            if (currSize > prevSize) {
                for (int i = prevSize; i < currSize; i++) {
                    patches.add(new JsonPatchOperation(JsonPatchOperationType.ADD, path + "/" + i, curr.get(i)));
                }
            } else if (prevSize > currSize) {
                for (int i = prevSize - 1; i >= currSize; i--) {
                    patches.add(new JsonPatchOperation(JsonPatchOperationType.REMOVE, path + "/" + i, null));
                }
            }
            return;
        }

        // Primitive types - only add patch if different
        if (!prev.equals(curr)) {
            patches.add(new JsonPatchOperation(JsonPatchOperationType.REPLACE, path, curr));
        }
    }
}
