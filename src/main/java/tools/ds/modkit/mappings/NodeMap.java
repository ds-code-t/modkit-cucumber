package tools.ds.modkit.mappings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.LinkedListMultimap;

import tools.ds.modkit.mappings.queries.Tokenized;

import java.util.*;
import java.util.regex.Pattern;

import static tools.ds.modkit.blackbox.BlackBoxBootstrap.metaFlag;

public class NodeMap {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new GuavaModule());
    }

    public final static String rootFlag = metaFlag + "_ROOT";

    private final ObjectNode root;

    public NodeMap() {
        this.root = MAPPER.createObjectNode();
        this.root.set(rootFlag, MAPPER.valueToTree(true));
    }

    public NodeMap(Map<?, ?> map) {
        this.root = toObjectNode(map);
        this.root.set(rootFlag, MAPPER.valueToTree(true));
    }

    public NodeMap(LinkedListMultimap<?, ?> multimap) {
        this.root = toObjectNode(multimap);
        this.root.set(rootFlag, MAPPER.valueToTree(true));
    }

    public ObjectNode objectNode() {
        return root;
    }

    public List<JsonNode> getAsList(Tokenized tokenized) {
        return tokenized.getList(root);
    }
    public List<JsonNode> getAsList(String key) {
        return (new Tokenized(key).getList(root));
    }

    public Object get(Tokenized tokenized) {
        return  tokenized.get(root);
    }


    public Object get(String key) {
        return  (new Tokenized(key)).get(root);
    }


    public void put(String key, Object value) {
        (new Tokenized(key)).setWithPath(root, value);
    }


    // ---- Merge (ObjectNode, Map, LinkedListMultimap) ----
    public void merge(ObjectNode other) {
        if (other != null) root.setAll(other);
    }

    public void merge(Map<?, ?> other) {
        if (other != null) root.setAll(toObjectNode(other));
    }

    public void merge(LinkedListMultimap<?, ?> other) {
        if (other != null) root.setAll(toObjectNode(other));
    }

    // ---- Normalize constructor/merge inputs ----
    private static ObjectNode toObjectNode(Map<?, ?> map) {
        if (map == null) return MAPPER.createObjectNode();
        JsonNode n = MAPPER.valueToTree(map);
        if (n != null && n.isObject()) return (ObjectNode) n;
        throw new IllegalArgumentException("Map did not serialize to an ObjectNode");
    }

    private static ObjectNode toObjectNode(LinkedListMultimap<?, ?> mm) {
        if (mm == null) return MAPPER.createObjectNode();
        Map<Object, Collection<Object>> tmp = new LinkedHashMap<>();
        mm.asMap().forEach((k, coll) -> tmp.put(k, new ArrayList<>(coll)));
        JsonNode n = MAPPER.valueToTree(tmp);
        if (n != null && n.isObject()) return (ObjectNode) n;
        throw new IllegalArgumentException("Multimap did not serialize to an ObjectNode");
    }



}
