package tools.ds.modkit;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;

/** ByteBuddy MethodDelegation target that routes invocations through the registry. */
public final class GenericOverrideInterceptor {

    private static final OverrideRegistry REG = OverrideRegistry.get();
    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("modkit.debug", "true"));

    @RuntimeType
    public static Object intercept(
            @This(optional = true) Object self,
            @AllArguments Object[] args,
            @SuperCall Callable<?> zuper,
            @Origin Method origin
    ) throws Throwable {

        // Look up override; if none — no logs, just run original.
        MethodOverride ovr = REG.findFor(origin);

        if (ovr != null && DEBUG) {
            System.err.println("[modkit:route] OVERRIDDEN " +
                    origin.getDeclaringClass().getName() + "#" + origin.getName() +
                    "(" + Arrays.toString(origin.getParameterTypes()) + ")");
        }

        if (ovr != null) {
            return ovr.invoke(self, args, zuper, origin);
        }
        return zuper.call();
    }
}
