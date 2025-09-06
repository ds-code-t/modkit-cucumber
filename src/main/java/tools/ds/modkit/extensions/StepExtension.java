package tools.ds.modkit.extensions;

import io.cucumber.core.backend.TestCaseState;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleStepArgument;
import io.cucumber.messages.types.PickleStepType;
import io.cucumber.plugin.event.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static tools.ds.modkit.util.Reflect.getProperty;
import static tools.ds.modkit.util.Reflect.invokeAnyMethod;

public class StepExtension implements PickleStepTestStep {

    public final PickleStepTestStep delegate;
    public final PickleStep rootStep;

    public StepExtension(PickleStepTestStep step){
        this.delegate = step;
        this.rootStep = (PickleStep) getProperty(delegate, "step.pickleStep");

    }

    public static PickleStep updateStep(PickleStep originalStep) {

        PickleStepArgument argument = originalStep.getArgument().orElse(null);


        return new PickleStep(originalStep.getArgument().orElse(null), originalStep.getAstNodeIds(), originalStep.getId(), originalStep.getType().orElse(null), originalStep.getText());
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
