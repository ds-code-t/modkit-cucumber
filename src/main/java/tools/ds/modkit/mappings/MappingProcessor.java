package tools.ds.modkit.mappings;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.LinkedListMultimap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tools.ds.modkit.mappings.AviatorUtil.eval;
import static tools.ds.modkit.mappings.AviatorUtil.evalToBoolean;
//import static tools.ds.modkit.mappings.KeyParser.Kind.SINGLE;
//import static tools.ds.modkit.mappings.KeyParser.parseKey;


public abstract class MappingProcessor implements Map<String, Object> {


    private final LinkedListMultimap<String, NodeMap> maps = LinkedListMultimap.create();
    private final List<String> keyOrder;

    public MappingProcessor(String... keys) {
        // Defensive copy to make key order immutable
        this.keyOrder = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(keys)));
    }

    public void addEntries(String key, NodeMap... values) {
        if (!keyOrder.contains(key)) {
            throw new IllegalArgumentException("Key not part of initial key order: " + key);
        }
        for (NodeMap v : values) {
            maps.put(key, v);
        }
    }

    public void addEntriesToStart(String key, NodeMap... values) {
        if (!keyOrder.contains(key)) {
            throw new IllegalArgumentException("Key not part of initial key order: " + key);
        }
        if (values.length == 0) return;                // no-op
        maps.get(key).addAll(0, List.of(values));      // insert at start in given order
    }

    public void overWriteEntries(String key, NodeMap... values) {
        if (!keyOrder.contains(key)) {
            throw new IllegalArgumentException("Key not part of initial key order: " + key);
        }
        maps.removeAll(key);
        for (NodeMap v : values) {
            maps.put(key, v);
        }
    }

    public void clearEntry(String key) {
        if (!keyOrder.contains(key)) {
            throw new IllegalArgumentException("Key not part of initial key order: " + key);
        }
        maps.removeAll(key);
    }

    /**
     * Get a flat list of values, grouped and ordered by the original key order
     */
    public List<NodeMap> valuesInKeyOrder() {
        List<NodeMap> out = new ArrayList<>();
        for (String key : keyOrder) {
            out.addAll(maps.get(key)); // maps.get() is live, but empty if unused
        }
        return out;
    }


    /**
     * Expose immutable key order (for debugging/inspection)
     */
    public List<String> keyOrder() {
        return keyOrder;
    }


    private static final Pattern ANGLE = Pattern.compile("<([^<>{}]+)>");
    private static final Pattern CURLY = Pattern.compile("\\{([^{}]+)\\}");


