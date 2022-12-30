package dev.nikunjgupta.converter;

import dev.nikunjgupta.Util;
import dev.nikunjgupta.provider.SchemaProvider;
import dev.nikunjgupta.provider.UniqueNameProvider;
import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GraphQlTypeConverter {
    private static final Map<String, GraphQLType> SCHEMA_TO_GRAPHQL_TYPE_MAP =
            new HashMap<>();

    static {
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("integer", Scalars.GraphQLInt);
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("number", Scalars.GraphQLFloat);
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("string", Scalars.GraphQLString);
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("boolean", Scalars.GraphQLBoolean);
    }

    private final Map<Schema, GraphQLInputType> graphQlInputTypes = new HashMap<>();
    private final Map<Schema, GraphQLOutputType> graphQlOutputTypes = new HashMap<>();

    private final OpenAPI openAPI;
    private final SchemaProvider schemaProvider;
    private final UniqueNameProvider uniqueNameProvider;

    public GraphQlTypeConverter(OpenAPI openAPI, SchemaProvider schemaProvider,
                                UniqueNameProvider uniqueNameProvider) {
        this.openAPI = openAPI;
        this.schemaProvider = schemaProvider;
        this.uniqueNameProvider = uniqueNameProvider;
    }

    public GraphQLOutputType getGraphQlOutputType(Schema schema) {
        return getGraphQlOutputType(schema, null);
    }

    public GraphQLOutputType getGraphQlOutputType(Schema schema, String defaultName) {
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
            if (!graphQlOutputTypes.containsKey(schema)) {
                if (schema instanceof ObjectSchema)
                    graphQlOutputTypes.put(schema, convertToGraphQLObjectType(schema.getName(),
                            (ObjectSchema) schema));
                else
                    graphQlOutputTypes.put(schema, ExtendedScalars.Object);
            }
            return graphQlOutputTypes.get(schema);
        }

        if (schema.getEnum() != null) {
            GraphQLEnumType.Builder gqlEnumBuilder = GraphQLEnumType.newEnum()
                    .name(uniqueNameProvider.getUniqueName(Util.nonNullOr(schema.getName(),
                            defaultName)))
                    .description(schema.getDescription());
            for (Object value : schema.getEnum()) {
                gqlEnumBuilder.value(value.toString());
            }
            return gqlEnumBuilder.build();
        }

        return (GraphQLOutputType) SCHEMA_TO_GRAPHQL_TYPE_MAP.get(schema.getType());
    }

    private GraphQLObjectType convertToGraphQLObjectType(String name, ObjectSchema objectSchema) {
        GraphQLObjectType.Builder graphQlObjectTypeBuilder = GraphQLObjectType.newObject()
                .name(uniqueNameProvider.getUniqueName(name))
                .description(objectSchema.getDescription());
        Map<String, Schema> schemaMap = objectSchema.getProperties();

        if (schemaMap == null)
            schemaMap = Collections.emptyMap();

        for (Map.Entry<String, Schema> entry : schemaMap.entrySet()) {
            Schema schema = entry.getValue();
            schema.setName(entry.getKey());
            graphQlObjectTypeBuilder = graphQlObjectTypeBuilder
                    .field(GraphQLFieldDefinition.newFieldDefinition()
                            .name(entry.getKey())
                            .type(getGraphQlOutputType(schema)));
        }

        return graphQlObjectTypeBuilder.build();
    }

    public GraphQLInputType getGraphQlInputType(Schema schema, String defaultName) {
        if (schema.getType() == null && schema.get$ref() != null) {
            // get actual schema object from the reference
            schema = schemaProvider.getActualSchema(schema);
            return getGraphQlInputType(schema, defaultName);
        }
        if (schema.getType().equals("array")) {
            ArraySchema arraySchema = (ArraySchema) schema;
            Schema arrayItemSchema = arraySchema.getItems();
            GraphQLType arrayItemGraphQlType = getGraphQlInputType(arrayItemSchema, defaultName);
            return GraphQLList.list(arrayItemGraphQlType);
        }
        if (schema.getType().equals("object")) {
            if (!graphQlInputTypes.containsKey(schema)) {
                if (schema instanceof ObjectSchema)
                    graphQlInputTypes.put(schema, convertToGraphQLInputObjectType(schema.getName(),
                            (ObjectSchema) schema));
                else
                    graphQlInputTypes.put(schema, ExtendedScalars.Object);
            }
            return graphQlInputTypes.get(schema);
        }

        if (schema.getEnum() != null) {
            GraphQLEnumType.Builder gqlEnumBuilder = GraphQLEnumType.newEnum()
                    .name(uniqueNameProvider.getUniqueName(Util.nonNullOr(schema.getName(),
                            defaultName)))
                    .description(schema.getDescription());
            for (Object value : schema.getEnum()) {
                gqlEnumBuilder.value(value.toString());
            }
            return gqlEnumBuilder.build();
        }

        return (GraphQLInputType) SCHEMA_TO_GRAPHQL_TYPE_MAP.get(schema.getType());
    }

    private GraphQLInputType convertToGraphQLInputObjectType(String name,
                                                             ObjectSchema objectSchema) {
        GraphQLInputObjectType.Builder gqlInputObjectTypeBuilder =
                GraphQLInputObjectType.newInputObject()
                .name(uniqueNameProvider.getUniqueName(name))
                .description(objectSchema.getDescription());

        Map<String, Schema> schemaMap = objectSchema.getProperties();

        if (schemaMap == null)
            schemaMap = Collections.emptyMap();

        for (Map.Entry<String, Schema> entry : schemaMap.entrySet()) {
            Schema schema = entry.getValue();
            schema.setName(entry.getKey());
            gqlInputObjectTypeBuilder
                    .field(GraphQLInputObjectField.newInputObjectField()
                            .name(entry.getKey())
                            .type(getGraphQlInputType(schema)));
        }

        return gqlInputObjectTypeBuilder.build();
    }

    public GraphQLInputType getGraphQlInputType(Schema schema) {
        return getGraphQlInputType(schema, null);
    }
}
