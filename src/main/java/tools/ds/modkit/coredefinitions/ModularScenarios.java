package tools.ds.modkit.coredefinitions;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;

import java.util.List;
import java.util.Map;

import static tools.ds.modkit.modularexecutions.CucumberScanUtil.listPickles;

public class ModularScenarios {


    @Given("RUN SCENARIOS:")
    public void runScenarios(DataTable dataTable)  {
        List<Map<String, String>> maps =dataTable.asMaps();
        String scenarioKey = maps.getFirst().containsKey("Scenario Args") ? "Scenario Args" : "Scenario Tags";
       for( Map<String, String> map:  maps)
       {
           map.get(scenarioKey);
       }
    }

}
