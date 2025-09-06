//package tools.ds.modkit.builtin;
//
//import net.bytebuddy.agent.builder.AgentBuilder;
//import tools.ds.modkit.ModPatch;
//import tools.ds.modkit.Patches;
//
//import java.lang.instrument.Instrumentation;
//
//import static net.bytebuddy.matcher.ElementMatchers.named;
//import static net.bytebuddy.matcher.ElementMatchers.returns;
//import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
//
///**
// * Demo patch:
// *  - intercepts zero-arg getName() on implementations of io.cucumber.core.gherkin.Pickle
// *  - prefixes the returned scenario name
// */
//public final class PickleNamePatch implements ModPatch {
//    @Override
//    public String id() { return "Pickle#getName"; }
//
//    @Override
//    public AgentBuilder apply(AgentBuilder base, Instrumentation inst) {
//
//        return Patches.adviseMethodOnImplementors(
//                base,
//                "io.cucumber.core.gherkin.Pickle",
//                named("getName").and(takesArguments(0)).and(returns(String.class)),
//                PickleNameAdvice.class
//        );
//    }
//}
