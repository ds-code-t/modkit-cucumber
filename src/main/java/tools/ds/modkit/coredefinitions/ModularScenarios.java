package tools.ds.modkit.coredefinitions;

import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.gherkin.Pickle;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import tools.ds.modkit.executions.StepExecution;
import tools.ds.modkit.extensions.StepExtension;
import tools.ds.modkit.mappings.NodeMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static tools.ds.modkit.coredefinitions.MetaSteps.RUN_SCENARIO;
import static tools.ds.modkit.executions.StepExecution.setNesting;
import static tools.ds.modkit.modularexecutions.CucumberScanUtil.listPickles;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.util.CucumberQueryUtil.examplesOf;
import static tools.ds.modkit.util.stepbuilder.StepUtilities.createPickleStepTestStep;

public class ModularScenarios {


    @Given("RUN SCENARIOS:")
    public static void runScenarios(DataTable dataTable) {
        EventBus bus = getScenarioState().getBus();
        List<Map<String, String>> maps = dataTable.asMaps();
        Map<String, String> cucumberProps = new HashMap<>();
        for (Map<String, String> map : maps) {
            String scenarioTags = map.get("Scenario Tags");
            String featurePaths = map.get("Features");
            if (scenarioTags != null)
                cucumberProps.put("cucumber.filter.tags", scenarioTags);
            if (featurePaths != null)
                cucumberProps.put("cucumber.features", featurePaths);
            List<Pickle> pickles = listPickles(cucumberProps);
            System.out.println("@@pickles size: " + pickles);
            StepExtension currentStep = getScenarioState().getCurrentStep();
            int startingNestingLevel = getScenarioState().getCurrentStep().getNestingLevel() + 1;

            StepExecution stepExecution = getScenarioState().stepExecution;
            StepExtension currentScenarioNameStep;
            StepExtension lastScenarioNameStep = null;
            for (Pickle pickle : pickles) {
//                System.out.println("@@pickle: " + pickle.getName());
                final String overRideStepText = RUN_SCENARIO + pickle.getName();
                NodeMap scenarioMap = getScenarioState().getScenarioMap(pickle);
                List<StepExtension> stepExtensions = pickle.getSteps().stream().map(s -> new StepExtension(createPickleStepTestStep(s, bus.generateId(), pickle.getUri()), stepExecution, pickle)).toList();
                currentScenarioNameStep = new StepExtension(currentStep.delegate, stepExecution, pickle, false, true, overRideStepText);
//                stepExtensions.add(0, currentScenarioNameStep);
                stepExtensions.forEach(s -> s.addScenarioMaps(new NodeMap(map), scenarioMap));
                currentStep.addChildStep(currentScenarioNameStep);

                if(lastScenarioNameStep != null) {
                    lastScenarioNameStep.setNextSibling(currentScenarioNameStep);
//                    currentScenarioNameStep.setPreviousSibling(lastScenarioNameStep);
                }
                lastScenarioNameStep = currentScenarioNameStep;
                Map<Integer, StepExtension> nestingMap = new HashMap<>();
                nestingMap.put(startingNestingLevel - 1, currentScenarioNameStep);
                setNesting(stepExtensions, startingNestingLevel, nestingMap);
            }
        }
    }

}
