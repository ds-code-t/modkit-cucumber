//package tools.ds.modkit.builtin;
//
//import net.bytebuddy.agent.builder.AgentBuilder;
//import tools.ds.modkit.ModPatch;
//import tools.ds.modkit.box.Rules;
//import tools.ds.modkit.box.Weaver;
//import tools.ds.modkit.adv.GenericAdvices;
//
//import java.lang.instrument.Instrumentation;
//import java.util.List;
//
//public final class UniversalCucumberPatch implements ModPatch {
//    @Override public String id() { return "UniversalCucumberPatch"; }
//
//    @Override
//    public AgentBuilder apply(AgentBuilder base, Instrumentation inst) {
//        // Declare ALL your targets & methods here (data-like).
//        List<Rules.TypeRule> rules = List.of(
//
//
//
//                Rules.TypeRule.of("io.cucumber.messages.types.Scenario")
//                        .method("getName", 0, "java.lang.String", GenericAdvices.StringReturnPrefixAdvice.class),
//
//
//                // 1) The generated messages Pickle class
//                Rules.TypeRule.of("io.cucumber.messages.types.Pickle")
//                        .method("getName", 0, "java.lang.String", GenericAdvices.StringReturnPrefixAdvice.class)
////                        .method("getId",   0, "java.lang.String", GenericAdvices.StringReturnSuffixAdvice.class)
//                // optional: mutate field via ctor (arg #2 = name)
//                // .ctor(7, GenericAdvices.CtorStringArg2PrefixAdvice.class)
//
//                ,
//                // 2) The concrete runtime wrapper you showed earlier
//                Rules.TypeRule.of("io.cucumber.core.gherkin.messages.GherkinMessagesPickle")
//                        .method("getName",  0, "java.lang.String", GenericAdvices.StringReturnPrefixAdvice.class)
////                        .method("getSteps", 0, null,               GenericAdvices.EmptyListReturnAdvice.class)
////                        .method("getId",    0, "java.lang.String", GenericAdvices.StringReturnSuffixAdvice.class)
//        );
//
//        // Keep the rules around for eager retransform in eagerRetransform(...)
//        this.rules = rules;
//
//        // Apply to a SINGLE AgentBuilder:
//        return Weaver.apply(base, rules, UniversalCucumberPatch.class.getClassLoader());
//    }
//
//    @Override
//    public void eagerRetransform(Instrumentation inst) {
//        Weaver.eagerRetransform(inst, rules);
//    }
//
//    // state to reuse in eagerRetransform
//    private List<Rules.TypeRule> rules;
//}
