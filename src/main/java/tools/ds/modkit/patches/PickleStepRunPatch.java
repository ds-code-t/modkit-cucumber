// src/main/java/tools/ds/modkit/patches/PickleStepRunPatch.java
package tools.ds.modkit.patches;

import io.cucumber.core.backend.TestCaseState;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.plugin.event.TestCase;
import tools.ds.modkit.blackbox.Registry;
import tools.ds.modkit.util.CallScope;

import static tools.ds.modkit.blackbox.Plans.on;
import static tools.ds.modkit.state.ScenarioState.beginNew;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.trace.ObjDataRegistry.*;
import static tools.ds.modkit.util.ExecutionModes.RUN;

public final class PickleStepRunPatch {
    private PickleStepRunPatch() {}

    /** Register the step-run interception. */
    public static void register() {
        Registry.register(
                on("io.cucumber.core.runner.PickleStepTestStep", "run", 4)
                        .returns("io.cucumber.core.runner.ExecutionMode")

                        .before(args -> {
                            System.out.println("@@before");
                        })

                        .around(
                                args -> {
                                    System.out.println("@@around");

                                    Object testCase = args[0]; // io.cucumber.core.runner.TestCase (non-public)
                                    ObjFlags st = getFlag(testCase);
                                    boolean skip = true;
                                    if (st.equals(ObjFlags.NOT_SET)) {
//
//                    testCase, bus, state, nativeExecutionMode);
                                        beginNew((TestCase) testCase, (EventBus) args[1], (TestCaseState) args[2]);
                                        setFlag(testCase, ObjFlags.INITIALIZING);
                                    } else if (st.equals(ObjFlags.RUNNING)) {
                                        skip = false;
                                    }
                                 //                                    return false;
                                    return skip; // true => skip original
                                },
                                args -> {
                                    Object self = CallScope.currentSelf();
                                    if(containsFlags(self, ObjFlags.LAST))
                                    {
                                        getScenarioState().getStepExecution().runSteps(RUN(args[2]));
                                    }
                                    return args[3];
                                }
                        )

                        .after((args, ret, thr) -> {
                            System.out.println("@@after");
                            return ret;
                        })

                        .build()
        );
    }
}
