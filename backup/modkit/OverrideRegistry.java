package tools.ds.modkit;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry of method overrides declared by {@link PatchProvider}s.
 *
 * Each entry maps:
 *  - targetType (FQCN; interface or class),
 *  - methodName,
 *  - erased parameter types (FQCNs),
 *  - and a {@link MethodOverride} implementation.
 *
 * Lookup is done at call time using the declaring class of the invoked method;
 * a match occurs when the declaring class is assignable to the registered targetType
 * and the name + parameter types match.
 */
public final class OverrideRegistry {

    /** A single override entry. */
    public static final class Entry {
        public final String targetType;       // FQCN, e.g. "io.cucumber.core.gherkin.Pickle"
        public final String methodName;       // e.g. "getName"
        public final List<String> paramTypes; // erased param types as FQCNs
        public final MethodOverride override;

        Entry(String targetType, String methodName, List<String> paramTypes, MethodOverride override) {
            this.targetType = Objects.requireNonNull(targetType, "targetType");
            this.methodName = Objects.requireNonNull(methodName, "methodName");
            this.paramTypes = Objects.requireNonNull(paramTypes, "paramTypes");
            this.override   = Objects.requireNonNull(override, "override");
        }

        @Override
        public String toString() {
            return targetType + "#" + methodName +
                    (paramTypes.isEmpty() ? "()" : "(" + String.join(",", paramTypes) + ")");
        }
    }

    private static final OverrideRegistry INSTANCE = new OverrideRegistry();
    public static OverrideRegistry get() { return INSTANCE; }

    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("modkit.debug", "true"));

    // Thread-safe list; registration typically happens once at startup.
    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    // Per-classloader cache of resolved target classes (also caches misses via Optional.empty()).
    private final Map<ClassLoader, Map<String, Optional<Class<?>>>> resolveCache =
            Collections.synchronizedMap(new WeakHashMap<>());

    private OverrideRegistry() {}

    /** Register a new override. */
    public void override(String targetType, String methodName, Class<?>[] paramTypes, MethodOverride impl) {
        List<String> params = new ArrayList<>();
        if (paramTypes != null) {
            for (Class<?> p : paramTypes) params.add(p.getName());
        }
        Entry e = new Entry(targetType, methodName, Collections.unmodifiableList(params), impl);
        entries.add(e);
        if (DEBUG) System.err.println("[modkit:registry] + " + e);
    }

    /** Immutable snapshot of all entries (for match construction / diagnostics). */
    public List<Entry> entriesSnapshot() {
        return List.copyOf(entries);
    }

    /** All target type FQCNs that have been registered. */
    public Set<String> targetTypeNames() {
        Set<String> out = new LinkedHashSet<>();
        for (Entry e : entries) out.add(e.targetType);
        return out;
    }

    /** Package prefixes derived from target types; useful to scope debug logging. */
    public Set<String> targetPackagePrefixes() {
        Set<String> pkgs = new LinkedHashSet<>();
        for (String fqn : targetTypeNames()) {
            int i = fqn.lastIndexOf('.');
            pkgs.add(i > 0 ? fqn.substring(0, i) : "");
        }
        return pkgs;
    }

    /**
     * Find an override for a specific reflective {@link Method} (the origin).
     * Returns null if none match. Logs only on a positive match (keeps output clean).
     */
    public MethodOverride findFor(Method origin) {
        final String name = origin.getName();
        final Class<?>[] pts = origin.getParameterTypes();
        final Class<?> decl = origin.getDeclaringClass();

        for (Entry e : entries) {
            if (!e.methodName.equals(name)) continue;
            if (e.paramTypes.size() != pts.length) continue;

            boolean sameParams = true;
            for (int i = 0; i < pts.length; i++) {
                if (!pts[i].getName().equals(e.paramTypes.get(i))) { sameParams = false; break; }
            }
            if (!sameParams) continue;

            try {
                Class<?> target = resolveTarget(e.targetType, decl.getClassLoader());
                if (target != null && target.isAssignableFrom(decl)) {
                    if (DEBUG) System.err.println("[modkit:lookup] " + decl.getName() + "#" + name + " -> " + e);
                    return e.override;
                }
            } catch (Throwable ignore) {
                // Swallow and continue scanning other entries.
            }
        }
        // No override found: intentionally silent to avoid noisy logs.
        return null;
    }

    /**
     * True if the given class is assignable to any registered target type.
     * Used by instrumentation filters to quickly decide whether a class is relevant.
     */
    public boolean isRelevant(Class<?> c) {
        if (c == null) return false;
        ClassLoader cl = c.getClassLoader();
        for (String targetName : targetTypeNames()) {
            Class<?> t = resolveTarget(targetName, cl);
            if (t != null && t.isAssignableFrom(c)) return true;
        }
        return false;
    }

    /** Resolve a target type by name for a given loader, with positive/negative caching. */
    private Class<?> resolveTarget(String fqcn, ClassLoader cl) {
        Map<String, Optional<Class<?>>> perLoader =
                resolveCache.computeIfAbsent(cl, k -> new HashMap<>());
        Optional<Class<?>> cached = perLoader.get(fqcn);
        if (cached != null) return cached.orElse(null);

        try {
            Class<?> c = Class.forName(fqcn, false, cl);
            perLoader.put(fqcn, Optional.of(c));
            return c;
        } catch (Throwable t) {
            perLoader.put(fqcn, Optional.empty());
            return null;
        }
    }
}
