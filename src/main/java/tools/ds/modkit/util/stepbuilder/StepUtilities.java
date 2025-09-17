package tools.ds.modkit.util.stepbuilder;

import io.cucumber.core.gherkin.Pickle;
import io.cucumber.core.runner.Runner;
import io.cucumber.plugin.event.PickleStepTestStep;
import tools.ds.modkit.util.Reflect;

import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.util.Reflect.invokeAnyMethod;

public class StepUtilities {

    public static Object matchStepToStepDefinition(Runner runner, Pickle pickle, io.cucumber.core.gherkin.Step step) {
        return invokeAnyMethod(runner, "matchStepToStepDefinition", pickle, step);
    }


    public static PickleStepTestStep createPickleStepTestStep(io.cucumber.core.gherkin.Step newStep, java.util.UUID uuid, java.net.URI uri) {
        Object pickleStepDefinitionMatch = getDefinition(getScenarioState().getRunner(), getScenarioState().getScenarioPickle(), newStep);
        return (PickleStepTestStep) Reflect.newInstance(
                "io.cucumber.core.runner.PickleStepTestStep",
                uuid,             // java.util.UUID
                uri,                // java.net.URI
                newStep,        // io.cucumber.core.gherkin.Step (public)
                pickleStepDefinitionMatch            // io.cucumber.core.runner.PickleStepDefinitionMatch (package-private instance is fine)
        );
    }

    public static Object getDefinition(Runner runner, io.cucumber.core.gherkin.Pickle scenarioPickle, io.cucumber.core.gherkin.Step step) {
        return matchStepToStepDefinition(runner, scenarioPickle, step);
    }


}
