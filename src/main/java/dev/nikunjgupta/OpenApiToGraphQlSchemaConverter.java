package dev.nikunjgupta;

import dev.nikunjgupta.converter.GraphQlTypeConverter;
import dev.nikunjgupta.provider.NameProvider;
import dev.nikunjgupta.provider.SchemaProvider;
import graphql.schema.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.LinkedList;
import java.util.Map;

/**
 * Actual Converter for openapi to graphql schema conversion
 */
public class OpenApiToGraphQlSchemaConverter {
    private final OpenAPI openAPI;
    private final SchemaProvider schemaProvider;
    private final NameProvider nameProvider;
    private final GraphQlTypeConverter graphQlTypeConverter;

    public OpenApiToGraphQlSchemaConverter(OpenAPI openAPI) {
        this.openAPI = openAPI;
        this.schemaProvider = SchemaProvider.getOrCreateSchemaProvider(openAPI);
        this.nameProvider = NameProvider.getInstance();
        this.graphQlTypeConverter = new GraphQlTypeConverter(openAPI, schemaProvider,
                nameProvider);
    }

    public GraphQLSchema generateSchema() {

        GraphQLObjectType.Builder queryTypeBuilder = GraphQLObjectType.newObject()
                .name("Query");
        boolean mutationAdded = false;
        GraphQLObjectType.Builder mutationTypeBuilder = GraphQLObjectType.newObject()
                .name("Mutation");

        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            PathItem pathItem = entry.getValue();
            String method = "";
            Operation operation = null;
            if ((pathItem.getGet() != null && (method = "get") != null && (operation =
                    pathItem.getGet()) != null)) {
                GraphQLFieldDefinition.Builder fieldBuilder = getGraphqlField(operation,
                        entry.getKey(), "get");
                if (fieldBuilder == null) {
                    System.out.println(method + " " + entry.getKey() + " path could not be " +
                            "converted to graphQl");
                } else {
                    queryTypeBuilder = queryTypeBuilder
                            .field(fieldBuilder);
                }
            }
            if ((pathItem.getPost() != null && (method = "post") != null && (operation =
                    pathItem.getPost()) != null)) {
                mutationAdded = addOperationToMutation(mutationTypeBuilder,
                        operation, method, entry.getKey()) || mutationAdded;
            }
            if ((pathItem.getPut() != null && (method = "put") != null && (operation =
                    pathItem.getPut()) != null)) {
                mutationAdded = addOperationToMutation(mutationTypeBuilder,
                        operation, method, entry.getKey()) || mutationAdded;
            }
            if ((pathItem.getPatch() != null && (method = "patch") != null && (operation =
                    pathItem.getPatch()) != null)) {
                mutationAdded = addOperationToMutation(mutationTypeBuilder,
                        operation, method, entry.getKey()) || mutationAdded;
            }
        }

        GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema().query(queryTypeBuilder);

        if (mutationAdded)
            schemaBuilder.mutation(mutationTypeBuilder);

        return schemaBuilder.build();
    }

    private boolean addOperationToMutation(GraphQLObjectType.Builder mutationTypeBuilder,
                                           Operation operation, String method, String path) {
        GraphQLFieldDefinition.Builder fieldBuilder = getGraphqlField(operation,
                path, method);
        if (fieldBuilder == null) {
            System.out.println(method + " " + path + " path could not be converted to graphQl");
            return false;
        }
        mutationTypeBuilder
                .field(fieldBuilder);
        return true;
    }

    private GraphQLFieldDefinition.Builder getGraphqlField(Operation operation, String path,
                                                           String method) {
        GraphQLFieldDefinition.Builder fieldBuilder = GraphQLFieldDefinition.newFieldDefinition()
                .name(nameProvider.getUniqueName(nameProvider.getOperationName(operation, path,
                        method)))
                .description(Util.nonNullOr(operation.getDescription(), operation.getSummary()));

        Schema responseSchema = getResponseSchema(operation);
        if (responseSchema == null) {
            return null;
        }

        fieldBuilder.type((GraphQLOutputType) graphQlTypeConverter.getGraphQlType(responseSchema));

        for (Parameter parameter : Util.nonNullOr(operation.getParameters(),
                new LinkedList<Parameter>())) {
            fieldBuilder.argument(GraphQLArgument.newArgument()
                    .name(parameter.getName())
                    .description(parameter.getDescription())
                    .type(graphQlTypeConverter.getGraphQlInputType(parameter.getSchema(),
                            parameter.getName()))
                    .build());
        }

        if (operation.getRequestBody() != null) {
            RequestBody requestBody = operation.getRequestBody();
            Schema requestSchema = getRequestSchema(requestBody);
            if (requestSchema == null) return null;
            requestSchema = schemaProvider.getActualSchema(requestSchema);
            if (requestSchema.getName() == null) {
                requestSchema.setName(nameProvider.getOperationName(operation, path, method) +
                        "Input");
            }
            fieldBuilder.argument(GraphQLArgument.newArgument()
                    .name("body")
                    .description(requestBody.getDescription())
                    .type(graphQlTypeConverter.getGraphQlInputType(requestSchema))
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
}
