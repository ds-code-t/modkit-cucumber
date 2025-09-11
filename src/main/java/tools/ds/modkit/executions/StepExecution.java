package tools.ds.modkit.executions;

import io.cucumber.core.backend.TestCaseState;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestCase;
import tools.ds.modkit.extensions.StepExtension;
import tools.ds.modkit.mappings.ParsingMap;
import tools.ds.modkit.trace.ObjDataRegistry;

import java.util.ArrayList;
import java.util.List;

import static tools.ds.modkit.state.GlobalState.getRuntime;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.trace.ObjDataRegistry.setFlag;
import static tools.ds.modkit.util.ExecutionModes.RUN;
import static tools.ds.modkit.util.Reflect.getProperty;
import static tools.ds.modkit.util.Reflect.invokeAnyMethod;


public class StepExecution {
    public final List<StepExtension> steps = new ArrayList<>();



    public StepExecution(TestCase testCase) {
        System.out.println("@@StepExecutions:");
        List<PickleStepTestStep> pSteps = (List<PickleStepTestStep>) getProperty(testCase, "testSteps");
        setFlag(pSteps.get(pSteps.size() - 1), ObjDataRegistry.ObjFlags.LAST);
        pSteps.forEach(step -> steps.add(new StepExtension(step)));
        System.out.println("@@pSteps: " + pSteps.size());
    }


    public void runSteps(Object executionMode){
        runSteps(getScenarioState().testCase, getScenarioState().bus,getScenarioState().getTestCaseState(), executionMode);
    }


    public void runSteps(TestCase testCase, EventBus bus, TestCaseState state, Object executionMode){
        System.out.println("@@runSteps");

        System.out.println("@@gherkinView: " +getScenarioState().gherkinView);
        System.out.println("\n@@getRuntimeOptions: " +     getScenarioState().getRuntimeOptions());
//        System.out.println("\n@@getTagExpressions(): " +     getScenarioState().getTagExpressions());
        System.out.println("\n@@getTags(): " +     getScenarioState().getTags());
//        System.out.println("\n@@getRuntime(): " +     getScenarioState().getRuntime());
        System.out.println("\n@@getRuntime(): " +     getRuntime());


        ParsingMap parsingMap = getScenarioState().getTestMap();
        setFlag(testCase, ObjDataRegistry.ObjFlags.RUNNING);
        for(StepExtension step: steps)
        {
            System.out.println("@@step: " + step.getStepText());
            step.updateStep(parsingMap).run(testCase,  bus,  state, executionMode) ;
//            invokeAnyMethod(step, "run", testCase,  bus,  state, executionMode) ;
        }
    }


}
