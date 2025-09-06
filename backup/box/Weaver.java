//package tools.ds.modkit.box;
//
//import net.bytebuddy.agent.builder.AgentBuilder;
//import net.bytebuddy.asm.Advice;
//
//import java.lang.instrument.Instrumentation;
//import java.util.List;
//
//import static net.bytebuddy.matcher.ElementMatchers.*;
//
//public final class Weaver {
//    private Weaver() {}
//
//    public static AgentBuilder apply(AgentBuilder base, List<Rules.TypeRule> rules, ClassLoader resolverCl) {
//        AgentBuilder b = base;
//        for (Rules.TypeRule tr : rules) {
//            b = b.type(named(tr.typeName))
//                    .transform((builder, td, cl, module, pd) -> {
//                        // methods
//                        for (Rules.MethodRule mr : tr.methods) {
//                            var m = named(mr.name).and(takesArguments(mr.argCount));
//                            if (mr.returnTypeFqcn != null) {
//                                Class<?> rt = tryResolve(mr.returnTypeFqcn, resolverCl);
//                                if (rt != null) m = m.and(returns(rt));
//                            }
//                            builder = builder.method(m).intercept(Advice.to(mr.adviceClass));
//                        }
//                        // ctors
//                        for (Rules.CtorRule cr : tr.ctors) {
//                            builder = builder.constructor(takesArguments(cr.argCount))
//                                    .intercept(Advice.to(cr.adviceClass));
//                        }
//                        return builder;
//                    });
//        }
//        return b;
//    }
//
//    public static void eagerRetransform(Instrumentation inst, List<Rules.TypeRule> rules) {
//        if (inst == null || !inst.isRetransformClassesSupported()) return;
//        var wanted = rules.stream().map(r -> r.typeName).distinct().toArray(String[]::new);
//        for (Class<?> c : inst.getAllLoadedClasses()) {
//            for (String fqcn : wanted) {
//                if (fqcn.equals(c.getName()) && inst.isModifiableClass(c)) {
//                    try { inst.retransformClasses(c); } catch (Throwable ignore) {}
//                }
//            }
//        }
//    }
//
//    private static Class<?> tryResolve(String fqcn, ClassLoader cl) {
//        if (fqcn == null) return null;
//        try { return Class.forName(fqcn, false, cl); } catch (Throwable t) { return null; }
//    }
//}
