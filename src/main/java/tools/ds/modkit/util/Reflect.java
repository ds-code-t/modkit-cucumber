package tools.ds.modkit.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Small helper for reflective calls against public or non-public classes. */
public final class Reflect {

    /**
     * Invoke a method (public or private), supporting both instance and static targets.
     *
     * Usage:
     *  - Instance: invokeAnyMethod(someObject, "doThing", args...)
     *  - Static:   invokeAnyMethod(SomeClass.class, "staticThing", args...)
     *
     * Matching order:
     *  1) by name (instance methods preferred when target is an instance; static-only if target is Class),
     *  2) then by argument count (varargs accepted),
     *  3) then by argument types (best assignable match).
     * Returns null on failure.
     */
    public static Object invokeAnyMethod(Object target, String methodName, Object... args) {
        if (target == null || methodName == null || methodName.isEmpty()) return null;
        final Object[] a = (args == null) ? new Object[0] : args;

        final boolean staticCall = (target instanceof Class<?>);
        final Class<?> clazz = staticCall ? (Class<?>) target : target.getClass();
        final Object receiver = staticCall ? null : target;

        // Collect candidates by name, preferring instance methods first for instance calls.
        List<Method> instanceNamed = new ArrayList<>();
        List<Method> staticNamed   = new ArrayList<>();

        for (Class<?> k = clazz; k != null; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (Modifier.isStatic(m.getModifiers())) {
                    staticNamed.add(m);
                } else {
                    instanceNamed.add(m);
                }
            }
        }

        List<Method> named = new ArrayList<>();
        if (staticCall) {
            // Static target: only consider static methods.
            named.addAll(staticNamed);
        } else {
            // Instance target: prefer instance methods; fallback to static if needed.
            named.addAll(instanceNamed);
            if (named.isEmpty()) named.addAll(staticNamed);
        }

        if (named.isEmpty()) return null;

        // Fast path: a single candidate → try it.
        if (named.size() == 1) {
            return tryInvoke(receiver, named.get(0), a);
        }

        // Filter by parameter count (respecting varargs)
        List<Method> byCount = new ArrayList<>();
        for (Method m : named) {
            if (acceptsArgCount(m, a.length)) {
                byCount.add(m);
            }
        }
        if (byCount.isEmpty()) {
            // No exact/vararg count match; fall back to first by name (may still succeed)
            return tryInvoke(receiver, named.get(0), a);
        }
        if (byCount.size() == 1) {
            return tryInvoke(receiver, byCount.get(0), a);
        }

