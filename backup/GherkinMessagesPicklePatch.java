//package tools.ds.modkit.builtin;
//
//import net.bytebuddy.agent.builder.AgentBuilder;
//import net.bytebuddy.asm.Advice;
//import tools.ds.modkit.ModPatch;
//
//import java.lang.instrument.Instrumentation;
//import java.util.Collections;
//
//import static net.bytebuddy.matcher.ElementMatchers.*;
//
//public final class GherkinMessagesPicklePatch implements ModPatch {
//    private static final String TARGET = "io.cucumber.core.gherkin.messages.GherkinMessagesPickle";
//
//    @Override
//    public String id() { return "GherkinMessagesPickle"; }
//
//    @Override
//    public AgentBuilder apply(AgentBuilder base, Instrumentation inst) {
//        // Match the concrete class directly and advise multiple methods.
//        return base
//                .type(named(TARGET))
//                .transform((b, td, cl, module, pd) ->
//                        b
//                                // getName(): prefix the name so it’s obvious we patched it
//                                .method(named("getName").and(takesArguments(0)).and(returns(String.class)))
//                                .intercept(Advice.to(NameAdvice.class))
////
//////                                // getSteps(): optionally break things by returning empty list
////                                .method(named("getSteps").and(takesArguments(0)))
////                                .intercept(Advice.to(StepsAdvice.class))
//////
////                                // getId(): suffix to prove interception across multiple getters
////                                .method(named("getId").and(takesArguments(0)).and(returns(String.class)))
////                                .intercept(Advice.to(IdAdvice.class))
//                );
//    }
//
//    @Override
//    public void eagerRetransform(Instrumentation inst) {
//        // If the class already loaded before our install, retransform it now.
//        if (inst == null || !inst.isRetransformClassesSupported()) return;
//        for (Class<?> c : inst.getAllLoadedClasses()) {
//            if (TARGET.equals(c.getName()) && inst.isModifiableClass(c)) {
//                try { inst.retransformClasses(c); } catch (Throwable ignore) { }
//            }
//        }
//    }
//
//    /** Advices **/
//
//    public static final class NameAdvice {
//        @Advice.OnMethodExit(onThrowable = Throwable.class)
//        static void exit(@Advice.Return(readOnly = false) String name) {
//            String prefix = System.getProperty("modkit.namePrefix", "★ ");
//            if (name != null && prefix != null && !prefix.isEmpty() && !name.startsWith(prefix)) {
//                name = prefix + name;
//            }
//        }
//    }
//
//    public static final class StepsAdvice {
//        @Advice.OnMethodExit(onThrowable = Throwable.class)
//        static void exit(@Advice.Return(readOnly = false) java.util.List<?> steps) {
//            // Toggle: -Dmodkit.breakSteps=true will return no steps (obvious proof; may break the run)
//            boolean breakSteps = Boolean.parseBoolean(System.getProperty("modkit.breakSteps", "false"));
//            if (breakSteps) {
//                steps = Collections.emptyList();
//            }
//        }
//    }
//
//    public static final class IdAdvice {
//        @Advice.OnMethodExit(onThrowable = Throwable.class)
//        static void exit(@Advice.Return(readOnly = false) String id) {
//            String suffix = System.getProperty("modkit.idSuffix", "::mod");
//            if (id != null && suffix != null && !id.endsWith(suffix)) {
//                id = id + suffix;
//            }
//        }
//    }
//}
