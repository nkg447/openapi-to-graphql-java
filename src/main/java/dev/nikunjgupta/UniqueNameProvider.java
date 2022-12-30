package dev.nikunjgupta;

import java.util.HashMap;
import java.util.Map;

public class UniqueNameProvider {
    private final Map<String, Integer> uniqueNameRecords = new HashMap<>();

    public UniqueNameProvider() {
    }

    public String getUniqueName(String name) {
        name = Util.nonNullOr(name, "anonymous");
        int count = uniqueNameRecords.getOrDefault(name, 0);
        uniqueNameRecords.put(name, count + 1);
        return name + (count > 0 ? count : "");
    }
}
