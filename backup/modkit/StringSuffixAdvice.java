package tools.ds.modkit;

import net.bytebuddy.asm.Advice;

/** Appends a suffix to any zero-arg, String-returning instance method. */
public final class StringSuffixAdvice {

    private static final String SUFFIX = " [instrumented]";

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.Return(readOnly = false) String ret) {
        if (ret != null && !ret.endsWith(SUFFIX)) {
            ret = ret + SUFFIX;
        }
    }
}
