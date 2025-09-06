//package tools.ds.modkit.builtin;
//
//import net.bytebuddy.agent.builder.AgentBuilder;
//import net.bytebuddy.asm.Advice;
//import tools.ds.modkit.ModPatch;
//
//import java.lang.instrument.Instrumentation;
//
//import static net.bytebuddy.matcher.ElementMatchers.*;
//
//public final class MessagesPicklePatch implements ModPatch {
//    private static final String TARGET = "io.cucumber.messages.types.Pickle";
//
//    @Override
//    public String id() { return "MessagesPickle"; }
//
//    @Override
//    public AgentBuilder apply(AgentBuilder base, Instrumentation inst) {
//        AgentBuilder builder = base
//                .type(named(TARGET))
//                .transform((b, td, cl, module, pd) ->
//                        // Intercept getName() to prefix the returned value.
//                        b.method(named("getName").and(takesArguments(0)).and(returns(String.class)))
//                                .intercept(Advice.to(GetNameAdvice.class))
//                );
//
//        // Optional: also patch the constructor to change the 'name' argument at construction time.
//        // Enable with -Dmodkit.messagesPickle.patchCtor=true
//        if (Boolean.parseBoolean(System.getProperty("modkit.messagesPickle.patchCtor", "false"))) {
//            builder = builder
//                    .type(named(TARGET))
//                    .transform((b, td, cl, module, pd) ->
//                            b.constructor(takesArguments(7)) // (String id, String uri, String name, String language, List steps, List tags, List astNodeIds)
//                                    .intercept(Advice.to(CtorAdvice.class))
//                    );
//        }
//
//        return builder;
//    }
//
//    @Override
//    public void eagerRetransform(Instrumentation inst) {
//        if (inst == null || !inst.isRetransformClassesSupported()) return;
//        for (Class<?> c : inst.getAllLoadedClasses()) {
//            if (TARGET.equals(c.getName()) && inst.isModifiableClass(c)) {
//                try { inst.retransformClasses(c); } catch (Throwable ignore) { }
//            }
//        }
//    }
//
//    /** Advice to modify the *returned* name (no field changes). */
//    public static final class GetNameAdvice {
//        @Advice.OnMethodExit(onThrowable = Throwable.class)
//        static void exit(@Advice.Return(readOnly = false) String name) {
//            String prefix = System.getProperty("modkit.namePrefix", "AA ");
//            if (name != null && prefix != null && !prefix.isEmpty() && !name.startsWith(prefix)) {
//                name = prefix + name;
//            }
//        }
//    }
//
//    /**
//     * Optional constructor advice that *changes the field* by modifying the third argument ('name')
//     * before the original constructor runs. Enable with -Dmodkit.messagesPickle.patchCtor=true
//     */
//    public static final class CtorAdvice {
//        @Advice.OnMethodEnter
//        static void enter(@Advice.Argument(value = 2, readOnly = false) String nameArg) {
//            String prefix = System.getProperty("modkit.namePrefix", "★ ");
//            if (nameArg != null && prefix != null && !prefix.isEmpty() && !nameArg.startsWith(prefix)) {
//                nameArg = prefix + nameArg;
//            }
//        }
//    }
//}
