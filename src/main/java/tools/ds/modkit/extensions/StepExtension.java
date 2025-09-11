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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public final int nestingLevel;
    public final List<String> stepTags = new ArrayList<>();
    public final List<StepExtension> childSteps = new ArrayList<>();
    public StepExtension parentStep;
    public StepExtension previousSibling;
    public StepExtension nextSibling;
//    public final ParsingMap parsingMap;

    private static  Pattern pattern = Pattern.compile("@\\[([^\\[\\]]+)\\]");
    public StepExtension(PickleStepTestStep step) {
        this.delegate = step;
        this.gherikinMessageStep = (io.cucumber.core.gherkin.Step) getProperty(delegate, "step");
        this.rootStep = (PickleStep) getProperty(gherikinMessageStep, "pickleStep");
        String[] strings = ((String) getProperty(rootStep, "text")).split(metaFlag);
        rootText = strings[0].trim();
        metaData = strings.length == 1 ? "" : strings[1].trim();
        Matcher matcher =  pattern.matcher(metaData);
        while (matcher.find()) {
            stepTags.add(matcher.group().substring(1).replaceAll("\\[\\]",""));
        }
        nestingLevel = (int)  matcher.replaceAll("").chars().filter(ch -> ch == ':').count();
    }

    public StepExtension updateStep(ParsingMap parsingMap) {
        PickleStepArgument argument = rootStep.getArgument().orElse(null);
        UnaryOperator<String> external = parsingMap::resolveWholeText;
        PickleStepArgument newPickleStepArgument = PickleStepArgUtils.transformPickleArgument(argument, external);
        PickleStep pickleStep = new PickleStep(newPickleStepArgument, rootStep.getAstNodeIds(), rootStep.getId(), rootStep.getType().orElse(null), parsingMap.resolveWholeText(rootStep.getText()));
//        io.cucumber.core.gherkin.messages.GherkinMessagesStep
        io.cucumber.core.gherkin.Step newGherikinMessageStep = (io.cucumber.core.gherkin.Step) Reflect.newInstance(
                "io.cucumber.core.gherkin.messages.GherkinMessagesStep",
                pickleStep,
                GherkinDialects.getDialect(getScenarioState().getPickleLanguage()).orElse(GherkinDialects.getDialect("en").get()),
                gherikinMessageStep.getPreviousGivenWhenThenKeyword(),
                gherikinMessageStep.getLocation(),
                gherikinMessageStep.getKeyword()
        );

        Object pickleStepDefinitionMatch  = getUpdatedDefinition(getScenarioState().getRunner(),getScenarioState().getScenarioPickle() ,newGherikinMessageStep);
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
