package tools.ds.modkit.mappings;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.*;

import static tools.ds.modkit.mappings.AviatorUtil.eval;
import static tools.ds.modkit.mappings.AviatorUtil.evalToBoolean;

public class ParsingMap implements Map<String, Object> {
    public static void main(String[] args) {

    }
    private final ArrayListMultimap<Object, Object> OverrideMap = ArrayListMultimap.create();
    private final ArrayListMultimap<Object, Object> scenarioMap = ArrayListMultimap.create();
    private final ArrayListMultimap<Object, Object> scenarioMapDefaults = ArrayListMultimap.create();
    private final ArrayListMultimap<Object, Object> runMap = ArrayListMultimap.create();
    //  GlobalMap
    private final ArrayListMultimap<Object, Object> DefaultMap = ArrayListMultimap.create();


    private final List<ArrayListMultimap<Object, Object>> maps =
            Arrays.asList(ArrayListMultimap.create(), ArrayListMultimap.create(),
                    ArrayListMultimap.create(), ArrayListMultimap.create(),
                    ArrayListMultimap.create());

    private static final Pattern ANGLE = Pattern.compile("<([^<>{}]+)>");
    private static final Pattern CURLY = Pattern.compile("\\{([^{}]+)\\}");


    public void put(int index, Object key, Object value) {
        maps.get(index).put(key, value);
    }

    public String resolveWholeText(String input) {
        QuoteParser parsedObj =  new QuoteParser(input);
        parsedObj.setMasked(resolveAll(parsedObj.masked()));
        int i = 0;
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
            for (int i = 0; i < maps.size(); i++) {
                if (input.contains("<")) {
                    input = resolveByMap(i, input);
                } else {
                    break;
                }
            }
            if (input.contains("{")) {
                input = resolveCurly(input);
            }
        } while (!input.equals(prev));
        return input;
    }

    private String resolveByMap(int idx, String s) {
        Matcher m = ANGLE.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1).trim();
            Collection<Object> vals = maps.get(idx).get(key);
            m.appendReplacement(sb, vals.isEmpty() ? m.group(0)
                    : Matcher.quoteReplacement(vals.iterator().next().toString()));
        }
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
        for (ArrayListMultimap<Object, Object> map : maps) {
            if (map.containsKey(key)) return map.get(key);
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
    public Set<Entry<String, Object>> entrySet() {
        return Set.of();
    }
}
