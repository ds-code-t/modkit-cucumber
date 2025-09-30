package tools.ds.modkit.coredefinitions;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;


public class MetaSteps {


    @Given("^(.*\\s+)?DATA TABLE$")
    public static void getDataTable(String tableName, DataTable dataTable) {
        System.out.println("@@getDataTable");
        System.out.println("@@tableName: " + tableName);
        System.out.println("@@dataTable: " + dataTable);
        // place Holder
    }

    public static final String RUN_SCENARIO = "RUN SCENARIO:";

    @Given("^" + RUN_SCENARIO + "(.*)$")
    public static void runScenario(String scenarioName) {
        System.out.println("@@runScenario");
        System.out.println("@@scenarioName: " + scenarioName);
        // place Holder
    }

    public static final String defaultMatchFlag = "\u207A-DEFAULT_DEFINITION_";

    @Given("^"+defaultMatchFlag+"(.*)$")
    public static void matchDefault(String text) {
        System.out.println("@@DEFAULT_DEFINITION_text:: " + text);
    }

}
