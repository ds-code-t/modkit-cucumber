package tools.ds.modkit.mappings;

import com.api.jsonata4java.expressions.Expressions;
import com.api.jsonata4java.expressions.EvaluateException;
import com.api.jsonata4java.expressions.ParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static tools.ds.modkit.mappings.NodeMap.MAPPER;


public class QueryOperations {


    public static boolean setValue(ObjectNode root, String jsonataExpr, Object value) throws ParseException, IOException, EvaluateException {
        boolean valueSet = false;
        jsonataExpr = jsonataExpr.strip().replaceAll("^\\$\\.", "");
        String mainPath = jsonataExpr;
        boolean isArrayNode = jsonataExpr.matches(".*\\[\\s*(?:\\d+)?\\s*\\]$");//
        Integer arrayIndex = null;
        if (isArrayNode) {
            mainPath = jsonataExpr.substring(0, jsonataExpr.lastIndexOf("["));
            String arrayIndexString = jsonataExpr.substring(jsonataExpr.lastIndexOf("[") + 1, jsonataExpr.lastIndexOf("]"));
            if (!arrayIndexString.isBlank())
                arrayIndex = Integer.parseInt(arrayIndexString.strip());
        }

        int lastIndex = Math.max(Math.max(mainPath.lastIndexOf("["), mainPath.lastIndexOf(".")), 0);
        String fieldName = mainPath.substring(lastIndex).strip().replaceAll("[\\.'\"\\]\\[]", "");
        String parentPath = mainPath + "^";

        String normalized = jsonataExpr.replaceAll("\\s+", "");
// Remove allowed single-step brackets: [123], ['name'], ["name"]
        String s = normalized.replaceAll("\\[(\\d+|(['\"]).*?\\2)\\]", "");
// Remove wildcard navigation forms only (not arithmetic *): **, .*, [*]
        s = s.replaceAll("(\\*\\*|(?<=\\.)\\*|\\[\\*\\])", "");

// Direct iff no problematic brackets remain AND doesn't end with parent operator
        final boolean isDirect = !s.contains("[") && !s.endsWith("^");

        if (isDirect) {


            Expressions e = Expressions.parse(parentPath);
            JsonNode parent = e.evaluate(root);

            if (parent instanceof ObjectNode parentObject) {

                JsonNode child = parentObject.get(fieldName);

                if (child == null && isArrayNode) {
                    child = MAPPER.createArrayNode();
                    parentObject.set(fieldName, child);
                }

                if (child instanceof ArrayNode childArray) {

                    if (arrayIndex == null) {
                        childArray.add(MAPPER.valueToTree(value));
                        valueSet = true;
                    } else {
                        ensureIndex(childArray, arrayIndex, MAPPER.valueToTree(value));
                        valueSet = true;
                    }
                } else if (child == null || child instanceof ObjectNode) {
                    if (isArrayNode)
                        throw new RuntimeException("'" + fieldName + "' is not an Array but is trying to be set as an Array");
                    parentObject.set(fieldName,  MAPPER.valueToTree(value));
                    valueSet = true;
                }
            } else {
                throw new RuntimeException("parent object of '" + fieldName + "' must be an ObjectNode");
            }

        } else {

            List<Object> objects = evalToList(root, mainPath);
            List<Object> parentObjects = null;

            for (Object obj : objects) {
                if (obj instanceof ArrayNode childArray) {
                    if (arrayIndex == null) {
                        childArray.add(MAPPER.valueToTree(value));
                        valueSet = true;
                    } else {
                        ensureIndex(childArray, arrayIndex, MAPPER.valueToTree(value));
                        valueSet = true;
                    }
                } else {
                    if (parentObjects == null) parentObjects = evalToList(root, parentPath);
                    for (Object parentObj : parentObjects) {
                        if (parentObj instanceof ObjectNode parentObject) {
                            parentObject.set(fieldName, MAPPER.valueToTree(value));
                            valueSet = true;
                        }
                    }
                }

            }
        }
        return valueSet;
    }


    /**
     * Evaluate JSONata; always return a List. Prints parse/eval errors and returns [] on failure.
     */
    public static List<Object> evalToList(JsonNode input, String jsonataExpr) {
        try {
            Expressions e = Expressions.parse(jsonataExpr);
            JsonNode out = e.evaluate(input);
            if (out == null || out.isNull()) return List.of();
            if (out.isArray()) {
                List<Object> list = new ArrayList<>();
                out.forEach(list::add);
                return list;
            }
            return List.of(out);
        } catch (ParseException pe) {
            System.err.println("JSONata syntax error: " + pe.getMessage());
            return List.of();
        } catch (Exception ex) {
            System.err.println("JSONata evaluation error: " + ex.getMessage());
            return List.of();
        }
    }

    public static void ensureIndex(ArrayNode array, int index, JsonNode value) {
        if (array == null || index < 0) {
            throw new IllegalArgumentException("ArrayNode is null or index < 0");
        }
        while (array.size() <= index) {
            array.add(NullNode.instance);
        }
        array.set(index, value == null ? NullNode.instance : value);
    }
}