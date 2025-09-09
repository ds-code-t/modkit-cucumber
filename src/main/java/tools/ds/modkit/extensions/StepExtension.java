package tools.ds.modkit.extensions;

import io.cucumber.core.backend.TestCaseState;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.runner.Runner;
import io.cucumber.gherkin.GherkinDialects;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleStepArgument;
import io.cucumber.plugin.event.*;
import tools.ds.modkit.mappings.ParsingMap;
import tools.ds.modkit.util.PickleStepArgUtils;
import tools.ds.modkit.util.Reflect;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static tools.ds.modkit.blackbox.BlackBoxBootstrap.metaFlag;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.util.Reflect.getProperty;
import static tools.ds.modkit.util.Reflect.invokeAnyMethod;
//import static tools.ds.modkit.util.stepbuilder.GherkinMessagesStepBuilder.cloneWithPickleStep;
import static tools.ds.modkit.util.stepbuilder.StepUtilities.matchStepToStepDefinition;

public class StepExtension implements PickleStepTestStep {

    public final PickleStepTestStep delegate;
    public final PickleStep rootStep;
    public final io.cucumber.core.gherkin.Step gherikinMessageStep;
    public final String rootText;
    public final String metaData;
//    public final ParsingMap parsingMap;

    public StepExtension(PickleStepTestStep step) {
        this.delegate = step;
        this.gherikinMessageStep = (io.cucumber.core.gherkin.Step) getProperty(delegate, "step");
        this.rootStep = (PickleStep) getProperty(gherikinMessageStep, "pickleStep");
        System.out.println("@@delegate - getStepText: "  + delegate.getStepText());
        System.out.println("@@rootStep - delegate class: " +   invokeAnyMethod(delegate, "getClass"));
        System.out.println("@@rootStep - gherikinMessageStep class: " +   invokeAnyMethod(gherikinMessageStep, "getClass"));
        System.out.println("@@rootStep - rootStep class: " +   invokeAnyMethod(rootStep, "getClass"));
        System.out.println("@@rootStep: " +   rootStep);
        System.out.println("@@rootStep - text: " + getProperty(rootStep, "text"));
        String[] strings = ((String) getProperty(rootStep, "text")).split(metaFlag);
        rootText = strings[0].trim();
        metaData = strings.length == 1 ? "" : strings[1].trim();

    }

    public StepExtension updateStep(ParsingMap parsingMap) {
        PickleStepArgument argument = rootStep.getArgument().orElse(null);
        UnaryOperator<String> external = parsingMap::resolveWholeText;
        System.out.println("@@argument: " + argument);
        PickleStepArgument newPickleStepArgument = PickleStepArgUtils.transformPickleArgument(argument, external);
        PickleStep pickleStep = new PickleStep(newPickleStepArgument, rootStep.getAstNodeIds(), rootStep.getId(), rootStep.getType().orElse(null), parsingMap.resolveWholeText(rootStep.getText()));
        System.out.println("@@: getScenarioState().getPickle(): " + getScenarioState().getScenarioPickle());
        System.out.println("@@: getScenarioState().getTEstCase(): " + getScenarioState().getTestCaseName());
        System.out.println("@@: getScenarioState().getTEstCase(): " + getScenarioState().getTestCase());
//        io.cucumber.core.gherkin.messages.GherkinMessagesStep
        System.out.println("\n--------\n---@@pickleStep: " + pickleStep.getClass());
        System.out.println("@@GherkinDialects.getDialect(getScenarioState().getPickleLanguage()): " + GherkinDialects.getDialect(getScenarioState().getPickleLanguage()));
        System.out.println("@@ gherikinMessageStep.getPreviousGivenWhenThenKeyword(): " +  gherikinMessageStep.getPreviousGivenWhenThenKeyword());
        System.out.println("@@ gherikinMessageStep.getLocation(): " +  gherikinMessageStep.getLocation());
        System.out.println("@@gherikinMessageStep.getKeyword(): " + gherikinMessageStep.getKeyword());
        io.cucumber.core.gherkin.Step newGherikinMessageStep = (io.cucumber.core.gherkin.Step) Reflect.newInstance(
                "io.cucumber.core.gherkin.messages.GherkinMessagesStep",
                pickleStep,
                GherkinDialects.getDialect(getScenarioState().getPickleLanguage()).orElse(GherkinDialects.getDialect("en").get()),
                gherikinMessageStep.getPreviousGivenWhenThenKeyword(),
                gherikinMessageStep.getLocation(),
                gherikinMessageStep.getKeyword()
        );

        System.out.println("@@newGherikinMessageStepgetClass: " + newGherikinMessageStep.getClass());
        Object pickleStepDefinitionMatch  = getUpdatedDefinition(getScenarioState().getRunner(),getScenarioState().getScenarioPickle() ,newGherikinMessageStep);
        System.out.println("@@pickleStepDefinitionMatch: " + pickleStepDefinitionMatch);
        return new StepExtension((PickleStepTestStep) Reflect.newInstance(
                "io.cucumber.core.runner.PickleStepTestStep",
                getId(),               // java.util.UUID
                getUri(),                // java.net.URI
                newGherikinMessageStep,        // io.cucumber.core.gherkin.Step (public)
                pickleStepDefinitionMatch            // io.cucumber.core.runner.PickleStepDefinitionMatch (package-private instance is fine)
        ));

    }

    public static Object getUpdatedDefinition(Runner runner, io.cucumber.core.gherkin.Pickle scenarioPickle, io.cucumber.core.gherkin.Step step) {
        System.out.println("\n##@@runner: " + runner);
        System.out.println("##@@scenarioPickle: " + scenarioPickle);
        System.out.println("##@@step: " + step);
        return matchStepToStepDefinition(runner, scenarioPickle, step);
    }


    public Object run(TestCase testCase, EventBus bus, TestCaseState state, Object executionMode) {
        return invokeAnyMethod(delegate, "run", testCase, bus, state, executionMode);
    }


    @Override
    public String getPattern() {
        return delegate.getPattern();
    }

    @Override
    public Step getStep() {
        return delegate.getStep();
    }

    @Override
    public List<Argument> getDefinitionArgument() {
        return delegate.getDefinitionArgument();
    }

    @Override
    public StepArgument getStepArgument() {
        return delegate.getStepArgument();
    }

    @Override
    public int getStepLine() {
        return delegate.getStepLine();
    }

    @Override
    public URI getUri() {
        return delegate.getUri();
    }

    @Override
    public String getStepText() {
        return delegate.getStepText();
    }

    @Override
    public String getCodeLocation() {
        return delegate.getCodeLocation();
    }

    @Override
    public UUID getId() {
        return delegate.getId();
    }
}
