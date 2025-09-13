package tools.ds.modkit.executions;

import io.cucumber.core.backend.TestCaseState;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;
import tools.ds.modkit.extensions.StepExtension;
import tools.ds.modkit.mappings.ParsingMap;
import tools.ds.modkit.status.SoftException;
import tools.ds.modkit.status.SoftRuntimeException;
import tools.ds.modkit.trace.ObjDataRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tools.ds.modkit.state.GlobalState.getRuntime;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.trace.ObjDataRegistry.setFlag;
import static tools.ds.modkit.util.ExecutionModes.RUN;
import static tools.ds.modkit.util.Reflect.getProperty;
import static tools.ds.modkit.util.Reflect.invokeAnyMethod;


public class StepExecution {
    public final List<StepExtension> steps = new ArrayList<>();

//    public boolean isRunComplete() {
//        return runComplete;
//    }
//
//    public void endRun(boolean runComplete) {
//        this.runComplete = runComplete;
//    }
//
//    private boolean runComplete = false;

    public StepExecution(TestCase testCase) {
        System.out.println("@@StepExecutions:");
        List<PickleStepTestStep> pSteps = (List<PickleStepTestStep>) getProperty(testCase, "testSteps");
        setFlag(pSteps.get(pSteps.size() - 1), ObjDataRegistry.ObjFlags.LAST);
        pSteps.forEach(step -> steps.add(new StepExtension(step, this)));
        System.out.println("@@pSteps: " + pSteps.size());
        int size = steps.size();


        Map<Integer, StepExtension> nestingMap = new HashMap<>();

        int lastNestingLevel = 0;


        for (int s = 0; s < size; s++) {
            StepExtension currentStep = steps.get(s);
            int currentNesting = currentStep.nestingLevel;


            StepExtension parentStep = nestingMap.get(currentNesting - 1);


            StepExtension previousSibling = currentNesting > lastNestingLevel ? null : nestingMap.get(currentNesting);

            if (previousSibling != null) {
                previousSibling.nextSibling = currentStep;
                currentStep.previousSibling = previousSibling;
            }
            if (parentStep != null) {
                currentStep.parentStep = parentStep;
                parentStep.childSteps.add(currentStep);
            }
            System.out.println("@@000 currentStep: " + currentStep.getStepText());
            System.out.println("@@000 parentStep: " + (parentStep == null ? "END" : parentStep.getStepText()));

            nestingMap.put(currentNesting, currentStep);
//            System.out.println("@@000 previousSibling: " + (previousSibling == null || previousSibling.nextSibling == null ? "END" : previousSibling.nextSibling.getStepText()));
            lastNestingLevel = currentNesting;

        }
    }


    public void runSteps(Object executionMode) {
        runSteps(getScenarioState().testCase, getScenarioState().bus, getScenarioState().getTestCaseState(), executionMode);
    }

    private boolean scenarioHardFail = false;
    private boolean scenarioSoftFail = false;
    private boolean scenarioComplete = false;

    public boolean isScenarioFailed() {
        return scenarioHardFail || scenarioSoftFail;
    }

    public boolean isScenarioHardFail() {
        return scenarioHardFail;
    }
    public boolean isScenarioSoftFail() {
        return scenarioSoftFail;
    }
    public void setScenarioHardFail() {
        this.scenarioHardFail = true;
        setScenarioComplete();
    }
    public void setScenarioSoftFail() {
        this.scenarioSoftFail = true;
    }

    public void setScenarioComplete() {
        this.scenarioComplete = true;
    }
    public boolean isScenarioComplete() {
        return this.scenarioComplete;
    }


    public void runSteps(TestCase testCase, EventBus bus, TestCaseState state, Object executionMode) {

        System.out.println("@@runSteps");

        System.out.println("@@gherkinView: " + getScenarioState().gherkinView);
        System.out.println("\n@@getRuntimeOptions: " + getScenarioState().getRuntimeOptions());
//        System.out.println("\n@@getTagExpressions(): " +     getScenarioState().getTagExpressions());
        System.out.println("\n@@getTags(): " + getScenarioState().getTags());
//        System.out.println("\n@@getRuntime(): " +     getScenarioState().getRuntime());
        System.out.println("\n@@getRuntime(): " + getRuntime());



        setFlag(testCase, ObjDataRegistry.ObjFlags.RUNNING);

        StepExtension currentStep = steps.get(0);
        System.out.println("@@### runsteps!! " + currentStep.getStepText());
        while (currentStep != null) {
            currentStep.run(testCase, bus, state, executionMode);
            currentStep = currentStep.nextSibling;
        }


//
//        for (StepExtension step : steps) {
//            System.out.println("@@step: " + step.getStepText());
//            StepExtension newStep = step.updateStep(parsingMap);
//            newStep.run(testCase, bus, state, executionMode);
//            if (newStep.result.getStatus().equals(Status.PASSED))
//                continue;
//            Throwable throwable = newStep.result.getError();
//            if (throwable.getClass().equals(SoftException.class) || throwable.getClass().equals(SoftRuntimeException.class))
//                continue;
//            System.out.println("@@newStep.result: " + newStep.result);
//            break;
//        }

    }


}
