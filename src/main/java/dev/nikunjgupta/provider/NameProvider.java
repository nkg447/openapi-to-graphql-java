package dev.nikunjgupta.provider;

import dev.nikunjgupta.Util;
import io.swagger.v3.oas.models.Operation;

import java.util.HashMap;
import java.util.Map;

public class NameProvider {
    private final Map<String, Integer> uniqueNameRecords = new HashMap<>();

    public NameProvider() {
    }

    public String getUniqueName(String name) {
        name = Util.nonNullOr(name, "Untitled");
        int count = uniqueNameRecords.getOrDefault(name, 0);
        uniqueNameRecords.put(name, count + 1);
        return name + (count > 0 ? count : "");
    }

    public String getOperationName(Operation operation, String path, String method) {
        return operation.getOperationId() != null ? operation.getOperationId() :
                method + path.replaceAll("\\/|\\{|}|-+", "_");
    }
}