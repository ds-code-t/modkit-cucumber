package tools.ds.modkit.util.stepbuilder;

import io.cucumber.core.gherkin.Pickle;
import io.cucumber.core.runner.Runner;

import static tools.ds.modkit.util.Reflect.invokeAnyMethod;

public class StepUtilities {

    public  static  Object  matchStepToStepDefinition(Runner runner, Pickle pickle, io.cucumber.core.gherkin.Step step)
    {
        return invokeAnyMethod(runner , "matchStepToStepDefinition", pickle, step);
    }
}
