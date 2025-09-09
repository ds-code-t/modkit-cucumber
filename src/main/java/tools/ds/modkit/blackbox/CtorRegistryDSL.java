// src/main/java/tools/ds/modkit/blackbox/CtorRegistryDSL.java
package tools.ds.modkit.blackbox;

import tools.ds.modkit.trace.InstanceRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CtorRegistryDSL {
    private CtorRegistryDSL() {
    }

    /* ================== Public API (thread-local) ================== */

    /**
     * Register ctor hooks for the given targets, storing instances in the per-thread registry.
     */
    public static void threadRegisterConstructed(List<?> targets, Object... extraKeys) {
        System.out.println("@@threadRegisterConstructed " + targets);
        registerCommon(targets, false, extraKeys);
    }

    public static void threadRegisterConstructed(Class<?>... targets) {
        threadRegisterConstructed(Arrays.asList((Object[]) targets));
    }

    public static void threadRegisterConstructed(String... targets) {
        threadRegisterConstructed(Arrays.asList((Object[]) targets));
    }

    /* ================== Public API (global) ================== */

    /**
     * Register ctor hooks for the given targets, storing instances in the global registry.
     */
    public static void globalRegisterConstructed(List<?> targets, Object... extraKeys) {
        registerCommon(targets, true, extraKeys);
    }

    public static void globalRegisterConstructed(Class<?>... targets) {
        globalRegisterConstructed(Arrays.asList((Object[]) targets));
    }

    public static void globalRegisterConstructed(String... targets) {
        globalRegisterConstructed(Arrays.asList((Object[]) targets));
    }

    /* ================== Internals ================== */

    private static void registerCommon(List<?> targets, boolean global, Object... extraKeys) {
        if (targets == null || targets.isEmpty()) return;

        for (Object t : targets) {
            final String fqcn = toFqcn(t);
            System.out.println("@@target: " + t);
            System.out.println("@@fqcn: " + fqcn);
            if (fqcn == null || fqcn.isEmpty()) continue;

            Registry.register(
                    Plans.onCtor(fqcn, 0) // arg count is irrelevant for afterInstance (Weaver matches all ctors)
                            .afterInstance(self -> {
                                System.out.println("@@register ctor self: " + self);
                                System.out.println("@@extraKeys: " + Arrays.asList(extraKeys));
                                if (self == null) return;

                                // Build keys: Class, FQCN, plus any extras provided by the caller
                                List<Object> keys = new ArrayList<>(2 + extraKeys.length);
                                keys.add(self.getClass());
                                keys.add(self.getClass().getName());
                                System.out.println("@@keys: " + keys);
                                for (Object k : extraKeys) System.out.println("@@k:: " + k);
                                for (Object k : extraKeys) if (k != null) keys.add(k);

                                Object[] arr = keys.toArray();
                                if (global) {
                                    InstanceRegistry.globalRegister(self, arr);
                                } else {
                                    System.out.println("@@registering-self:  " + self);
                                    System.out.println("@@registering-arr:  " + Arrays.asList(arr));
                                    InstanceRegistry.register(self, arr);
                                }
                            })
                            .build()
            );
        }
    }

    /**
     * Accepts Class, String (FQCN), or any object (uses its runtime class).
     */
    private static String toFqcn(Object t) {
        if (t == null) return null;
        if (t instanceof Class<?>) return ((Class<?>) t).getName();
        if (t instanceof CharSequence) return t.toString();
        return t.getClass().getName();
    }
}
