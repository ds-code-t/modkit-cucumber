package tools.ds.modkit.mappings;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.lexer.token.OperatorType;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorObject;

import java.util.Map;

public final class AviatorUtil {

    static {
        // bool(x) helper
        AviatorEvaluator.addFunction(new BoolFn());

        // Override &&
        AviatorEvaluator.addOpFunction(OperatorType.AND, new AbstractFunction() {
            @Override public String getName() { return "&&"; }
            @Override
            public AviatorObject call(Map<String, Object> env, AviatorObject a, AviatorObject b) {
                if (!truthy(a.getValue(env))) return AviatorBoolean.FALSE; // short-circuit
                return AviatorBoolean.valueOf(truthy(b.getValue(env)));
            }
        });

        // Override ||
        AviatorEvaluator.addOpFunction(OperatorType.OR, new AbstractFunction() {
            @Override public String getName() { return "||"; }
            @Override
            public AviatorObject call(Map<String, Object> env, AviatorObject a, AviatorObject b) {
                if (truthy(a.getValue(env))) return AviatorBoolean.TRUE; // short-circuit
                return AviatorBoolean.valueOf(truthy(b.getValue(env)));
            }
        });
    }

    private AviatorUtil() { }

    /** Evaluates an object by converting it to a String and running it through Aviator. */
    public static Object eval(Object expr, Map<String, Object> map) {
        return expr == null ? null : AviatorEvaluator.execute(expr.toString(), map);
    }

    /** Evaluates an object to boolean, using Aviator's final-result coercion. */
    public static boolean evalToBoolean(Object expr, Map<String, Object> map) {
        return expr != null && (boolean) AviatorEvaluator.execute(expr.toString(), map, true);
    }

    /** Shared truthiness (numbers: non-zero; strings/collections/maps: non-empty). */
    private static boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0.0;
        if (v instanceof CharSequence s) return s.length() > 0;
        if (v instanceof java.util.Collection<?> c) return !c.isEmpty();
        if (v instanceof java.util.Map<?, ?> m) return !m.isEmpty();
        return true;
    }

    /** bool(x) for use inside expressions. */
    public static class BoolFn extends AbstractFunction {
        @Override public String getName() { return "bool"; }
        @Override public AviatorObject call(Map<String, Object> env, AviatorObject x) {
            return AviatorBoolean.valueOf(truthy(x.getValue(env)));
        }
    }
}
