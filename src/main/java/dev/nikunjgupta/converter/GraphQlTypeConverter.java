package dev.nikunjgupta.converter;

import dev.nikunjgupta.Util;
import dev.nikunjgupta.provider.SchemaProvider;
import dev.nikunjgupta.provider.NameProvider;
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
    private final Map<Schema, GraphQLOutputType> graphQlTypes = new HashMap<>();

    private final OpenAPI openAPI;
    private final SchemaProvider schemaProvider;
    private final NameProvider nameProvider;

    public GraphQlTypeConverter(OpenAPI openAPI, SchemaProvider schemaProvider,
                                NameProvider nameProvider) {
        this.openAPI = openAPI;
        this.schemaProvider = schemaProvider;
        this.nameProvider = nameProvider;
    }

    public GraphQLType getGraphQlType(Schema schema) {
        return getGraphQlType(schema, null);
    }

    public GraphQLType getGraphQlType(Schema schema, String defaultName) {
        if (schema.getType() == null && schema.get$ref() != null) {
            // get actual schema object from the reference
            schema = schemaProvider.getActualSchema(schema);
            return getGraphQlType(schema, defaultName);
        }
        if (schema.getType().equals("array")) {
            ArraySchema arraySchema = (ArraySchema) schema;
            Schema arrayItemSchema = arraySchema.getItems();
            GraphQLType arrayItemGraphQlType = getGraphQlType(arrayItemSchema, defaultName);
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
            if(!graphQlTypes.containsKey(schema)){
                GraphQLEnumType.Builder gqlEnumBuilder = GraphQLEnumType.newEnum()
                        .name(nameProvider.getUniqueName(Util.nonNullOr(schema.getName(),
                                defaultName)))
                        .description(schema.getDescription());
                for (Object value : schema.getEnum()) {
                    gqlEnumBuilder.value(value.toString());
                }
                graphQlTypes.put(schema, gqlEnumBuilder.build());
            }

            return graphQlTypes.get(schema);
        }

        return SCHEMA_TO_GRAPHQL_TYPE_MAP.get(schema.getType());
    }

    private GraphQLObjectType convertToGraphQLObjectType(String name, ObjectSchema objectSchema) {
        GraphQLObjectType.Builder graphQlObjectTypeBuilder = GraphQLObjectType.newObject()
                .name(nameProvider.getUniqueName(name))
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
                            .type((GraphQLOutputType) getGraphQlType(schema)));
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
                    .name(nameProvider.getUniqueName(Util.nonNullOr(schema.getName(),
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
                .name(nameProvider.getUniqueName(name))
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
