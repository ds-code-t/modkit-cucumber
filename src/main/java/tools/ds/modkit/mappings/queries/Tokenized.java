package tools.ds.modkit.mappings.queries;

import com.api.jsonata4java.expressions.EvaluateException;
import com.api.jsonata4java.expressions.Expressions;
import com.api.jsonata4java.expressions.ParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static tools.ds.modkit.mappings.NodeMap.MAPPER;

public final class Tokenized {

    final String query;
    final List<String> tokens;
    final int tokenCount;

    final boolean directPath;

    public Tokenized(String inputQuery) {
        query = inputQuery.strip().replaceAll("^\\$\\.", "")
                .replaceAll("(^[A-Za-z0-9_]+)([^\\[].*|$)", "$1[-1]$2")
                .replaceAll("\\s*([\\(\\){}\\[\\].#:,-])\\s*", "$1");
//                .replaceAll("\\[\\*\\]", "");
        directPath = query.replaceAll("\\[\\d+\\]", "")
                .replaceAll("\\*|%|\\{|\\(|^|<|>|=|\\.\\.|,|:", "[").contains("\\[");
        tokens = Arrays.stream(query.replaceAll("(\\[[^\\[\\]]\\])", ".$1").split("\\.")).toList();
        tokenCount = tokens.size();
    }


    public List<JsonNode> getList(JsonNode root) {
        return getList(root, query);
    }

    public List<JsonNode> getList(JsonNode root, String queryString) {
        JsonNode returnedNode = getWithPath(root, queryString);
        if (returnedNode == null)
            return Collections.emptyList();
        if (returnedNode instanceof ArrayNode arrayNode)
            return arrayNode.valueStream().toList();
        List<JsonNode> nodeList = new ArrayList<>();
        nodeList.add(returnedNode);
        return nodeList;
    }


//    public List<JsonNode> getParentsList(JsonNode root) {
//        return getList(root, query + ".{ 'p': % }.p");
//    }


    public JsonNode getWithPath(JsonNode root) {
        return getWithPath(root, query);
    }


    public static JsonNode getWithPath(JsonNode root, String passedQuery) {
        Expressions e = null;
        try {
            e = Expressions.parse(passedQuery.replaceAll("\\[\\*\\]", ""));
            return e.evaluate(root);
        } catch (ParseException | IOException | EvaluateException ex) {
            return null;
        }
    }


    public void setWithPath(JsonNode root, Object finalValue, Tokenized... tokenizeds) {
        JsonNode currentValue = MAPPER.valueToTree(finalValue);
        for (int t = tokenizeds.length - 1; t >= 0; t--) {
            Tokenized tokenize = tokenizeds[t];
            currentValue = tokenize.setWithPath(currentValue);
        }

    }

    public void setWithPath(Object value) {
        JsonNode root = tokens.getFirst().startsWith("[") ? MAPPER.createArrayNode() : MAPPER.createObjectNode();
        setWithPath(root, value);
    }


    public void setWithPath(JsonNode root, Object value) {
        if (directPath) {
            JsonNode currentNode = root;
            for (int i = 0; i < tokenCount; i++) {
                String token = tokens.get(i);
                String nextToken = i + 1 < tokenCount ? tokens.get(i + 1) : null;
                Object valueToSet = nextToken == null ? MAPPER.valueToTree(value) : nextToken.startsWith("[") ? ArrayNode.class : ObjectNode.class;
                if (currentNode instanceof ArrayNode arrayNode) {
                    Integer index = token.startsWith("[") ? token.equals("[]") ? arrayNode.size() : Integer.parseInt(token.substring(1, token.length() - 1)) : null;
                    currentNode = setArrayNode(arrayNode, index, valueToSet);
                } else if (currentNode instanceof ObjectNode objectNode) {
                    currentNode = setProperty(objectNode, token, valueToSet);
                } else {
                    throw new RuntimeException("Cannot set '" + currentNode + "'. JsonNode needs to be ArrayNode or ObjectNode");
                }
            }
        } else {
            String lastToken = tokens.getLast();
            List<JsonNode> parentNodes = getList(root, query.replaceAll("\\.?" + lastToken + "$", ""));
            for (JsonNode parent : parentNodes) {
                if (parent instanceof ArrayNode arrayNode) {
                    if (lastToken.startsWith("[")) {
                        if (lastToken.equals("[*]")) {
                            IntStream.range(0, arrayNode.size()).forEach(i -> arrayNode.set(i, MAPPER.valueToTree(value)));
                        } else if (lastToken.equals("[]")) {
                            arrayNode.add(MAPPER.valueToTree(value));
                        } else if (lastToken.startsWith("[")) {
                            arrayNode.set(arrayNode.size() - 1, MAPPER.valueToTree(value));
                        }
                    }
                } else if (parent instanceof ObjectNode objectNode) {
                    if (!lastToken.startsWith("[")) {
                        if (objectNode.has(lastToken)) {
                            objectNode.set(lastToken, MAPPER.valueToTree(value));
                        }
                    }
                }
            }
        }
    }


    public static JsonNode setProperty(ObjectNode objectNode, String fieldName, Object value) {
        JsonNode valueToSet = value instanceof JsonNode valueNode ? valueNode : objectNode.get(fieldName);
        if (valueToSet == null)
            valueToSet = value.equals(ArrayNode.class) ? MAPPER.createArrayNode() : MAPPER.createObjectNode();
        objectNode.set(fieldName, valueToSet);
        return valueToSet;
    }

    public static JsonNode setArrayNode(ArrayNode arrayNode, Integer index, Object value) {
        if (index == null) {
            JsonNode valueToSet = value instanceof JsonNode valueNode ? valueNode : value.equals(ArrayNode.class) ? MAPPER.createArrayNode() : MAPPER.createObjectNode();
            arrayNode.add(valueToSet);
            return valueToSet;
        }

        JsonNode currentlySet = ensureIndex(arrayNode, index);
        if (value instanceof JsonNode valueNode) {
            arrayNode.set(index, valueNode);
            return valueNode;
        }
        if (currentlySet != null)
            return currentlySet;

        JsonNode defaultValue = value.equals(ArrayNode.class) ? MAPPER.createArrayNode() : MAPPER.createObjectNode();
        arrayNode.set(index, defaultValue);
        return defaultValue;
    }


    public static JsonNode ensureIndex(ArrayNode array, int index) {
        if (array == null || index < 0) {
            throw new IllegalArgumentException("ArrayNode is null or index < 0");
        }
        while (array.size() < index) {
            array.add(NullNode.instance);
        }
        return array.get(index);
    }

}
