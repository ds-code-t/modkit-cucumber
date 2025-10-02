package tools.ds.modkit.coredefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.messages.types.Step;
import tools.ds.modkit.extensions.StepExtension;

import static tools.ds.modkit.evaluations.AviatorUtil.eval;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;

public class ConditionalSteps {

    @Given("^((?:IF:|ELSE:|ELSE-IF:|THEN:).*)$")
    public static void runConditional(String inputString) {
        System.out.println("@@runConditional: " + inputString);
        StepExtension currentStep = getScenarioState().getCurrentStep();
        String evaluatedString = currentStep. evalWithStepMaps(inputString);
        System.out.println("@@evaluatedString: " + evaluatedString);

        StepExtension modifiedStep = currentStep.modifyStep(evaluatedString);
        System.out.println("@@modifiedStep: " + modifiedStep);
        getScenarioState().getCurrentStep().insertNextSibling(modifiedStep);
    }

}
