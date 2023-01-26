package dev.nikunjgupta;

import dev.nikunjgupta.provider.NameProvider;
import graphql.schema.GraphQLSchema;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class OpenApiToGraphQlSchema {
    public static GraphQLSchema generateGraphQlSchema(String openApiUri) {
        SwaggerParseResult result = new OpenAPIParser().readLocation(openApiUri, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        GraphQLSchema graphQLSchema = new OpenApiToGraphQlSchemaConverter(openAPI).generateSchema();
        reset();
        return graphQLSchema;
    }

    private static void reset() {
        NameProvider.getInstance().reset();
    }


    public static void main(String[] args) {
//        String url = "https://raw.githubusercontent.com/typicode/jsonplaceholder/31e6581ba012d27fd480b052b44001d09e21fdfa/public/swagger.json";
        String url = "https://petstore.swagger.io/v2/swagger.json";
//        String url = "https://raw.githubusercontent.com/logzio/public-api/master/alerts/swagger.json";
        GraphQLSchema graphQLSchema = generateGraphQlSchema(url);
    }

}
