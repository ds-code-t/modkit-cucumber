package tools.ds.modkit;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/** Functional interface for overriding any method via the router. */
@FunctionalInterface
public interface MethodOverride {
    Object invoke(Object self, Object[] args, Callable<?> zuper, Method origin) throws Throwable;
}
