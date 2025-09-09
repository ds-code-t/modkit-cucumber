package tools.ds.modkit.extensions;

import io.cucumber.core.backend.TestCaseState;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.gherkin.Pickle;
import io.cucumber.core.runner.Runner;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleStepArgument;
import io.cucumber.plugin.event.*;
import tools.ds.modkit.mappings.ParsingMap;
import tools.ds.modkit.util.PickleStepArgUtils;
import tools.ds.modkit.util.stepbuilder.GherkinMessagesStepBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static tools.ds.modkit.blackbox.BlackBoxBootstrap.metaFlag;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.util.PickleStepArgUtils.transformPickleArgument;
import static tools.ds.modkit.util.Reflect.getProperty;
import static tools.ds.modkit.util.Reflect.invokeAnyMethod;
import static tools.ds.modkit.util.stepbuilder.GherkinMessagesStepBuilder.cloneWithPickleStep;
import static tools.ds.modkit.util.stepbuilder.StepUtilities.matchStepToStepDefinition;

public class StepExtension implements PickleStepTestStep {

    public final PickleStepTestStep delegate;
    public final PickleStep rootStep;
    public final io.cucumber.core.gherkin.Step gherikinMessageStep;
    public final String rootText;
    public final String metaData;
    public final ParsingMap parsingMap;

    public StepExtension(PickleStepTestStep step){
        this.delegate = step;
        this.gherikinMessageStep = (io.cucumber.core.gherkin.Step) getProperty(delegate, "step");
        this.rootStep = (PickleStep) getProperty(gherikinMessageStep, "pickleStep");
        String[] strings = ((String) getProperty(rootStep, "text")).split(metaFlag);
        rootText = strings[0].trim();
        metaData = strings.length ==1 ? "" :  strings[1].trim();
        this.parsingMap = getScenarioState().getTestMap();
    }

    public  PickleStep updateStep() {
        PickleStepArgument argument = rootStep.getArgument().orElse(null);
        UnaryOperator<String> external = parsingMap::resolveWholeText;
        PickleStepArgument newPickleStepArgument = PickleStepArgUtils.transformPickleArgument(argument, external);
        PickleStep pickleStep = new PickleStep(newPickleStepArgument, rootStep.getAstNodeIds(), rootStep.getId(), rootStep.getType().orElse(null), parsingMap.resolveWholeText(rootStep.getText()));
        cloneWithPickleStep(gherikinMessageStep , pickleStep);
        return pickleStep;
    }
///////
        public Object getUpdatedDefinition( io. cucumber. core. gherkin. Step step ){

        return matchStepToStepDefinition( getScenarioState().getRunner(), (Pickle) getScenarioState().getPickle(), step);
        }


    Object run(TestCase testCase, EventBus bus, TestCaseState state, Object executionMode)
    {
        return invokeAnyMethod(delegate, "run", testCase,  bus,  state, executionMode);
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
