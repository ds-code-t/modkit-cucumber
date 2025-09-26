package tools.ds.modkit.mappings;

import com.api.jsonata4java.expressions.Expressions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonataParentDemo {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Define JSON as a string
        String json = """
        {
          "root": {
            "child": {
              "grand": 123
            },
            "sibling": "hi"
          }
        }
        """;

        JsonNode root = mapper.readTree(json);

        System.out.println("JSON:");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        System.out.println();

        // Simple parent/root queries
        printQuery(root, "root.child.grand"); // direct
        printQuery(root, "root.child.{\"grand\": grand, \"fromParent\": %.sibling}"); // parent
        printQuery(root, "root.child.{\"fromRoot\": $.root.sibling}"); // root
        printQuery(root, "root.child.{\"parent\": %}"); // whole parent object
    }

    private static void printQuery(JsonNode input, String expr) throws Exception {
        Expressions e = Expressions.parse(expr);
        JsonNode out = e.evaluate(input);
        System.out.println(expr + " => " + out.toString());
    }
}
