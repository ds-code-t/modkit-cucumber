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

import static tools.ds.modkit.executions.StepExecution.setNesting;
import static tools.ds.modkit.modularexecutions.CucumberScanUtil.listPickles;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.util.CucumberQueryUtil.examplesOf;
import static tools.ds.modkit.util.stepbuilder.StepUtilities.createPickleStepTestStep;

public class ModularScenarios {


    @Given("RUN SCENARIOS:")
    public void runScenarios(DataTable dataTable) {
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
            Map<Integer, StepExtension> nestingMap = new HashMap<>();
            nestingMap.put(startingNestingLevel - 1, currentStep);
            StepExecution stepExecution = getScenarioState().stepExecution;
            for (Pickle pickle : pickles) {
                System.out.println("@@getExamplesLocation: " + pickle.getLocation().getLine());
                System.out.println("@@examples: " + examplesOf(pickle));
                System.out.println("@@pickle name: " + pickle.getName());


                NodeMap scenarioMap = getScenarioState().getScenarioMap(pickle);
                List<StepExtension> stepExtensions = pickle.getSteps().stream().map(s -> new StepExtension(createPickleStepTestStep(s, bus.generateId(), pickle.getUri()), stepExecution, pickle)).toList();
//                stepExtensions.forEach(s -> s.parentStep = currentStep);
                System.out.println("@@@java.util.Collections$UnmodifiableMap: " + map);
                System.out.println("@@@java.util.Collections$UnmodifiableMap: " + map.getClass());
                HashMap passedMap = new HashMap(map);
                NodeMap nodeMap = new NodeMap(passedMap);
                System.out.println("@@nodeMap.get A: " + nodeMap.getAsList("A"));
                stepExtensions.forEach(s -> s.addScenarioMaps(new NodeMap(passedMap), scenarioMap));


                System.out.println("@@stepExtensions.size : " + stepExtensions.size());
//                currentStep.childSteps.addAll(stepExtensions);
                System.out.println("@@currentStep- : " + currentStep.getStepText());
                System.out.println("@@currentStep-childSteps : " + currentStep.getChildSteps());
//                System.out.println("@@currentStep-childSteps1getStepText() : " + currentStep.getChildSteps().getFirst().getStepText());
                setNesting(stepExtensions, startingNestingLevel, nestingMap);
            }
        }
    }

}
