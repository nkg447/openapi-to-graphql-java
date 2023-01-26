package dev.nikunjgupta;

import dev.nikunjgupta.converter.GraphQlTypeConverter;
import dev.nikunjgupta.provider.NameProvider;
import dev.nikunjgupta.provider.SchemaProvider;
import graphql.schema.*;
import io.swagger.models.HttpMethod;
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

    /**
     * C'tor
     */
    public OpenApiToGraphQlSchemaConverter(OpenAPI openAPI) {
        this.openAPI = openAPI;
        this.schemaProvider = SchemaProvider.getOrCreateSchemaProvider(openAPI);
        this.nameProvider = NameProvider.getInstance();
        this.graphQlTypeConverter = new GraphQlTypeConverter(openAPI, schemaProvider,
                nameProvider);
    }

    /**
     * Generate GraphQlSchema
     *
     * @return GraphQlSchema object
     */
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
            if ((pathItem.getGet() != null && (operation =
                    pathItem.getGet()) != null)) {
                GraphQLFieldDefinition.Builder fieldBuilder =
                        createGraphQLFieldDefinition(operation,
                                entry.getKey(), HttpMethod.GET);
                if (fieldBuilder == null) {
                    System.out.println("GET " + entry.getKey() + " path could not be " +
                            "converted to graphQl");
                } else {
                    queryTypeBuilder = queryTypeBuilder
                            .field(fieldBuilder);
                }
            }
            if ((pathItem.getPost() != null && (operation =
                    pathItem.getPost()) != null)) {
                mutationAdded = addOperationToMutation(mutationTypeBuilder,
                        operation, HttpMethod.POST, entry.getKey()) || mutationAdded;
            }
            if ((pathItem.getPut() != null && (operation =
                    pathItem.getPut()) != null)) {
                mutationAdded = addOperationToMutation(mutationTypeBuilder,
                        operation, HttpMethod.PUT, entry.getKey()) || mutationAdded;
            }
            if ((pathItem.getPatch() != null && (operation =
                    pathItem.getPatch()) != null)) {
                mutationAdded = addOperationToMutation(mutationTypeBuilder,
                        operation, HttpMethod.PATCH, entry.getKey()) || mutationAdded;
            }
        }

        GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema().query(queryTypeBuilder);

        if (mutationAdded)
            schemaBuilder.mutation(mutationTypeBuilder);

        return schemaBuilder.build();
    }

    /**
     * creates and adds a fieldDefinition to mutationTypeBuilder
     *
     * @param mutationTypeBuilder GraphQLObjectType.Builder for mutation
     * @param operation           operation object from OpenApi
     * @param method              http method
     * @param path                endpoint path
     * @return true if successfully added a fieldDefinition to mutationTypeBuilder
     */
    private boolean addOperationToMutation(GraphQLObjectType.Builder mutationTypeBuilder,
                                           Operation operation, HttpMethod method, String path) {
        GraphQLFieldDefinition.Builder fieldBuilder = createGraphQLFieldDefinition(operation,
                path, method);
        if (fieldBuilder == null) {
            System.out.println(method + " " + path + " path could not be converted to graphQl");
            return false;
        }
        mutationTypeBuilder
                .field(fieldBuilder);
        return true;
    }

    /**
     * Create a graphql field from an openApi Operation object
     *
     * @param operation operation object from OpenApi
     * @param path      endpoint path
     * @param method    http method
     * @return GraphQLFieldDefinition.Builder
     */
    private GraphQLFieldDefinition.Builder createGraphQLFieldDefinition(Operation operation,
                                                                        String path,
                                                                        HttpMethod method) {
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

    /**
     * @return Schema object from a RequestBody of an Operation of openApi
     */
    private Schema getRequestSchema(RequestBody requestBody) {
        // TODO: union of 2 request type is possible - handle that
        Content requestContent = requestBody.getContent();
        if (requestContent.size() == 0)
            return null;
        return requestContent.entrySet().stream().findFirst()
                .get().getValue().getSchema();
    }

    /**
     * @return Schema object Response of an Operation of openApi
     */
    private Schema getResponseSchema(Operation operation) {
        // fetched the responseSchema
        // TODO: better logic
        // TODO: union of 2 response is possible - handle that
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
