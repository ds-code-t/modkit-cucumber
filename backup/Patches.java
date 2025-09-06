//package tools.ds.modkit;
//
//import net.bytebuddy.agent.builder.AgentBuilder;
//import net.bytebuddy.asm.Advice;
//import net.bytebuddy.description.method.MethodDescription;
//import net.bytebuddy.matcher.ElementMatcher;
//
//import static net.bytebuddy.matcher.ElementMatchers.*;
//
///** Small helpers around AgentBuilder usage. */
//public final class Patches {
//    private Patches() {}
//
//    /** Common ignores; keep the type as AgentBuilder for fluent subtypes. */
//    public static AgentBuilder configureBase(AgentBuilder base) {
//        return base.ignore(
//                nameStartsWith("net.bytebuddy.")
//                        .or(nameStartsWith("org.junit."))
//                        .or(nameStartsWith("org.slf4j."))
//                        .or(nameStartsWith("ch.qos.logback."))
//                        .or(isSynthetic())
//        );
//    }
//
//    /**
//     * Match any type that implements/extends {@code interfaceOrBaseType} and
//     * weave {@code adviceClass} into methods that match {@code methodMatcher}.
//     */
//    public static AgentBuilder adviseMethodOnImplementors(
//            AgentBuilder builder,
//            String interfaceOrBaseType,
//            ElementMatcher<? super MethodDescription> methodMatcher,
//            Class<?> adviceClass) {
//
//        return builder
//                .type(hasSuperType(named(interfaceOrBaseType)))
//                .transform((b, typeDesc, cl, module, pd) ->
//                        b.method(methodMatcher).intercept(Advice.to(adviceClass))
//                );
//    }
//}
