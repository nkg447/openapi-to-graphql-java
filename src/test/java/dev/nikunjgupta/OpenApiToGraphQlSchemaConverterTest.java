package dev.nikunjgupta;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

public class OpenApiToGraphQlSchemaConverterTest {

    private static final String basePath =
            OpenApiToGraphQlSchemaConverterTest.class.getClassLoader().getResource("").getFile();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testGenerateSchema() throws IOException {
        String[][] testScenarios = new String[][]{
                new String[]{"petstore.json", "petstore.graphql"},
                new String[]{"alerts.json", "alerts.graphql"},
                new String[]{"jsonplaceholder.json", "jsonplaceholder.graphql"}
        };
        for (String[] scenario : testScenarios) {
            String openapiSchemaPath = basePath + "openapi/schema/" + scenario[0];
            String graphqlSchemaPath = basePath + "graphql/schema/" + scenario[1];
            GraphQLSchema schema = OpenApiToGraphQlSchema.generateGraphQlSchema(openapiSchemaPath);
            SchemaPrinter schemaPrinter = new SchemaPrinter();
            String[] printedSchema = schemaPrinter.print(schema).split("\n");
            List<String> expectedSchema = Files.readAllLines(new File(graphqlSchemaPath).toPath());
            for (int i = 0; i < printedSchema.length; i++) {
                assertEquals("Did not match - " + scenario[0], expectedSchema.get(i).trim(),
                        printedSchema[i].trim());
            }

        }
    }
}