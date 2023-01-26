package dev.nikunjgupta.provider;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides Schema object related stuff.
 * Should be one SchemaProvider for each openapi.
 */
public class SchemaProvider {
    private final OpenAPI openAPI;
    private static final Map<OpenAPI, SchemaProvider> OPEN_API_SCHEMA_PROVIDER_MAP=new HashMap<>();

    private SchemaProvider(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    public static SchemaProvider getOrCreateSchemaProvider(OpenAPI openAPI){
        if(!OPEN_API_SCHEMA_PROVIDER_MAP.containsKey(openAPI)){
            OPEN_API_SCHEMA_PROVIDER_MAP.put(openAPI, new SchemaProvider(openAPI));
        }
        return OPEN_API_SCHEMA_PROVIDER_MAP.get(openAPI);
    }

    public Schema getActualSchema(Schema schema) {
        if (schema.getType() != null)
            return schema;
        String schemaName = schema.get$ref().split("#/components/schemas/")[1];
        schema = openAPI.getComponents().getSchemas().get(schemaName);
        schema.setName(schemaName);
        return schema;
    }
}
