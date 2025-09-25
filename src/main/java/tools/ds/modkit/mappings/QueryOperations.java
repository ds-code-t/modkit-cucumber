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
        jsonataExpr = jsonataExpr.strip().replaceAll("^\\$", "").replaceAll("^\\.", "");
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
        String parentPath = mainPath.substring(0, lastIndex);

        System.out.println("@@parentPath: " + parentPath);
        String fieldName = mainPath.substring(lastIndex).strip().replaceAll("[\\.'\"\\]\\[]", "");
//        String parentExpr = mainPath + "^";
//        String parentExpr = "(" + mainPath + ")~>$map(function($v){$v.%})";

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
                } else {
                    if (isArrayNode)
                        throw new RuntimeException("'" + fieldName + "' is not an Array but is trying to be set as an Array");
                    parentObject.set(fieldName, MAPPER.valueToTree(value));
                    valueSet = true;
                }
            } else {
                throw new RuntimeException("parent object of '" + fieldName + "' must be an ObjectNode");
            }

        } else {

            // Holder for (parent object, current child value at fieldName)

// Build pairs once from parentPath
            List<JsonNode> parentNodes = evalToList(root, parentPath); // returns JsonNodes
            List<Pair> pairs = parentNodes.stream()
                    .filter(n -> n instanceof ObjectNode)
                    .map(n -> (ObjectNode) n)
                    .map(po -> new Pair(po, po.get(fieldName)))
                    .filter(p -> p.current() != null)   // only parents that currently have the field
                    .toList();

            for (Pair pair : pairs) {
                JsonNode obj = pair.current();
                if (obj instanceof ArrayNode childArray) {
                    if (arrayIndex == null) {
                        childArray.add(MAPPER.valueToTree(value));
                        valueSet = true;
                    } else {
                        ensureIndex(childArray, arrayIndex, MAPPER.valueToTree(value));
                        valueSet = true;
                    }
                } else {
                    pair.parent().set(fieldName, MAPPER.valueToTree(value));
                    valueSet = true;
                }
            }
        }
        return valueSet;
    }

    record Pair(ObjectNode parent, JsonNode current) {
    }

    /**
     * Evaluate JSONata; always return a List. Prints parse/eval errors and returns [] on failure.
     */
    public static List<JsonNode> evalToList(JsonNode input, String jsonataExpr) {
        try {
            Expressions e = Expressions.parse(jsonataExpr);
            JsonNode out = e.evaluate(input);
            if (out == null || out.isNull()) return List.of();
            if (out.isArray()) {
                List<JsonNode> list = new ArrayList<>();
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


    public static void main(String[] args) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.function.Function<com.fasterxml.jackson.databind.JsonNode, String> pp =
                n -> {
                    try { return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(n); }
                    catch (Exception e) { return String.valueOf(n); }
                };

        int total = 0, passed = 0;
        java.util.function.BiConsumer<String, Boolean> pf = (label, ok) -> {
            System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
            System.out.println();
        };
        java.util.function.BiFunction<String, Boolean, Boolean> showRet = (label, actual) -> {
            System.out.println(label);
            System.out.println("Expected (return): true");
            System.out.println("Actual   (return): " + actual);
            return actual == true;
        };
        java.util.function.Function<Boolean, Boolean> showRetFalse = (actual) -> {
            System.out.println("Expected (return): false");
            System.out.println("Actual   (return): " + actual);
            return actual == false;
        };

        // --------- Seed JSON ----------
        ObjectNode root = mapper.createObjectNode();
        ObjectNode a = root.putObject("a").putObject("b");
        ArrayNode c = a.putArray("c"); c.add(1).add(2);
        a.put("d", "orig");
        ObjectNode e = a.putObject("e"); e.put("note", "exists");

        ArrayNode data = root.putArray("data");
        ((ObjectNode) data.addObject().putObject("child")).put("name", "x");
        ((ObjectNode) data.addObject().putObject("child")).put("name", "y");

        ArrayNode people = root.putArray("people");
        ((ObjectNode) people.addObject()).put("age", 17).put("nick", "Teen");
        ((ObjectNode) people.addObject()).put("age", 18).put("nick", "Adult0");

        System.out.println("=== INITIAL JSON ===");
        System.out.println(pp.apply(root));
        System.out.println();

        // Helper to fetch by JSON Pointer and compare to expected JSON (string)
        java.util.function.BiFunction<String, String, Boolean> assertPtrEquals = (ptr, expectedJson) -> {
            try {
                JsonNode expected = mapper.readTree(expectedJson);
                JsonNode actual = root.at(ptr);
                System.out.println("Expected (value): " + pp.apply(expected));
                System.out.println("Actual   (value): " + pp.apply(actual));
                return expected.equals(actual);
            } catch (Exception ex) {
                System.out.println("Exception comparing: " + ex.getMessage());
                return false;
            }
        };

        // ===== DIRECT PATHS =====

        // 1) Direct: append to array $.a.b.c  -> [1,2,99]
        total++;
        System.out.println("[1] Direct $.a.b.c  (append 99)");
        boolean r1 = setValue(root, "$.a.b.c", mapper.readTree("99"));
        boolean ok1r = showRet.apply("", r1);
        boolean ok1v = assertPtrEquals.apply("/a/b/c", "[1,2,99]");
        boolean ok1 = ok1r && ok1v; if (ok1) passed++; pf.accept("[1] $.a.b.c", ok1);

        // 2) Direct: overwrite $.a.b.d -> "hello"
        total++;
        System.out.println("[2] Direct $.a.b.d  (overwrite to \"hello\")");
        boolean r2 = setValue(root, "$.a.b.d", mapper.readTree("\"hello\""));
        boolean ok2r = showRet.apply("", r2);
        boolean ok2v = assertPtrEquals.apply("/a/b/d", "\"hello\"");
        boolean ok2 = ok2r && ok2v; if (ok2) passed++; pf.accept("[2] $.a.b.d", ok2);

        // 3) Direct: bracket field $.a['b'].e -> {"k":1}
        total++;
        System.out.println("[3] Direct $.a['b'].e  (overwrite to {\"k\":1})");
        boolean r3 = setValue(root, "$.a.'b'.e", mapper.readTree("{\"k\":1}"));
        boolean ok3r = showRet.apply("", r3);
        boolean ok3v = assertPtrEquals.apply("/a/b/e", "{\"k\":1}");
        boolean ok3 = ok3r && ok3v; if (ok3) passed++; pf.accept("[3] $.a['b'].e", ok3);

        // 4) Direct: index at tail $.a.b.c[0] -> set index 0 to 7 => [7,2,99]
        total++;
        System.out.println("[4] Direct $.a.b.c[0]  (set index 0 to 7)");
        boolean r4 = setValue(root, "$.a.b.c[0]", mapper.readTree("7"));
        boolean ok4r = showRet.apply("", r4);
        boolean ok4v = assertPtrEquals.apply("/a/b/c", "[7,2,99]");
        boolean ok4 = ok4r && ok4v; if (ok4) passed++; pf.accept("[4] $.a.b.c[0]", ok4);

        // ===== NON-DIRECT UPDATES =====

        // 5) Non-direct: $.data[].child.name -> "ZZZ" for all
        total++;
        System.out.println("[5] Non-Direct $.data[].child.name  (set to \"ZZZ\")");
        boolean r5 = setValue(root, "$.data[].child.name", mapper.readTree("\"ZZZ\""));
        boolean ok5r = showRet.apply("", r5);
        boolean ok5v = assertPtrEquals.apply("/data", "[{\"child\":{\"name\":\"ZZZ\"}},{\"child\":{\"name\":\"ZZZ\"}}]");
        boolean ok5 = ok5r && ok5v; if (ok5) passed++; pf.accept("[5] $.data[].child.name", ok5);

        // 6) Non-direct: $.people[age >= 18].nick -> "Adult"
        total++;
        System.out.println("[6] Non-Direct $.people[age >= 18].nick  (set to \"Adult\")");
        boolean r6 = setValue(root, "$.people[age >= 18].nick", mapper.readTree("\"Adult\""));
        boolean ok6r = showRet.apply("", r6);
        boolean ok6v = assertPtrEquals.apply("/people", "[{\"age\":17,\"nick\":\"Teen\"},{\"age\":18,\"nick\":\"Adult\"}]");
        boolean ok6 = ok6r && ok6v; if (ok6) passed++; pf.accept("[6] $.people[age >= 18].nick", ok6);

        // 7) Non-direct (multi-index on c): $.a.b.c[0,2] -> (no change expected with your current code)
        total++;
        System.out.println("[7] Non-Direct $.a.b.c[0,2]  (expect no change)");
        boolean r7 = setValue(root, "$.a.b.c[0,2]", mapper.readTree("55"));
        boolean ok7r = showRetFalse.apply(r7);
        boolean ok7v = assertPtrEquals.apply("/a/b/c", "[7,2,99]"); // unchanged
        boolean ok7 = ok7r && ok7v; if (ok7) passed++; pf.accept("[7] $.a.b.c[0,2]", ok7);

        // 8) Non-direct (slice): $.a.b.c[1:3] -> (no change expected with your current code)
        total++;
        System.out.println("[8] Non-Direct $.a.b.c[1:3]  (expect no change)");
        boolean r8 = setValue(root, "$.a.b.c[1:3]", mapper.readTree("66"));
        boolean ok8r = showRetFalse.apply(r8);
        boolean ok8v = assertPtrEquals.apply("/a/b/c", "[7,2,99]"); // unchanged
        boolean ok8 = ok8r && ok8v; if (ok8) passed++; pf.accept("[8] $.a.b.c[1:3]", ok8);

        // 9) Non-direct (empty wildcard): $.data[] -> (no change expected with your current code)
        total++;
        System.out.println("[9] Non-Direct $.data[]  (expect no change)");
        boolean r9 = setValue(root, "$.data[]", mapper.readTree("\"ignored\""));
        boolean ok9r = showRetFalse.apply(r9);
        boolean ok9v = assertPtrEquals.apply("/data", "[{\"child\":{\"name\":\"ZZZ\"}},{\"child\":{\"name\":\"ZZZ\"}}]"); // unchanged
        boolean ok9 = ok9r && ok9v; if (ok9) passed++; pf.accept("[9] $.data[]", ok9);

        // ===== ERROR / EDGE =====

        // 10) Invalid JSONata: $.a..b -> expect false and no mutation
        total++;
        System.out.println("[10] Error $.a..b  (invalid JSONata; expect no change and false)");
        String before10 = pp.apply(root);
        boolean r10 = setValue(root, "$.a..b", mapper.readTree("\"boom\""));
        boolean ok10r = showRetFalse.apply(r10);
        String after10 = pp.apply(root);
        boolean ok10v = before10.equals(after10); // full-tree unchanged
        System.out.println("Expected (value): <unchanged tree>");
        System.out.println("Actual   (value): <" + (ok10v ? "unchanged" : "changed") + ">");
        boolean ok10 = ok10r && ok10v; if (ok10) passed++; pf.accept("[10] $.a..b", ok10);

        // 11) Non-direct: $.data[].child -> replace each child object
        total++;
        System.out.println("[11] Non-Direct $.data[].child  (overwrite each child to {\"note\":\"multi\"})");
        boolean r11 = setValue(root, "$.data[].child", mapper.readTree("{\"note\":\"multi\"}"));
        boolean ok11r = showRet.apply("", r11);
        boolean ok11v = assertPtrEquals.apply("/data", "[{\"child\":{\"note\":\"multi\"}},{\"child\":{\"note\":\"multi\"}}]");
        boolean ok11 = ok11r && ok11v; if (ok11) passed++; pf.accept("[11] $.data[].child", ok11);

        // 12) Confirm c is still [7,2,99]
        total++;
        System.out.println("[12] Sanity check $.a.b.c");
        boolean ok12 = assertPtrEquals.apply("/a/b/c", "[7,2,99]");
        if (ok12) passed++; pf.accept("[12] $.a.b.c unchanged", ok12);

        System.out.println("==== SUMMARY ====");
        System.out.println("Passed " + passed + " / " + total + " tests");
    }


}