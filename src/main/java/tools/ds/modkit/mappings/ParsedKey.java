package tools.ds.modkit.mappings;

import com.api.jsonata4java.expressions.Expressions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tools.ds.modkit.mappings.NodeMap.MAPPER;

public class ParsedKey {

    public final String suffix;
    public final String fullPath;
    public final String parentPath;
    public boolean isPlainKey;
    public final boolean isDirectPath;
    public String topLevelFieldName;
    public String lastFieldName;
    public Integer lastArrayIndex;
    public Integer topArrayIndex;

    public int tokenCount;

//    public final String parentPath;

    // matches whole string, with optional suffix at the end
    private static final Pattern PATTERN = Pattern.compile("^(.*?)(?:\\s(as-[A-Z]+))?$");

    public ParsedKey(String key) {

        Matcher m = PATTERN.matcher(key.strip());
        m.matches(); // always true
        this.suffix = m.group(2); // may be null if no suffix
        this.fullPath = preParseKey(m.group(1).strip());
        this.parentPath = fullPath + ".{ 'p': % }.p";

        isDirectPath =
                fullPath
                        .replaceAll("\\[\\d+\\]", "")
                        .replaceAll("%|\\{|^|=|\\[|\\.\\.", "*").contains("*");

        if (isDirectPath) {
            List<String[]> tokens = Arrays.stream(fullPath.replaceAll("\\$\\.", "").split("\\."))
                    .map(s -> {
                        int index = s.indexOf("[");
                        if (index < 0) return new String[]{s};
                        return new String[]{s.substring(0, index).trim(), s.substring(index + 1, s.length() - 1).trim()};
                    }).toList();
            tokenCount = tokens.size();
            topLevelFieldName = tokens.getFirst()[0];
            lastFieldName = tokens.getLast()[0];
            lastArrayIndex = tokens.getLast().length > 1 ? parseOrNull(tokens.getLast()[1]) : null;
            topArrayIndex = tokens.getFirst().length > 1 ? parseOrNull(tokens.getFirst()[1]) : null;
            isPlainKey = tokens.size() == 1 && tokens.getFirst().length == 1;
        }

    }

    public static boolean setValue(ObjectNode root, ParsedKey key, Object value) {
        if (key.isDirectPath) {
            if (key.tokenCount < 3) {
                JsonNode jsonNode = root.get(key.topLevelFieldName);
                ArrayNode newArrayNode;
                if (!(jsonNode instanceof ArrayNode)) {
                    newArrayNode = MAPPER.createArrayNode();
                    if (jsonNode != null)
                        newArrayNode.add(MAPPER.valueToTree(jsonNode));
                    root.set(key.topLevelFieldName, newArrayNode);
                } else {
                    newArrayNode = (ArrayNode) jsonNode;
                }

                int topArrayIndex = key.topArrayIndex == null ? 0 : key.topArrayIndex;

                ObjectNode parentObject = (ObjectNode) getWithExpression(root, key.topLevelFieldName + "[" + topArrayIndex + "]");
                if (parentObject == null) {
                    JsonNode valueToAdd = key.tokenCount == 1 ? MAPPER.valueToTree(value) : MAPPER.createObjectNode();
                    if (key.topArrayIndex == null)
                        newArrayNode.add(valueToAdd);
                    else
                        ensureIndex(newArrayNode, topArrayIndex, valueToAdd);
                }

                if (key.tokenCount == 2) {
                    parentObject = parentObject != null ? parentObject : (ObjectNode) getWithExpression(root, key.topLevelFieldName + "[" + topArrayIndex + "]");
                    JsonNode lastProperty = parentObject.get(key.lastFieldName);
                    if (lastProperty instanceof ArrayNode lastArrayNode) {
                        int lastArrayIndex = key.lastArrayIndex == null ? 0 : key.lastArrayIndex;
                        ensureIndex(lastArrayNode, lastArrayIndex, MAPPER.valueToTree(value));
                    } else
                        parentObject.set(key.lastFieldName, MAPPER.valueToTree(value));
                }
            } else {
                JsonNode node = getWithExpression(root, key.fullPath);
                if (node instanceof ArrayNode arrayNode) {
                    arrayNode.add(MAPPER.valueToTree(value));
                } else {
                    ObjectNode parentNode = (ObjectNode) getWithExpression(root, key.parentPath);
                    parentNode.set(key.lastFieldName, MAPPER.valueToTree(value));
                }
            }
        }
        return true;
    }


    public static JsonNode getWithExpression(ObjectNode root, String expression) {
        Expressions e = null;
        try {
            e = Expressions.parse(expression);
            return e.evaluate(root);
        } catch (Exception ex) {
            return null;
        }
    }


    private static final Pattern INDEX_PATTERN = Pattern.compile("#[\\d:,]+");
    private static final Pattern INT_PATTERN = Pattern.compile("\\d+");

    public static String preParseKey(String key) {
        return rewrite(key.replaceAll("\\s*([{}\\[\\].#:,-])\\s*", "$1"))
                .replaceAll("(^[A-Za-z0-9_]+)([^\\[].*|$)", "$1[-1]$2")
                .replaceAll("\\[*\\]", "")
                ;
    }


    public static String rewrite(String input) {
        if (input == null) return null;
        return INDEX_PATTERN.matcher(input).replaceAll(mr -> {
            String body = mr.group().substring(1);

            String decremented = INT_PATTERN.matcher(body).replaceAll(nmr -> {
                int n = Integer.parseInt(nmr.group());
                return Integer.toString(n - 1);
            });

            return "[" + decremented + "]";
        });
    }

    private static Integer parseOrNull(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
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
