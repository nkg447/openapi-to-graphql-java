package dev.nikunjgupta.provider;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

public class SchemaProvider {
    private final OpenAPI openAPI;

    public SchemaProvider(OpenAPI openAPI) {
        this.openAPI = openAPI;
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
