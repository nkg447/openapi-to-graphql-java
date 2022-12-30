package dev.nikunjgupta;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.HashMap;
import java.util.Map;

public class GraphQlTypeConverter {
    private final Map<Schema, GraphQLInputType> graphQlInputTypes = new HashMap<>();
    private final Map<Schema, GraphQLOutputType> graphQlOutputTypes = new HashMap<>();
    private final OpenAPI openAPI;
    private SchemaProvider schemaProvider;
    private UniqueNameProvider uniqueNameProvider;

    private static final Map<String, GraphQLType> SCHEMA_TO_GRAPHQL_TYPE_MAP =
            new HashMap<>();

    static {
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("integer", Scalars.GraphQLInt);
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("number", Scalars.GraphQLFloat);
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("string", Scalars.GraphQLString);
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("boolean", Scalars.GraphQLBoolean);
    }

    public GraphQlTypeConverter(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    private GraphQLOutputType getGraphQlOutputType(Schema schema, String defaultName) {
        if (schema.getType() == null && schema.get$ref() != null) {
            // get actual schema object from the reference
            schema = schemaProvider.getActualSchema(schema);
            return getGraphQlOutputType(schema, defaultName);
        }
        if (schema.getType().equals("array")) {
            ArraySchema arraySchema = (ArraySchema) schema;
            Schema arrayItemSchema = arraySchema.getItems();
            GraphQLType arrayItemGraphQlType = getGraphQlOutputType(arrayItemSchema, defaultName);
            return GraphQLList.list(arrayItemGraphQlType);
        }
        if (schema.getType().equals("object")) {
            if (!graphQlTypes.containsKey(schema)) {
                if (schema instanceof ObjectSchema)
                    graphQlTypes.put(schema, convertToGraphQLObjectType(schema.getName(),
                            (ObjectSchema) schema));
                else
                    graphQlTypes.put(schema, ExtendedScalars.Object);
            }
            return graphQlTypes.get(schema);
        }

        if (schema.getEnum() != null) {
            GraphQLEnumType.Builder gqlEnumBuilder = GraphQLEnumType.newEnum()
                    .name(uniqueNameProvider.getUniqueName(Util.nonNullOr(schema.getName(), defaultName)))
                    .description(schema.getDescription());
            for (Object value : schema.getEnum()) {
                gqlEnumBuilder.value(value.toString());
            }
            return gqlEnumBuilder.build();
        }

        return SCHEMA_TO_GRAPHQL_TYPE_MAP.get(schema.getType());
    }
}