        // Choose best by assignability score
        Method best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Method m : byCount) {
            int score = applicabilityScore(m, a);
            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }
        if (best == null) best = byCount.get(0);
        return tryInvoke(receiver, best, a);
    }

    // ---- internals -----------------------------------------------------------

    private static Object tryInvoke(Object receiver, Method m, Object[] args) {
        try {
            makeAccessible(m, receiver);
            Object[] callArgs = prepareArgsForVarargs(m, args);
            // For static methods, receiver may be null (that's fine).
            return m.invoke(receiver, callArgs);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static void makeAccessible(Method m, Object receiver) {
        try {
            if (!m.canAccess(receiver)) {
                m.setAccessible(true);
            }
        } catch (Throwable ignore) {
            // Best effort only; if JPMS blocks it, invoke will fail and we return null.
        }
    }

    /** Does this method accept this many arguments (including via varargs)? */
    private static boolean acceptsArgCount(Method m, int argCount) {
        int params = m.getParameterCount();
        if (m.isVarArgs()) {
            // varargs: at least (params - 1) must be present
            return argCount >= params - 1;
        } else {
            return argCount == params;
        }
    }

    /** Higher score = better match. Prefers non-varargs, exact type matches, then assignable matches. */
    private static int applicabilityScore(Method m, Object[] args) {
        Class<?>[] pt = m.getParameterTypes();
        boolean var = m.isVarArgs();
        int score = 0;

        if (!var && pt.length != args.length) {
            return Integer.MIN_VALUE; // impossible
        }

        if (var) {
            // non-vararg parameters
            int fixed = pt.length - 1;
            for (int i = 0; i < fixed; i++) {
                score += matchScore(pt[i], args[i]);
            }
            // vararg tail
            Class<?> comp = pt[pt.length - 1].getComponentType();
            for (int i = fixed; i < args.length; i++) {
                score += matchScore(comp, args[i]);
            }
            // prefer non-varargs slightly when tied
            score -= 1;
        } else {
            for (int i = 0; i < pt.length; i++) {
                score += matchScore(pt[i], args[i]);
            }
        }
        return score;
    }

    /** Exact match +2, assignable +1, null to primitive = -inf, else 0. */
    private static int matchScore(Class<?> paramType, Object arg) {
        if (arg == null) return paramType.isPrimitive() ? Integer.MIN_VALUE : 1;
        Class<?> a = arg.getClass();
        if (paramType.isPrimitive()) {
            Class<?> wrap = wrapperType(paramType);
            if (wrap == a) return 2;
            return (wrap.isAssignableFrom(a)) ? 1 : Integer.MIN_VALUE;
        } else {
            if (paramType == a) return 2;
            return (paramType.isAssignableFrom(a)) ? 1 : 0;
        }
    }

    private static Class<?> wrapperType(Class<?> primitive) {
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == byte.class)    return Byte.class;
        if (primitive == short.class)   return Short.class;
        if (primitive == char.class)    return Character.class;
        if (primitive == int.class)     return Integer.class;
        if (primitive == long.class)    return Long.class;
        if (primitive == float.class)   return Float.class;
        if (primitive == double.class)  return Double.class;
        return primitive; // not expected
    }

    /** Prepare arguments for reflective call, packaging varargs if necessary. */
    private static Object[] prepareArgsForVarargs(Method m, Object[] args) {
        if (!m.isVarArgs()) return args;

        Class<?>[] pt = m.getParameterTypes();
        int fixed = pt.length - 1;
        if (args.length == pt.length) {
            // Might already be passed as a single array for the vararg – let reflection handle it as-is.
            Object last = args[args.length - 1];
            if (last != null && last.getClass().isArray()
                    && pt[pt.length - 1].isAssignableFrom(last.getClass())) {
                return args;
            }
        }

        Object[] packed = new Object[pt.length];
        // copy fixed
        System.arraycopy(args, 0, packed, 0, Math.min(fixed, args.length));

        // build vararg array
        Class<?> comp = pt[pt.length - 1].getComponentType();
        int varCount = Math.max(0, args.length - fixed);
        Object varArray = Array.newInstance(comp, varCount);
        for (int i = 0; i < varCount; i++) {
            Array.set(varArray, i, (fixed + i) < args.length ? args[fixed + i] : null);
        }
        packed[pt.length - 1] = varArray;
        return packed;
    }

    /** Single-hop: read a direct field/getter on the given target. */
    public static Object getDirectProperty(Object target, String name) {
        if (target == null || name == null || name.isEmpty()) return null;

        // 1) Field by exact name (walks hierarchy)
        Field f = findField(target.getClass(), name);
        if (f != null) {
            try {
                if (!f.canAccess(target)) f.setAccessible(true);
                return f.get(target);
            } catch (ReflectiveOperationException ignore) { /* fall through */ }
        }

        // 2) Zero-arg method with exact name
        Method m = findZeroArgMethod(target.getClass(), name);
        if (m != null) {
            try {
                if (!m.canAccess(target)) m.setAccessible(true);
                return m.invoke(target);
            } catch (ReflectiveOperationException ignore) { /* fall through */ }
        }

        // 3) Bean-style getters
        String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Method bean = findZeroArgMethod(target.getClass(), "get" + cap);
        if (bean == null) bean = findZeroArgMethod(target.getClass(), "is" + cap);
        if (bean != null) {
            try {
                if (!bean.canAccess(target)) bean.setAccessible(true);
                return bean.invoke(target);
            } catch (ReflectiveOperationException ignore) { /* fall through */ }
        }

        return null;
    }

    /**
     * Multi-hop: supports dot-separated paths like "propA.propB.propC".
     * Each segment is resolved via getDirectProperty(..).
     */
    public static Object getProperty(Object target, String name) {
        if (target == null || name == null || name.isEmpty()) return null;

        Object current = target;
        String[] parts = name.split("\\.");
        for (String raw : parts) {
            String segment = raw.trim();
            if (segment.isEmpty()) continue;            // tolerate accidental ".."
            current = getDirectProperty(current, segment);
            if (current == null) return null;           // any miss -> null
        }
        return current;
    }

    // --- helpers (unchanged) ---
    private static Field findField(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException ignore) { /* keep walking */ }
        }
        return null;
    }

    public static Method findZeroArgMethod(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredMethod(name); }
            catch (NoSuchMethodException ignore) { /* keep walking */ }
        }
        return null;
    }


    public static String nameOf(Object target) {
        if (target == null) return null;
        Method m = findZeroArgMethod(target.getClass(), "getName");
        if (m == null) return null;
        try {
            if (!m.canAccess(target)) m.setAccessible(true);
            Object v = m.invoke(target);
            return (v instanceof String) ? (String) v : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Reflect() {}
}