//    public void put(int index, Object key, Object value) {
//        maps.get(index).put(key, value);
//    }


    public String resolveWholeText(String input) {
        System.out.println("@@===resolveWholeText: " + input);
        try {
            QuoteParser parsedObj = new QuoteParser(input);
            parsedObj.setMasked(resolveAll(parsedObj.masked()));
            System.out.println("@@===parsedObj: " + parsedObj);
            for (var e : parsedObj.entrySet()) {
                System.out.println("@@===e: " + e);
                char q = parsedObj.quoteTypeOf(e.getKey());
                System.out.println("@@===q: " + q);
                if (q == QuoteParser.SINGLE || q == QuoteParser.DOUBLE) {
                    parsedObj.put(e.getKey(), resolveAll(e.getValue()));
                }
            }
            return parsedObj.restore();
        } catch (Throwable t) {
            throw new RuntimeException("Could not resolve'" + input + "'", t);
        }
    }

    public String resolveAll(String input) {
        System.out.println("@@===resolveAll: " + input);
        try {
            String prev;
            do {
                prev = input;
                if (input.contains("<")) {
                    input = resolveByMap(input);
                    System.out.println("@@=====3: " + input);
                } else {
                    break;
                }
                if (input.contains("{")) {
                    input = resolveCurly(input);
                }
            } while (!input.equals(prev));
            return input;
        } catch (Throwable t) {
            throw new RuntimeException("Could not resolve'" + input + "'", t);
        }
    }

    private static final Pattern AS_TYPE_PATTERN =
            Pattern.compile("^(.*?)(\\s+as-[A-Z]+)$");

    private String resolveByMap(String s) {
        System.out.println("@@===resolveByMap: " + s);
        System.out.println("@@toZeroBasedSeries1" + s);
        String mainText = toZeroBasedSeries(s).trim();
        System.out.println("@@toZeroBasedSeries2 " + mainText);
        String suffix = "";
        Matcher suffixMatcher = AS_TYPE_PATTERN.matcher(mainText);
        if (suffixMatcher.matches()) {
            mainText = suffixMatcher.group(1);
            suffix = suffixMatcher.group(2);   // always non-null here
        }

        try {
            Matcher m = ANGLE.matcher(mainText);
            StringBuffer sb = new StringBuffer();
            Object replacement = null;
            while (m.find()) {

                System.out.println("@@$$m.group(1): " + m.group(1));

                for (NodeMap map : valuesInKeyOrder()) {
                    if (map == null) continue;
                    List<?> list = map.getValues(m.group(1));
                    if (list == null || list.isEmpty()) continue;
                    replacement = suffix.isEmpty() ? list.get(list.size() - 1) : (suffix.equals("as-LIST") ? list : list.get(list.size() - 1));
                    if (replacement != null)
                        break;
                }
                if (replacement != null) break;
            }

            if (replacement == null)
                return s;

            m.appendReplacement(sb, String.valueOf(replacement));

            m.appendTail(sb);
            return sb.toString();
        } catch (Throwable t) {
            throw new RuntimeException("Could not resolve by map '" + s + "'", t);
        }
    }


    private String resolveCurly(String s) {
        try {
            Matcher m = CURLY.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String key = m.group(1).trim();
                String repl = key.endsWith("?") ? String.valueOf(evalToBoolean(key.substring(0, key.length() - 1), this)) : String.valueOf(eval(key, this));
                m.appendReplacement(sb, repl == null ? m.group(0) : Matcher.quoteReplacement(repl));
            }
            m.appendTail(sb);
            return sb.toString();
        } catch (Throwable t) {
            throw new RuntimeException("Could not resolve by curly bracket expression '" + s + "'", t);
        }
    }


    @Override
    public Object get(Object key) {
        if(key==null)
            throw new RuntimeException("key cannot be null");
        for (NodeMap map : maps.values()) {
            List<?> list = map.getValues(String.valueOf(key));
            if (list.isEmpty()) continue;
            return list.get(list.size() - 1);
        }
        return null;
    }


    @Override
    public Object put(String key, Object value) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }


    @Override
    public Object remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<String> keySet() {
        return Set.of();
    }

    @Override
    public Collection<Object> values() {
        return List.of();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return Set.of();
    }


    private static final Pattern SERIES = Pattern.compile("#\\s*([\\d\\s,\\-:]+)");

    public static String toZeroBasedSeries(String input) {
        return SERIES.matcher(input).replaceAll(mr -> {
                    String seq = mr.group(1);                 // e.g. "1-2 , 6"
                    StringBuilder out = new StringBuilder("[");
                    for (String token : seq.replaceAll("\\s+", "")
                            .split("(?=[,\\-:])|(?<=[,\\-:])")) {
                        if (token.equals(",") || token.equals("-") || token.equals(":")) {
                            out.append(token);
                        } else {
                            int n = Integer.parseInt(token);
                            if (n == 0) throw new IllegalArgumentException("Index cannot be 0 when using '#' syntax");
                            out.append(n - 1);
                        }
                    }
                    out.append(']');

                    return out.toString();
                }).replaceAll("\\s+\\[", "[")   // remove whitespace before '['
                .replaceAll("]\\s+", "]")// remove whitespace after ']'
                .replaceAll("\\s*\\.\\s*", ".");
    }

}
