package dev.nikunjgupta.provider;

import dev.nikunjgupta.Util;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.Operation;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides Name related stuff
 * Singleton
 */
public class NameProvider {
    private static NameProvider INSTANCE = new NameProvider();
    private final Map<String, Integer> uniqueNameRecords = new HashMap<>();

    /**
     * C'tor
     */
    private NameProvider() {
    }

    /**
     * @return instance of NameProvider
     */
    public static NameProvider getInstance() {
        return INSTANCE;
    }

    /**
     * @return A unique unused name (postfix a count)
     */
    public String getUniqueName(String name) {
        name = Util.nonNullOr(name, "Untitled");
        int count = uniqueNameRecords.getOrDefault(name, 0);
        uniqueNameRecords.put(name, count + 1);
        return name + (count > 0 ? count : "");
    }

    /**
     * @return Operation's name with replaced special characters
     */
    public String getOperationName(Operation operation, String path, HttpMethod method) {
        return operation.getOperationId() != null ? operation.getOperationId() :
                method.name().toLowerCase() + path.replaceAll("\\/|\\{|}|-+", "_");
    }

    /**
     * look for a common origin name
     *
     * @return common origin name if found, else null
     */
    public String getCommonOriginName(String name1, String name2) {
        for (String name : uniqueNameRecords.keySet()) {
            if (name1.startsWith(name) && name2.startsWith(name)) {
                return name;
            }
        }
        return null;
    }

    /**
     * Free up 1 instance of name
     *
     * @param name name to free up
     */
    public void freeName(String name) {
        uniqueNameRecords.put(name, uniqueNameRecords.get(name) - 1);
    }

    /**
     * Reset the Singleton
     */
    public void reset() {
        INSTANCE = new NameProvider();
    }
}
