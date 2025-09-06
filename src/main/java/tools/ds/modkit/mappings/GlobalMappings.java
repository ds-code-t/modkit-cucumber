package tools.ds.modkit.mappings;

import com.google.common.collect.*;

public class GlobalMappings {

    private static final ListMultimap<Object, Object> GLOBAL =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());

    public static void put(Object k, Object v) {
        GLOBAL.put(k, v);
    }

    public static java.util.List<Object> get(Object k) {
        return GLOBAL.get(k);
    }

    // IMPORTANT: synchronize when iterating
    public static void dump(java.util.function.Consumer<java.util.Map.Entry<Object, Object>> c) {
        synchronized (GLOBAL) {
            for (var e : GLOBAL.entries()) c.accept(e);
        }
    }


}
