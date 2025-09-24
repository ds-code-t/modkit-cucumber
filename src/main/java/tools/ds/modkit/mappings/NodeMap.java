package tools.ds.modkit.mappings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.jayway.jsonpath.*;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JsonSmartJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import java.util.*;
import java.util.regex.Pattern;

public class NodeMap {

    // ---- DEBUG --------------------------------------------------------------
    private static final boolean DEBUG = true;
    private static void dbg(String fmt, Object... args) {
        if (DEBUG) System.out.println("[NodeMapDBG] " + String.format(fmt, args));
    }
    private static String nodeType(JsonNode n) {
        if (n == null) return "null";
        if (n.isObject()) return "ObjectNode";
        if (n.isArray()) return "ArrayNode";
        if (n.isNull()) return "NullNode";
        if (n.isTextual()) return "TextNode";
        if (n.isNumber()) return "NumericNode";
        if (n.isBoolean()) return "BooleanNode";
        return n.getClass().getSimpleName();
    }

    // ---- TypeRefs ----
    private static final TypeRef<List<Object>> LIST_OF_OBJECTS = new TypeRef<>() {};

    // ---- ObjectMapper ----
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        dbg("ObjectMapper init: registering GuavaModule");
        MAPPER.registerModule(new GuavaModule());
        // default typing intentionally NOT enabled (to avoid mapping exceptions)
    }

    // ---- JsonPath Configs ----
    // Paths: plain Java types (no Jackson mapping)
    private static final Configuration PATHS_CFG = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider(MAPPER)) // <-- use Jackson provider
            // IMPORTANT: no mappingProvider here (so we get a plain List<String>)
            .options(Option.ALWAYS_RETURN_LIST, Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS)
            .build();


    // Values: Jackson provider (so values come back as JsonNode/POJO via ObjectMapper)
    private static final Configuration VALUES_CFG = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider(MAPPER))
            .mappingProvider(new JacksonMappingProvider(MAPPER))
            .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
            .build();

    // Simple complete path: "$.a.b[2].c" (no wildcards, filters, or recursive)
    private static final Pattern SIMPLE_PATH = Pattern.compile(
            "^\\$\\.(?:[^.\\[\\]*?()@!]+(?:\\[\\d+])?)(?:\\.(?:[^.\\[\\]*?()@!]+(?:\\[\\d+])?))*$"
    );

    // ---- State ----
    private final ObjectNode root;

    // ---- Ctors ----
    public NodeMap() {
        dbg("Ctor: NodeMap()");
        this.root = MAPPER.createObjectNode();
        dbg("Root created: {}", root);
    }

    public NodeMap(Map<?, ?> map) {
        dbg("Ctor: NodeMap(Map) -> {}", map);
        this.root = toObjectNode(map);
        dbg("Root from map: {}", root);
    }

    public NodeMap(LinkedListMultimap<?, ?> multimap) {
        dbg("Ctor: NodeMap(LinkedListMultimap) -> {}", multimap);
        this.root = toObjectNode(multimap);
        dbg("Root from multimap: {}", root);
    }

    public ObjectNode objectNode() {
        dbg("objectNode() -> {}", root);
        return root;
    }

    // ---- Key preprocessing ----
    private static String requireDollarDot(String key) {
        dbg("requireDollarDot(key=%s)", key);
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key cannot be null or blank");
        String out = (key.startsWith("$.") ? key : "$." + key).strip();
        dbg("requireDollarDot -> %s", out);
        return out;
    }

    private static String getKeyPreprocess(String k) {
        dbg("getKeyPreprocess(%s)", k);
        String out = requireDollarDot(k);
        dbg("getKeyPreprocess -> %s", out);
        return out;
    }

    private static String putKeyPreprocess(String k) {
        dbg("putKeyPreprocess(%s)", k);
        String out = requireDollarDot(k);
        dbg("putKeyPreprocess -> %s", out);
        return out;
    }

    public List<Object> getValues(String key) {
        dbg("getValues(key=%s)", key);
        List<Object> out = get(key).values();
        dbg("getValues -> %s", out);
        return out;
    }

    // ---- GET: returns LinkedListMultimap<fullPath, value> ----
    public LinkedListMultimap<String, Object> get(String key) {
        dbg("GET(key=%s)", key);
        String q = key != null && key.startsWith("$.") ? key : getKeyPreprocess(key);
        dbg("GET: initial query -> %s", q);

        // For top-level properties with no explicit index: point to the LAST element if array exists.
        q = normalizeTopLevelKeyForGet(q);
        dbg("GET: normalized-for-get query -> %s", q);
        if (q == null) {
            dbg("GET: normalized query is null (top-level array missing or empty). Returning empty multimap.");
            return LinkedListMultimap.create();
        }

        // paths
        dbg("GET: reading paths with PATHS_CFG...");
        Object rawPaths = JsonPath.using(PATHS_CFG).parse(root).read(q);
        dbg("GET: rawPaths class = %s, value = %s", (rawPaths == null ? "null" : rawPaths.getClass()), rawPaths);

        @SuppressWarnings("unchecked")
        List<String> paths = (List<String>) rawPaths;
        if (paths == null) paths = List.of();
        dbg("GET: paths -> %s", paths);

        // values
        dbg("GET: reading values with VALUES_CFG...");
        List<Object> values = JsonPath.using(VALUES_CFG).parse(root).read(q, LIST_OF_OBJECTS);
        dbg("GET: values (raw) -> %s", values);
        if (values == null) values = List.of();

        LinkedListMultimap<String, Object> out = LinkedListMultimap.create();
        int n = Math.min(paths.size(), values.size());
        dbg("GET: zipping %d entries", n);
        for (int i = 0; i < n; i++) {
            dbg("GET: put(%s -> %s)", paths.get(i), values.get(i));
            out.put(paths.get(i), values.get(i));
        }
        dbg("GET: done. multimap size=%d", out.size());
        return out;
    }

    // ---- PUT ----
    public void put(String key, Object value) {
        dbg("PUT(key=%s, value=%s)", key, value);
        String q = putKeyPreprocess(key);
        dbg("PUT: initial query -> %s", q);

        // Convert the value to a JsonNode (handles Maps, POJOs, Collections, Guava Multimap)
        JsonNode valueNode = toJsonNode(value);
        boolean valueIsArrayLike = isArrayLike(value) || (valueNode != null && valueNode.isArray());
        dbg("PUT: valueNode=%s (%s), valueIsArrayLike=%s", valueNode, nodeType(valueNode), valueIsArrayLike);

        // Top-level multimap semantics:
        //  - If assigning nested props under a top-level property with no explicit index,
        //    insert [lastIndex] AND ensure that element is an ObjectNode (coerce if needed).
        q = normalizeTopLevelKey(q);
        dbg("PUT: normalized-for-put query -> %s", q);

        // Find all matches as paths (strings)
        dbg("PUT: reading matching paths with PATHS_CFG...");
        Object rawPaths = JsonPath.using(PATHS_CFG).parse(root).read(q);
        dbg("PUT: rawPaths class = %s, value = %s", (rawPaths == null ? "null" : rawPaths.getClass()), rawPaths);

        @SuppressWarnings("unchecked")
        List<String> paths = (List<String>) rawPaths;
        boolean hadMatches = paths != null && !paths.isEmpty();
        dbg("PUT: hadMatches=%s, paths=%s", hadMatches, paths);

        if (hadMatches) {
            for (String p : paths) {
                dbg("PUT: applyAtPath (matched) -> %s", p);
                applyAtPath(p, valueNode, valueIsArrayLike, false);
            }
            dbg("PUT: done with matched paths.");
            return;
        }

        // If no matches and it's a simple complete path, create the missing structure and set.
        boolean simple = isSimpleCompletePath(q);
        dbg("PUT: no matches; isSimpleCompletePath(%s) -> %s", q, simple);
        if (simple) {
            dbg("PUT: applyAtPath (createIfMissing=true) -> %s", q);
            applyAtPath(q, valueNode, valueIsArrayLike, true);
        } else {
            dbg("PUT: no-op (not a simple complete path and no matches)");
        }
    }

    // ---- Normalize keys (GET/PUT) for top-level array semantics ----

    // GET version: do NOT create; just point to last index if array exists; else return null.
    private String normalizeTopLevelKeyForGet(String q) {
        dbg("normalizeTopLevelKeyForGet(q=%s)", q);
        if (!q.startsWith("$.")) {
            dbg("normalizeTopLevelKeyForGet: q does not start with '$.'; returning unchanged.");
            return q;
        }

        String withoutRoot = q.substring(2);
        int dot = withoutRoot.indexOf('.');
        int bracket = withoutRoot.indexOf('[');
        int end = (dot == -1 ? withoutRoot.length() : dot);
        if (bracket != -1 && bracket < end) end = bracket;

        String topField = withoutRoot.substring(0, end);
        dbg("normalizeTopLevelKeyForGet: topField=%s, dot=%d, bracket=%d, end=%d", topField, dot, bracket, end);

        JsonNode existing = root.get(topField);
        dbg("normalizeTopLevelKeyForGet: existing=%s (%s)", existing, nodeType(existing));
        if (existing == null || !existing.isArray()) {
            dbg("normalizeTopLevelKeyForGet: top-level is missing or not array -> return null");
            return null;
        }

        ArrayNode arr = (ArrayNode) existing;
        dbg("normalizeTopLevelKeyForGet: top-level array size=%d", arr.size());
        if (arr.size() == 0) {
            dbg("normalizeTopLevelKeyForGet: array empty -> return null");
            return null;
        }

        boolean hasNested = dot != -1;
        boolean hasExplicitIndex = (bracket != -1 && bracket < end + 1);
        dbg("normalizeTopLevelKeyForGet: hasNested=%s, hasExplicitIndex=%s", hasNested, hasExplicitIndex);

        if (!hasExplicitIndex) {
            int lastIdx = arr.size() - 1;
            String out = hasNested
                    ? "$." + topField + "[" + lastIdx + "]" + withoutRoot.substring(end)
                    : "$." + topField + "[" + lastIdx + "]";
            dbg("normalizeTopLevelKeyForGet: rewrite -> %s", out);
            return out;
        }
        dbg("normalizeTopLevelKeyForGet: explicit index present -> unchanged");
        return q;
    }

    // PUT version: CREATE array if needed; if writing nested (no explicit index), ensure last element is an ObjectNode.
    private String normalizeTopLevelKey(String q) {
        dbg("normalizeTopLevelKey(q=%s)", q);
        if (!q.startsWith("$.")) {
            dbg("normalizeTopLevelKey: q does not start with '$.'; returning unchanged.");
            return q;
        }
        String withoutRoot = q.substring(2);

        int dot = withoutRoot.indexOf('.');
        int bracket = withoutRoot.indexOf('[');
        int end = (dot == -1 ? withoutRoot.length() : dot);
        if (bracket != -1 && bracket < end) end = bracket;

        String topField = withoutRoot.substring(0, end);
        dbg("normalizeTopLevelKey: topField=%s, dot=%d, bracket=%d, end=%d", topField, dot, bracket, end);

        // Ensure top-level array
        int size = ensureTopLevelArray(topField);
        dbg("normalizeTopLevelKey: ensured top-level array '%s' size=%d", topField, size);

        boolean hasNested = (dot != -1);
        boolean hasExplicitIndex = (bracket != -1 && bracket < end + 1);
        dbg("normalizeTopLevelKey: hasNested=%s, hasExplicitIndex=%s", hasNested, hasExplicitIndex);

        if (!hasExplicitIndex) {
            ArrayNode arr = (ArrayNode) root.get(topField);
            if (hasNested) {
                // We will write into the last element's nested path.
                int lastIdx = Math.max(size - 1, 0);
                dbg("normalizeTopLevelKey: nested write; lastIdx initially=%d", lastIdx);
                // If array empty, add a new object at [0].
                if (arr.size() == 0) {
                    dbg("normalizeTopLevelKey: array empty -> add new ObjectNode at [0]");
                    arr.add(MAPPER.createObjectNode());
                    lastIdx = 0;
                } else if (!arr.get(lastIdx).isObject()) {
                    dbg("normalizeTopLevelKey: last element not object -> coerce to ObjectNode");
                    arr.set(lastIdx, MAPPER.createObjectNode());
                }
                String out = "$." + topField + "[" + lastIdx + "]" + withoutRoot.substring(end);
                dbg("normalizeTopLevelKey: rewrite -> %s", out);
                return out;
            } else {
                // Just top-level assignment → append as a new element
                String out = "$." + topField + "[" + size + "]";
                dbg("normalizeTopLevelKey: top-level append -> %s", out);
                return out;
            }
        }
        dbg("normalizeTopLevelKey: explicit index present -> unchanged");
        return q;
    }

    private int ensureTopLevelArray(String fieldName) {
        dbg("ensureTopLevelArray(fieldName=%s)", fieldName);
        JsonNode existing = root.get(fieldName);
        dbg("ensureTopLevelArray: existing=%s (%s)", existing, nodeType(existing));
        if (existing == null || existing.isNull()) {
            ArrayNode arr = MAPPER.createArrayNode();
            root.set(fieldName, arr);
            dbg("ensureTopLevelArray: created new [] at top-level for '%s'", fieldName);
            return 0;
        }
        if (existing.isArray()) {
            int sz = ((ArrayNode) existing).size();
            dbg("ensureTopLevelArray: already an array size=%d", sz);
            return sz;
        }

        ArrayNode arr = MAPPER.createArrayNode();
        arr.add(existing);
        root.set(fieldName, arr);
        dbg("ensureTopLevelArray: wrapped existing non-array into array; size now=%d", arr.size());
        return arr.size();
    }

    // ---- Apply write at a concrete path ----
    private void applyAtPath(String fullPath, JsonNode newValue, boolean valueIsArrayLike, boolean createIfMissing) {
        dbg("applyAtPath(fullPath=%s, newValue=%s (%s), arrayLike=%s, createIfMissing=%s)",
                fullPath, newValue, nodeType(newValue), valueIsArrayLike, createIfMissing);

        PathTail tail = splitParent(fullPath);
        dbg("applyAtPath: splitParent -> parentPath=%s, lastIsIndex=%s, lastKey=%s, lastIndex=%d",
                tail.parentPath, tail.lastIsIndex, tail.lastKey, tail.lastIndex);

        ContainerAndKey parent = resolveContainer(tail.parentPath, createIfMissing);
        dbg("applyAtPath: resolveContainer -> container=%s (%s), keyOrIndex='%s'",
                parent == null ? null : parent.container,
                parent == null ? "null" : nodeType(parent.container),
                parent == null ? null : parent.keyOrIndex);
        if (parent == null) {
            dbg("applyAtPath: parent is null -> abort");
            return;
        }

        if (tail.lastIsIndex) {
            dbg("applyAtPath: last segment is [index]=%d", tail.lastIndex);
            ArrayNode arr = asArray(parent.container, parent.keyOrIndex, createIfMissing);
            dbg("applyAtPath: asArray -> %s (%s)", arr, (arr == null ? "null" : "ArrayNode"));
            if (arr == null) {
                dbg("applyAtPath: asArray returned null -> abort");
                return;
            }

            int idx = tail.lastIndex;
            if (idx < 0) {
                dbg("applyAtPath: negative index -> abort");
                return;
            }

            dbg("applyAtPath: ensureArraySize (before=%d) to idx=%d", arr.size(), idx);
            ensureArraySize(arr, idx);
            JsonNode existing = arr.get(idx);
            dbg("applyAtPath: existing at [%d] = %s (%s)", idx, existing, nodeType(existing));

            JsonNode toWrite = decideArrayWrite(existing, newValue, valueIsArrayLike);
            dbg("applyAtPath: decideArrayWrite -> %s (%s)", toWrite, nodeType(toWrite));
            arr.set(idx, toWrite);
            dbg("applyAtPath: set array[%d] -> %s", idx, arr.get(idx));
        } else {
            dbg("applyAtPath: last segment is FIELD='%s'", tail.lastKey);
            ObjectNode obj = asObject(parent.container, parent.keyOrIndex, createIfMissing);
            dbg("applyAtPath: asObject -> %s (%s)", obj, (obj == null ? "null" : "ObjectNode"));
            if (obj == null) {
                dbg("applyAtPath: asObject returned null -> abort");
                return;
            }

            JsonNode existing = obj.get(tail.lastKey);
            dbg("applyAtPath: existing field '%s' = %s (%s)", tail.lastKey, existing, nodeType(existing));

            JsonNode toWrite;
            if (existing != null && existing.isArray()) {
                dbg("applyAtPath: existing is array -> decideArrayWrite");
                toWrite = decideArrayWrite(existing, newValue, valueIsArrayLike);
            } else if (existing != null && !existing.isArray()) {
                dbg("applyAtPath: existing is scalar/object -> promote to array and append/overwrite accordingly");
                ArrayNode arr = MAPPER.createArrayNode();
                arr.add(existing);
                if (valueIsArrayLike && newValue != null && newValue.isArray()) {
                    dbg("applyAtPath: incoming is array-like and array -> replace contents");
                    arr.removeAll();
                    arr.addAll((ArrayNode) newValue);
                } else {
                    dbg("applyAtPath: incoming is single -> append");
                    arr.add(newValue == null || newValue.isNull() ? NullNode.getInstance() : newValue);
                }
                toWrite = arr;
            } else {
                dbg("applyAtPath: no existing -> direct set");
                toWrite = newValue;
            }

            obj.set(tail.lastKey, toWrite);
            dbg("applyAtPath: field '%s' set to %s", tail.lastKey, obj.get(tail.lastKey));
        }
    }

    private JsonNode decideArrayWrite(JsonNode existing, JsonNode incoming, boolean incomingArrayLike) {
        dbg("decideArrayWrite(existing=%s (%s), incoming=%s (%s), arrayLike=%s)",
                existing, nodeType(existing), incoming, nodeType(incoming), incomingArrayLike);

        if (existing != null && existing.isArray()) {
            ArrayNode arr = (ArrayNode) existing;
            if (incomingArrayLike) {
                dbg("decideArrayWrite: incoming is array-like -> overwrite array values");
                arr.removeAll();
                if (incoming != null && incoming.isArray()) {
                    arr.addAll((ArrayNode) incoming);
                    dbg("decideArrayWrite: added all from incoming array");
                } else if (incoming != null) {
                    arr.add(incoming);
                    dbg("decideArrayWrite: added single node to array");
                }
                return arr;
            } else {
                dbg("decideArrayWrite: incoming is single -> append");
                arr.add(incoming == null || incoming.isNull() ? NullNode.getInstance() : incoming);
                return arr;
            }
        }
        dbg("decideArrayWrite: existing not array -> return incoming as-is");
        return incoming;
    }

    // ---- Resolve parent container for a simple, concrete path ----
    private static final class ContainerAndKey {
        final JsonNode container; // array or object that holds the last segment
        final String keyOrIndex;  // field name (object) or array field name (when creating arrays)
        ContainerAndKey(JsonNode container, String keyOrIndex) { this.container = container; this.keyOrIndex = keyOrIndex; }
    }

    private ContainerAndKey resolveContainer(String parentPath, boolean createIfMissing) {
        dbg("resolveContainer(parentPath=%s, createIfMissing=%s)", parentPath, createIfMissing);
        if ("$".equals(parentPath)) {
            dbg("resolveContainer: parent is root -> return (root, \"\")");
            return new ContainerAndKey(root, "");
        }

        List<PathToken> tokens = tokenizeSimple(parentPath);
        dbg("resolveContainer: tokens=%s", tokens);
        if (tokens.isEmpty()) {
            dbg("resolveContainer: tokens empty -> return (root, \"\")");
            return new ContainerAndKey(root, "");
        }

        JsonNode current = root;

        for (int i = 1; i < tokens.size(); i++) { // skip "$"
            PathToken t = tokens.get(i);
            boolean last = (i == tokens.size() - 1);
            dbg("resolveContainer: step %d/%d token=%s, current=%s (%s), last=%s",
                    i, tokens.size() - 1, t, current, nodeType(current), last);

            if (t.isIndex) {
                // 'current' must be the array we are indexing into
                if (!(current instanceof ArrayNode)) {
                    dbg("resolveContainer: current not ArrayNode at index token");
                    if (!createIfMissing) return null;
                    // We can't conjure an array here; arrays are created at previous object field via look-ahead.
                    dbg("resolveContainer: cannot fabricate array at this step -> return null");
                    return null;
                }
                ArrayNode arr = (ArrayNode) current;
                dbg("resolveContainer: ensureArraySize(arr.size=%d, idx=%d)", arr.size(), t.index);
                ensureArraySize(arr, t.index);
                JsonNode elem = arr.get(t.index);
                dbg("resolveContainer: elem@[%d]=%s (%s)", t.index, elem, nodeType(elem));

                if (last) {
                    dbg("resolveContainer: parent ends with [idx], need element ObjectNode for field write");
                    if ((elem == null || elem.isNull()) && createIfMissing) {
                        dbg("resolveContainer: elem null -> create ObjectNode at index");
                        ObjectNode created = MAPPER.createObjectNode();
                        arr.set(t.index, created);
                        elem = created;
                    }
                    if (elem == null || elem.isNull() || !elem.isObject()) {
                        if (!createIfMissing) {
                            dbg("resolveContainer: elem not object and cannot create -> return null");
                            return null;
                        }
                        dbg("resolveContainer: replacing elem with ObjectNode at index");
                        ObjectNode created = MAPPER.createObjectNode();
                        arr.set(t.index, created);
                        elem = created;
                    }
                    dbg("resolveContainer: return (elemObject, \"\")");
                    return new ContainerAndKey(elem, "");
                }

                // Need an ObjectNode here to continue descending
                if (elem == null || elem.isNull() || !elem.isObject()) {
                    if (!createIfMissing) {
                        dbg("resolveContainer: need ObjectNode to descend but cannot create -> return null");
                        return null;
                    }
                    dbg("resolveContainer: creating ObjectNode to descend at index");
                    ObjectNode created = MAPPER.createObjectNode();
                    arr.set(t.index, created);
                    elem = created;
                }
                current = elem;
            } else {
                // Field on an object
                if (!current.isObject()) {
                    dbg("resolveContainer: current not ObjectNode at field token -> return null");
                    return null;
                }
                ObjectNode obj = (ObjectNode) current;
                JsonNode next = obj.get(t.key);
                dbg("resolveContainer: obj.get('%s') -> %s (%s)", t.key, next, nodeType(next));

                if (next == null) {
                    if (!createIfMissing) {
                        dbg("resolveContainer: missing field and cannot create");
                        if (last) {
                            dbg("resolveContainer: last token -> return (obj, '%s')", t.key);
                            return new ContainerAndKey(obj, t.key);
                        }
                        return null;
                    }
                    // Look-ahead: if next token is an index, create an array; else create an object
                    if (!last && tokens.get(i + 1).isIndex) {
                        dbg("resolveContainer: look-ahead sees index -> create ArrayNode at '%s'", t.key);
                        ArrayNode arr = MAPPER.createArrayNode();
                        obj.set(t.key, arr);
                        next = arr;
                    } else {
                        dbg("resolveContainer: create ObjectNode at '%s'", t.key);
                        ObjectNode created = MAPPER.createObjectNode();
                        obj.set(t.key, created);
                        next = created;
                    }
                }

                if (last) {
                    dbg("resolveContainer: last token (field) -> return (obj, '%s')", t.key);
                    return new ContainerAndKey(obj, t.key);
                }

                current = next;
            }
        }

        dbg("resolveContainer: finished loop; return (current, '')");
        return new ContainerAndKey(current, "");
    }

    private ObjectNode asObject(JsonNode parent, String field, boolean create) {
        dbg("asObject(parent=%s (%s), field='%s', create=%s)", parent, nodeType(parent), field, create);
        if (parent == root && (field == null || field.isEmpty())) {
            dbg("asObject: parent is root and field empty -> return root");
            return root;
        }
        ObjectNode out = (parent != null && parent.isObject()) ? (ObjectNode) parent : null;
        dbg("asObject -> %s", out);
        return out;
    }

    private ArrayNode asArray(JsonNode parent, String fieldOrIndex, boolean create) {
        dbg("asArray(parent=%s (%s), fieldOrIndex='%s', create=%s)", parent, nodeType(parent), fieldOrIndex, create);
        if (parent == null) {
            dbg("asArray: parent null -> return null");
            return null;
        }

        // Case 1: parent itself is an array (we're writing by numeric index into it)
        if (parent.isArray()) {
            dbg("asArray: parent is already ArrayNode -> return it");
            return (ArrayNode) parent;
        }

        // Case 2: parent is an object; fieldOrIndex is the field that should hold an array
        if (parent.isObject()) {
            ObjectNode obj = (ObjectNode) parent;
            JsonNode existing = (fieldOrIndex == null || fieldOrIndex.isEmpty()) ? null : obj.get(fieldOrIndex);
            dbg("asArray: obj.get('%s') -> %s (%s)", fieldOrIndex, existing, nodeType(existing));

            if (existing != null) {
                if (existing.isArray()) {
                    dbg("asArray: existing is ArrayNode -> return it");
                    return (ArrayNode) existing;
                }
                if (create) {
                    dbg("asArray: existing not array; create==true -> wrap existing into new ArrayNode");
                    ArrayNode arr = MAPPER.createArrayNode();
                    arr.add(existing);
                    obj.set(fieldOrIndex, arr);
                    dbg("asArray: wrapped and set obj['%s'] -> %s", fieldOrIndex, arr);
                    return arr;
                }
                dbg("asArray: existing not array; create==false -> return null");
                return null;
            }

            // Field missing
            if (create) {
                dbg("asArray: field missing; create==true -> create new ArrayNode at '%s'", fieldOrIndex);
                ArrayNode arr = MAPPER.createArrayNode();
                obj.set(fieldOrIndex, arr);
                return arr;
            }
            dbg("asArray: field missing; create==false -> return null");
            return null;
        }

        // Not an array or suitable object context
        dbg("asArray: parent neither array nor object -> return null");
        return null;
    }

    private static void ensureArraySize(ArrayNode arr, int idx) {
        // verbose print for each growth step
        while (arr.size() <= idx) {
            dbg("ensureArraySize: arr.size=%d <= %d -> add NullNode", arr.size(), idx);
            arr.add(NullNode.getInstance());
        }
    }

    // ---- Path parsing helpers for simple concrete paths ----
    private static final class PathTail {
        final String parentPath;
        final boolean lastIsIndex;
        final String lastKey;
        final int lastIndex;
        PathTail(String parentPath, String lastKey) { this.parentPath = parentPath; this.lastIsIndex = false; this.lastKey = lastKey; this.lastIndex = -1; }
        PathTail(String parentPath, int lastIndex)  { this.parentPath = parentPath; this.lastIsIndex = true;  this.lastKey = null;   this.lastIndex = lastIndex; }
        @Override public String toString() {
            return "PathTail{parentPath='"+parentPath+"', lastIsIndex="+lastIsIndex+", lastKey='"+lastKey+"', lastIndex="+lastIndex+"}";
        }
    }

    private static PathTail splitParent(String fullPath) {
        dbg("splitParent(fullPath=%s)", fullPath);
        if (fullPath == null || fullPath.equals("$")) {
            PathTail pt = new PathTail("$", "");
            dbg("splitParent -> %s", pt);
            return pt;
        }
        int depth = 0, lastDot = -1, lastOpenIdx = -1, lastCloseIdx = -1;
        for (int i = 0; i < fullPath.length(); i++) {
            char c = fullPath.charAt(i);
            if (c == '[') { depth++; lastOpenIdx = i; }
            else if (c == ']') { depth--; lastCloseIdx = i; }
            else if (c == '.' && depth == 0) lastDot = i;
        }
        if (lastOpenIdx > lastDot && lastCloseIdx == fullPath.length() - 1) {
            String parent = fullPath.substring(0, lastOpenIdx);
            int idx = Integer.parseInt(fullPath.substring(lastOpenIdx + 1, lastCloseIdx));
            PathTail pt = new PathTail(parent.isEmpty() ? "$" : parent, idx);
            dbg("splitParent -> %s", pt);
            return pt;
        }
        if (lastDot >= 0 && lastDot < fullPath.length() - 1) {
            String parent = fullPath.substring(0, lastDot);
            String field = fullPath.substring(lastDot + 1);
            PathTail pt = new PathTail(parent.isEmpty() ? "$" : parent, field);
            dbg("splitParent -> %s", pt);
            return pt;
        }
        PathTail pt = new PathTail("$", fullPath.startsWith("$.") ? fullPath.substring(2) : fullPath);
        dbg("splitParent -> %s", pt);
        return pt;
    }

    private static final class PathToken {
        final boolean isIndex; final String key; final int index;
        private PathToken(String key) { this.isIndex = false; this.key = key; this.index = -1; }
        private PathToken(int index)  { this.isIndex = true;  this.key = null; this.index = index; }
        static PathToken key(String k) { return new PathToken(k); }
        static PathToken idx(int i)    { return new PathToken(i); }
        @Override public String toString() {
            return isIndex ? ("[" + index + "]") : key;
        }
    }

    private static List<PathToken> tokenizeSimple(String path) {
        dbg("tokenizeSimple(path=%s)", path);
        List<PathToken> out = new ArrayList<>();
        if (path == null || path.isEmpty()) {
            dbg("tokenizeSimple: empty path -> []");
            return out;
        }
        int i = 0, n = path.length();
        if (path.charAt(0) == '$') { out.add(PathToken.key("$")); i++; }
        while (i < n) {
            char c = path.charAt(i);
            if (c == '.') {
                int j = i + 1;
                while (j < n && path.charAt(j) != '.' && path.charAt(j) != '[') j++;
                String field = path.substring(i + 1, j);
                if (!field.isEmpty()) out.add(PathToken.key(field));
                i = j;
            } else if (c == '[') {
                int j = path.indexOf(']', i);
                int idx = Integer.parseInt(path.substring(i + 1, j));
                out.add(PathToken.idx(idx));
                i = j + 1;
            } else {
                int j = i;
                while (j < n && path.charAt(j) != '.' && path.charAt(j) != '[') j++;
                String field = path.substring(i, j);
                if (!field.isEmpty()) out.add(PathToken.key(field));
                i = j;
            }
        }
        dbg("tokenizeSimple -> %s", out);
        return out;
    }

    // ---- Value conversion helpers ----
    private static JsonNode toJsonNode(Object value) {
        dbg("toJsonNode(value=%s (%s))", value, value == null ? "null" : value.getClass().getName());
        if (value == null) {
            dbg("toJsonNode: value null -> NullNode");
            return NullNode.getInstance();
        }
        if (value instanceof JsonNode jn) {
            dbg("toJsonNode: value already JsonNode -> %s", nodeType(jn));
            return jn;
        }

        if (value instanceof Multimap<?, ?> mm) {
            dbg("toJsonNode: value is Multimap -> convert to Map<K, List<V>> then to tree");
            Map<Object, Collection<Object>> tmp = new LinkedHashMap<>();
            mm.asMap().forEach((k, coll) -> tmp.put(k, new ArrayList<>(coll)));
            JsonNode n = MAPPER.valueToTree(tmp);
            dbg("toJsonNode: result node=%s (%s)", n, nodeType(n));
            return n;
        }
        JsonNode n = MAPPER.valueToTree(value);
        dbg("toJsonNode: result node=%s (%s)", n, nodeType(n));
        return n;
    }

    private static boolean isArrayLike(Object v) {
        boolean res = (v instanceof JsonNode jn && jn.isArray())
                || (v instanceof Collection<?>)
                || (v != null && v.getClass().isArray())
                || (v instanceof Multimap<?, ?>);
        dbg("isArrayLike(value=%s (%s)) -> %s", v, (v == null ? "null" : v.getClass().getName()), res);
        return res;
    }

    // ---- Merge (ObjectNode, Map, LinkedListMultimap) ----
    public void merge(ObjectNode other) {
        dbg("merge(ObjectNode=%s)", other);
        if (other != null) root.setAll(other);
        dbg("merge: root now -> %s", root);
    }

    public void merge(Map<?, ?> other) {
        dbg("merge(Map=%s)", other);
        if (other == null) return;
        ObjectNode n = toObjectNode(other);
        root.setAll(n);
        dbg("merge: root now -> %s", root);
    }

    public void merge(LinkedListMultimap<?, ?> other) {
        dbg("merge(LinkedListMultimap=%s)", other);
        if (other == null) return;
        ObjectNode n = toObjectNode(other);
        root.setAll(n);
        dbg("merge: root now -> %s", root);
    }

    // ---- Normalize constructor/merge inputs ----
    private static ObjectNode toObjectNode(Map<?, ?> map) {
        dbg("toObjectNode(Map=%s)", map);
        if (map == null) {
            ObjectNode on = MAPPER.createObjectNode();
            dbg("toObjectNode(Map): map null -> empty ObjectNode");
            return on;
        }
        JsonNode n = MAPPER.valueToTree(map);
        dbg("toObjectNode(Map): valueToTree -> %s (%s)", n, nodeType(n));
        if (n != null && n.isObject()) return (ObjectNode) n;
        throw new IllegalArgumentException("Map did not serialize to an ObjectNode");
    }

    private static ObjectNode toObjectNode(LinkedListMultimap<?, ?> mm) {
        dbg("toObjectNode(Multimap=%s)", mm);
        if (mm == null) {
            ObjectNode on = MAPPER.createObjectNode();
            dbg("toObjectNode(Multimap): null -> empty ObjectNode");
            return on;
        }
        Map<Object, Collection<Object>> tmp = new LinkedHashMap<>();
        mm.asMap().forEach((k, coll) -> tmp.put(k, new ArrayList<>(coll)));
        JsonNode n = MAPPER.valueToTree(tmp);
        dbg("toObjectNode(Multimap): valueToTree -> %s (%s)", n, nodeType(n));
        if (n != null && n.isObject()) return (ObjectNode) n;
        throw new IllegalArgumentException("Multimap did not serialize to an ObjectNode");
    }

    private static boolean isSimpleCompletePath(String path) {
        boolean res = (path != null && SIMPLE_PATH.matcher(path).matches());
        dbg("isSimpleCompletePath(%s) -> %s", path, res);
        return res;
    }
}
