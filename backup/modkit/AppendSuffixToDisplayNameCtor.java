package tools.ds.modkit;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Field;

/**
 * Best-effort: after constructing a JUnit AbstractTestDescriptor, try to append
 * our suffix to its cached 'displayName' field so IDE UIs that read the field
 * directly (or cache early) also show the change.
 *
 * Note: In newer JDKs the field is final; setting it may be rejected. We swallow
 * any errors — method advice on getDisplayName()/getLegacyReportingName() still
 * applies as a fallback.
 */
public final class AppendSuffixToDisplayNameCtor {

    private static final String SUFFIX = " [instrumented]";

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void after(@Advice.This Object self) {
        try {
            // Walk up the class hierarchy to find a 'displayName' field
            Class<?> c = self.getClass();
            Field f = null;
            while (c != null) {
                try {
                    f = c.getDeclaredField("displayName");
                    break;
                } catch (NoSuchFieldException ignored) {
                    c = c.getSuperclass();
                }
            }
            if (f == null) return;

            f.setAccessible(true);
            Object v = f.get(self);
            if (v instanceof String s && !s.endsWith(SUFFIX)) {
                // This may no-op on some JVMs if the field is final; it's OK.
                f.set(self, s + SUFFIX);
            }
        } catch (Throwable ignored) {
            // Best effort only; getters are still advised.
        }
    }
}
