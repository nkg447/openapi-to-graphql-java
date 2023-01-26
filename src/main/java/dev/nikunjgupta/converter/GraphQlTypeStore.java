package dev.nikunjgupta.converter;

import dev.nikunjgupta.provider.NameProvider;
import graphql.schema.*;
import graphql.schema.idl.SchemaPrinter;

import java.util.HashMap;
import java.util.List;

/**
 * Custom implementation of HashMap where we want to add same GraphQLType value for
 * a key if similar is already present in the map
 * @param <T>
 * @param <U>
 */
public class GraphQlTypeStore<T, U extends GraphQLType> extends HashMap<T, U> {

    private final NameProvider nameProvider = NameProvider.getInstance();
    private final SchemaPrinter schemaPrinter = new SchemaPrinter();

    @Override
    public U put(T key, U value) {
        // check if store already has this value
        // if already there then put existing value
        U existingValue = getExistingValueIfExist(value);
        if (existingValue != null) {
            value = existingValue;
        }
        return super.put(key, value);
    }

    /**
     * Look for a similar value in the existing set of values
     * @param value value to look for
     * @return the similar existing value, if found, else null
     */
    private U getExistingValueIfExist(U value) {
        if (value instanceof GraphQLNamedSchemaElement) {
            GraphQLNamedSchemaElement namedSchemaElement = (GraphQLNamedSchemaElement) value;
            String name = namedSchemaElement.getName();
            // check for similarly among all existing values
            for (U entry : values()) {
                if (entry instanceof GraphQLNamedSchemaElement) {
                    GraphQLNamedSchemaElement entryValue =
                            (GraphQLNamedSchemaElement) entry;
                    // look for a common name for entry and value
                    // eg common name for "abc1" and "abc2" would be "abc"
                    String originName = nameProvider.getCommonOriginName(name,
                            entryValue.getName());
                    if (originName != null // found a common name
                            && isEqualGraphQLNamedSchemaElement(namedSchemaElement, entryValue)) { // elements are similar
                        nameProvider.freeName(originName);
                        return entry;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param element1 first element
     * @param element2 second element
     * @return true if elements are similar
     */
    private boolean isEqualGraphQLNamedSchemaElement(GraphQLNamedSchemaElement element1,
                                                     GraphQLNamedSchemaElement element2) {
        if (element1.getClass().equals(element2.getClass())) {
            if (element1 instanceof GraphQLEnumType) {
                List<GraphQLEnumValueDefinition> definitionList1 =
                        ((GraphQLEnumType) element1).getValues();
                List<GraphQLEnumValueDefinition> definitionList2 =
                        ((GraphQLEnumType) element2).getValues();
                if (definitionList1.size() == definitionList2.size()) {
                    for (int i = 0; i < definitionList1.size(); i++) {
                        if (!isEqualGraphQLEnumValueDefinition(definitionList1.get(i),
                                definitionList2.get(i))) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            if (element1 instanceof GraphQLFieldsContainer) {
                List<GraphQLFieldDefinition> definitionList1 =
                        ((GraphQLFieldsContainer) element1).getFieldDefinitions();
                List<GraphQLFieldDefinition> definitionList2 =
                        ((GraphQLFieldsContainer) element2).getFieldDefinitions();
                if (definitionList1.size() == definitionList2.size()) {
                    for (int i = 0; i < definitionList1.size(); i++) {
                        if (!isEqualGraphQLFieldDefinition(definitionList1.get(i),
                                definitionList2.get(i))) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param definition1 first enum value definition
     * @param definition2 second enum value definition
     * @return true if both definitions are similar
     */
    private boolean isEqualGraphQLEnumValueDefinition(GraphQLEnumValueDefinition definition1,
                                                      GraphQLEnumValueDefinition definition2) {
        if (!definition1.getName().equals(definition2.getName()))
            return false;
        return definition1.getValue().equals(definition2.getValue());
    }

    /**
     * @param definition1 first field definition
     * @param definition2 second field definition
     * @return true if both definitions are similar
     */
    private boolean isEqualGraphQLFieldDefinition(GraphQLFieldDefinition definition1,
                                                  GraphQLFieldDefinition definition2) {
        if (!definition1.getName().equals(definition2.getName()))
            return false;
        return GraphQLTypeUtil.simplePrint(definition1.getType()).equals(GraphQLTypeUtil.simplePrint(definition2.getType()));
    }

}
