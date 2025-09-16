package tools.ds.modkit.mappings;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tools.ds.modkit.mappings.AviatorUtil.eval;
import static tools.ds.modkit.mappings.AviatorUtil.evalToBoolean;
import static tools.ds.modkit.mappings.GlobalMappings.GLOBALS;
import static tools.ds.modkit.mappings.KeyParser.Kind.SINGLE;
import static tools.ds.modkit.mappings.KeyParser.parseKey;

import java.util.*;
import java.util.regex.*;


public abstract class MappingProcessor implements Map<String, Object> {


    protected final LinkedHashMap<String, NodeMap> maps = new LinkedHashMap<>();


    private static final Pattern ANGLE = Pattern.compile("<([^<>{}]+)>");
    private static final Pattern CURLY = Pattern.compile("\\{([^{}]+)\\}");


    public void put(int index, Object key, Object value) {
        maps.get(index).put(key, value);
    }


    public String resolveWholeText(String input) {

        QuoteParser parsedObj = new QuoteParser(input);
        parsedObj.setMasked(resolveAll(parsedObj.masked()));
        for (var e : parsedObj.entrySet()) {
            char q = parsedObj.quoteTypeOf(e.getKey());
            if (q == QuoteParser.SINGLE || q == QuoteParser.DOUBLE) {
                parsedObj.put(e.getKey(), resolveAll(e.getValue()));
            }
        }
        return parsedObj.restore();
    }

    public String resolveAll(String input) {
        String prev;
        do {
            prev = input;
            if (input.contains("<")) {
                input = resolveByMap(input);
            } else {
                break;
            }
            if (input.contains("{")) {
                input = resolveCurly(input);
            }
        } while (!input.equals(prev));
        return input;
    }

    private String resolveByMap(String s) {
        Matcher m = ANGLE.matcher(s);
        StringBuffer sb = new StringBuffer();
        String replacement = null;
        while (m.find()) {
            KeyParser.KeyParse keys = parseKey(m.group(1));

            for (NodeMap map : maps.values()) {
                List<Object> list = map.getAsList(keys.base(), keys.intList());
                if (list.isEmpty()) continue;
                replacement = keys.kind().equals(SINGLE) ? String.valueOf(list.getFirst()) : String.valueOf(list);
                break;
            }
            if (replacement != null) break;
        }

        m.appendReplacement(sb, replacement == null ? m.group(0) : replacement);

        m.appendTail(sb);
        return sb.toString();
    }


    private String resolveCurly(String s) {
        Matcher m = CURLY.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1).trim();
            String repl = key.endsWith("?") ? String.valueOf(evalToBoolean(key.substring(0, key.length() - 1), this)) : String.valueOf(eval(key, this));
            m.appendReplacement(sb, repl == null ? m.group(0) : Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }


    @Override
    public Object get(Object key) {
        for (NodeMap map : maps.values()) {
            List<Object> list = map.getAsList(key, -1);
            if (list.isEmpty()) continue;
            return list.getFirst();
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
}
