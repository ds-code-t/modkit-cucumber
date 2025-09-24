package tools.ds.modkit.mappings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.LinkedListMultimap;

import java.util.List;

public class TestNodeB {

    // ---------- tiny test helpers ----------
    private static void put(NodeB nm, String key, Object val) {
        System.out.printf("PUT  %-30s = %s%n", key, toOneLine(val));
        nm.put(key, val);
    }

    private static Object get(NodeB nm, String key) {
        System.out.printf("GET  %-30s%n", key);
        return nm.get(key);
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("── " + title + " ───────────────────────────────────────────");
    }

    private static void dumpRoot(NodeB nm) {
        System.out.println("ROOT: " + toOneLine(nm.objectNode()));
    }

    private static String toOneLine(Object o) {
        if (o == null) return "null";
        if (o instanceof ObjectNode on) return on.toString();
        return String.valueOf(o);
    }

    private static void expectEq(String label, Object expected, Object actual) {
        String e = toOneLine(expected);
        String a = toOneLine(actual);
        boolean pass = e.equals(a);
        System.out.println("Expected: " + e);
        System.out.println("Actual  : " + a + (pass ? "   ✅ PASS" : "   ❌ FAIL"));
    }

    // For convenience when the “actual” is the full root JSON
    private static void expectRoot(String label, NodeB nm, String expectedJson) {
        expectEq(label, expectedJson, nm.objectNode());
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        NodeB nm = new NodeB();

//        // 1) Top-level multimap behavior (append)
//        header("1) users top-level append (array append semantics)");
//        put(nm, "users", "Alice");
//        put(nm, "users", "Bob");
//        dumpRoot(nm);
//        expectRoot("users array", nm, "{\"users\":[\"Alice\",\"Bob\"]}");
//
//        // 2) Nested write under last top-level element (no explicit index)
//        //    Should coerce users[last] to an object (if needed) and write the field
//        header("2) users.name attaches to last element (coerce last to object)");
//        put(nm, "users.name", "Charlie");
//        dumpRoot(nm);
//        expectRoot("users with nested name", nm, "{\"users\":[\"Alice\",{\"name\":\"Charlie\"}]}");
//
//        // 3) Repeated write to the same FIELD => overwrite (no auto-promotion to array)
//        header("3) users[1].tags overwrites (no auto array promotion for fields)");
//        put(nm, "users[1].tags", "admin");
//        put(nm, "users[1].tags", "editor"); // overwrites prior value
//        dumpRoot(nm);
//        Object tags = get(nm, "$.users[1].tags");
//        expectEq("users[1].tags (overwrite)", List.of("editor"),
//                ((LinkedListMultimap<String, Object>) tags).values());
//
//        // 4) Explicit complete path with creation of missing parents (array padding with nulls)
//        header("4) $.accounts[2].id created (pad array with nulls, create object at index)");
//        put(nm, "$.accounts[2].id", 123);
//        dumpRoot(nm);
//        Object accountsId = get(nm, "$.accounts[2].id");
//        expectEq("$.accounts[2].id", List.of(123),
//                ((LinkedListMultimap<String, Object>) accountsId).values());
//
//        // 5) GET normalizes to last index for top-level keys (no explicit index)
//        header("5) get(logs) returns last element only for top-level key");
//        put(nm, "logs", "first");
//        put(nm, "logs", "second");
//        Object logsLast = get(nm, "logs"); // should point to $.logs[1]
//        expectEq("logs last", List.of("second"),
//                ((LinkedListMultimap<String, Object>) logsLast).values());
//
//        // 6) Explicit index overwrite vs. top-level append
//        header("6) numbers index overwrite then append");
//        put(nm, "$.numbers[0]", 42);  // write at index 0
//        put(nm, "$.numbers[0]", 43);  // overwrite at index 0
//        put(nm, "numbers", 44);       // top-level append -> [43, 44]
//        dumpRoot(nm);
//        // Note: tags remains a SCALAR ("editor") per (3), not an array.
//        expectRoot("numbers array & scalar tags stay", nm,
//                "{\"users\":[\"Alice\",{\"name\":\"Charlie\",\"tags\":\"editor\"}],"
//                        + "\"accounts\":[null,null,{\"id\":123}],"
//                        + "\"logs\":[\"first\",\"second\"],"
//                        + "\"numbers\":[43,44]}");
//
//        // 7) Construct from Guava LinkedListMultimap
//        header("7) Construct from Multimap");
//        LinkedListMultimap<String, String> mm = LinkedListMultimap.create();
//        mm.put("k1", "v1");
//        mm.put("k1", "v2");
//        NodeB nm2 = new NodeB(mm);
//        dumpRoot(nm2);
//        expectRoot("from multimap", nm2, "{\"k1\":[\"v1\",\"v2\"]}");
//
//        // 8) Merge another NodeB
//        header("8) Merge another NodeB (verify no unintended promotion)");
//        NodeB nm3 = new NodeB();
//        put(nm3, "extra", "hello");
//        System.out.println("MERGE nm3.objectNode() into nm");
//        nm.merge(nm3.objectNode());
//        dumpRoot(nm);
//        // tags is still scalar "editor"
//        expectRoot("after merge extra (tags still scalar)", nm,
//                "{\"users\":[\"Alice\",{\"name\":\"Charlie\",\"tags\":\"editor\"}],"
//                        + "\"accounts\":[null,null,{\"id\":123}],"
//                        + "\"logs\":[\"first\",\"second\"],"
//                        + "\"numbers\":[43,44],"
//                        + "\"extra\":[\"hello\"]}");
//
//        // 9) GET on missing top-level property -> empty multimap
//        header("9) get(missing) returns empty");
//        Object miss = get(nm, "missing");
//        expectEq("missing multimap empty", "[]",
//                ((LinkedListMultimap<String, Object>) miss).entries().toString());
//
//        // 10) Deep missing path with createIfMissing
//        header("10) deep nested creation (pad arrays, create objects)");
//        put(nm, "$.deep[0].child[1].leaf", "x");
//        dumpRoot(nm);
//        Object deepLeaf = get(nm, "$.deep[0].child[1].leaf");
//        expectEq("$.deep[0].child[1].leaf", List.of("x"),
//                ((LinkedListMultimap<String, Object>) deepLeaf).values());
//
//        // 11) Edge: overwrite a scalar field with a non-scalar value (object)
//        header("11) overwrite scalar with object (field overwrite semantics)");
//        put(nm, "users[1].tags", "{\"role\":\"admin\"}"); // becomes a string unless NodeB maps JSON -> tree
//        dumpRoot(nm);
//        // Since NodeB uses ObjectMapper.valueToTree, passing a raw JSON string keeps it as a string.
//        // Show intended overwrite by passing a map-like object instead:
//        nm.put("users[1].tags", java.util.Map.of("role","admin"));
//        Object overwritten = get(nm, "$.users[1].tags");
//        expectEq("tags overwritten with object", List.of("{\"role\":\"admin\"}"),
//                ((LinkedListMultimap<String, Object>) overwritten).values());
//
//        // 12) Edge: write to explicit array index inside a newly-created array deeper in tree
//        header("12) deep explicit index into new array");
//        put(nm, "$.config.sections[3].name", "Networking");
//        Object secName = get(nm, "$.config.sections[3].name");
//        expectEq("$.config.sections[3].name", List.of("Networking"),
//                ((LinkedListMultimap<String, Object>) secName).values());


        // 11) Edge: overwrite a scalar field with a non-scalar value (object)
        header("11) overwrite scalar with object (field overwrite semantics)");
        put(nm, "users[1].tags", "{\"role\":\"admin\"}"); // becomes a string unless NodeB maps JSON -> tree
        dumpRoot(nm);
        // Since NodeB uses ObjectMapper.valueToTree, passing a raw JSON string keeps it as a string.
        // Show intended overwrite by passing a map-like object instead:
        // Overwrite with an object
        nm.put("users[1].tags", java.util.Map.of("role","admin"));

        Object overwritten = get(nm, "$.users[1].tags");

// Expect a Map (LinkedHashMap equals Map.of by value)
        expectEq("tags overwritten with object",
                List.of(java.util.Map.of("role","admin")),
                ((LinkedListMultimap<String, Object>) overwritten).values());

// Bonus: prove nested query works
        Object role = get(nm, "$.users[1].tags.role");
        expectEq("tags.role", List.of("admin"),
                ((LinkedListMultimap<String, Object>) role).values());

        System.out.println("@@overwritten: "+  ((LinkedListMultimap<String, Object>) overwritten).values().getFirst().getClass());
        System.out.println();
        System.out.println("All checks done.");
    }
}
