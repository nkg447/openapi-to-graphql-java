package dev.nikunjgupta;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class Converter {
    private static final Map<String, GraphQLType> SCHEMA_TO_GRAPHQL_TYPE_MAP =
            new HashMap<>();

    static {
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("integer", Scalars.GraphQLInt);
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("number", Scalars.GraphQLFloat);
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("string", Scalars.GraphQLString);
        SCHEMA_TO_GRAPHQL_TYPE_MAP.put("boolean", Scalars.GraphQLBoolean);
    }

    private final Map<Schema, GraphQLType> graphQlTypes = new HashMap<>();
    private final OpenAPI openAPI;
    private final Map<String, Integer> uniqueNameRecords = new HashMap<>();

    public Converter(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    private GraphQLOutputType getGraphQlOutputType(Schema schema) {
        return (GraphQLOutputType) getGraphQlType(schema);
    }

    private GraphQLInputType getGraphQlInputType(Schema schema) {
        return (GraphQLInputType) getGraphQlType(schema, null);
    }

    private GraphQLInputType getGraphQlInputType(Schema schema, String defaultName) {
        GraphQLType type = getGraphQlType(schema, defaultName);
        if(type instanceof GraphQLInputType)
            return (GraphQLInputType) type;
        return null;
    }

    private GraphQLType getGraphQlType(Schema schema) {
        return getGraphQlType(schema, null);
    }

    private GraphQLType getGraphQlType(Schema schema, String defaultName) {
        if (schema.getType() == null && schema.get$ref() != null) {
            // get actual schema object from the reference
            schema = getActualSchemaObject(schema);
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
            GraphQLEnumType.Builder gqlEnumBuilder = GraphQLEnumType.newEnum()
                    .name(getUniqueName(Util.nonNullOr(schema.getName(), defaultName)))
                    .description(schema.getDescription());
            for (Object value : schema.getEnum()) {
                gqlEnumBuilder.value(value.toString());
            }
            return gqlEnumBuilder.build();
        }

        return SCHEMA_TO_GRAPHQL_TYPE_MAP.get(schema.getType());
    }

    public GraphQLSchema generateSchema() {
        populateGraphQlTypes();
        GraphQLObjectType.Builder queryTypeBuilder = GraphQLObjectType.newObject()
                .name("Query");
        boolean mutationAdded = false;
        GraphQLObjectType.Builder mutationTypeBuilder = GraphQLObjectType.newObject()
                .name("Mutation");

        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            PathItem pathItem = entry.getValue();

            if (pathItem.getGet() != null) {
                Operation operation = pathItem.getGet();
                GraphQLFieldDefinition.Builder fieldBuilder = getGraphqlField(operation,
                        entry.getKey(), "get");
                if (fieldBuilder == null) {
                    System.out.println(entry.getKey() + " path could not be converted to graphQl");
                    continue;
                }
                queryTypeBuilder = queryTypeBuilder
                        .field(fieldBuilder);
            } else {
                String method = "";
                Operation operation = null;
                if ((pathItem.getPost() != null && (method = "post") != null && (operation =
                        pathItem.getPost()) != null)
                        || (pathItem.getPatch() != null && (method = "patch") != null && (operation = pathItem.getPatch()) != null)
                        || (pathItem.getPut() != null && (method = "put") != null && (operation =
                        pathItem.getPut()) != null)
                ) {
                }

                GraphQLFieldDefinition.Builder fieldBuilder = getGraphqlField(operation,
                        entry.getKey(), method);
                if (fieldBuilder == null) {
                    System.out.println(entry.getKey() + " path could not be converted to graphQl");
                    continue;
                }
                mutationTypeBuilder = mutationTypeBuilder
                        .field(fieldBuilder);
                mutationAdded = true;
            }
        }

        GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema().query(queryTypeBuilder);

        if (mutationAdded)
            schemaBuilder.mutation(mutationTypeBuilder);

        return schemaBuilder.build();
    }

    private GraphQLFieldDefinition.Builder getGraphqlField(Operation operation, String path,
                                                           String method) {
        GraphQLFieldDefinition.Builder fieldBuilder = GraphQLFieldDefinition.newFieldDefinition()
                .name(getUniqueName(getOperationName(operation, path, method)))
                .description(Util.nonNullOr(operation.getDescription(), operation.getSummary()));

        Schema responseSchema = getResponseSchema(operation);
        if (responseSchema == null) {
            return null;
        }

        fieldBuilder.type(getGraphQlOutputType(responseSchema));

        for (Parameter parameter : Util.nonNullOr(operation.getParameters(),
                new LinkedList<Parameter>())) {
            fieldBuilder.argument(GraphQLArgument.newArgument()
                    .name(parameter.getName())
                    .description(parameter.getDescription())
                    .type(getGraphQlInputType(parameter.getSchema(), parameter.getName()))
                    .build());
        }

        if (operation.getRequestBody() != null) {
            RequestBody requestBody = operation.getRequestBody();
            Schema requestSchema = getRequestSchema(requestBody);
            if (requestSchema == null) return null;
            requestSchema = getActualSchemaObject(requestSchema);
            if (requestSchema.getName() == null) {
                requestSchema.setName(getOperationName(operation, path, method) + "Input");
            }
            fieldBuilder.argument(GraphQLArgument.newArgument()
                    .name(requestSchema.getName())
                    .description(requestBody.getDescription())
                    .type(getGraphQlInputType(requestSchema))
                    .build());
        }

        return fieldBuilder;
    }

    private Schema getRequestSchema(RequestBody requestBody) {
        Content requestContent = requestBody.getContent();
        if (requestContent.size() == 0)
            return null;
        return requestContent.entrySet().stream().findFirst()
                .get().getValue().getSchema();
    }

    private Schema getResponseSchema(Operation operation) {
        // fetched the responseSchema
        // TODO: better logic
        Map<String, ApiResponse> responseMap = operation.getResponses();
        if (responseMap.size() == 0)
            return null;
        Content responseContent = responseMap.entrySet().stream().findFirst().get().getValue()
                .getContent();
        if (responseContent.size() == 0)
            return null;
        return responseContent.entrySet().stream().findFirst()
                .get().getValue().getSchema();
    }

    private String getOperationName(Operation operation, String path, String method) {
        return operation.getOperationId() != null ? operation.getOperationId() :
                method + path.replace("/", "_")
                        .replace("{", "")
                        .replace("}", "");
    }

    private void populateGraphQlTypes() {
        Map<String, Schema> schemaMap = openAPI.getComponents().getSchemas();

        for (Map.Entry<String, Schema> entry : schemaMap.entrySet()) {
            if (!graphQlTypes.containsKey(entry.getValue())) {
                graphQlTypes.put(entry.getValue(), convertToGraphQLObjectType(entry.getKey(),
                        (ObjectSchema) entry.getValue()));
            }
        }
    }

    private GraphQLObjectType convertToGraphQLObjectType(String name, ObjectSchema objectSchema) {
        GraphQLObjectType.Builder graphQlObjectTypeBuilder = GraphQLObjectType.newObject()
                .name(getUniqueName(name))
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

    private Schema getActualSchemaObject(Schema schema) {
        if (schema.getType() != null)
            return schema;
        String schemaName = schema.get$ref().split("#/components/schemas/")[1];
        schema = openAPI.getComponents().getSchemas().get(schemaName);
        schema.setName(schemaName);
        return schema;
    }

    private String getUniqueName(String name) {
        name = Util.nonNullOr(name, "anonymous");
        int count = uniqueNameRecords.getOrDefault(name, 0);
        uniqueNameRecords.put(name, count + 1);
        return name + (count > 0 ? count : "");
    }

}
