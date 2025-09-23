package tools.ds.modkit.coredefinitions;

import com.google.common.collect.LinkedListMultimap;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import tools.ds.modkit.state.ScenarioState;

import java.util.ArrayList;
import java.util.List;

import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.util.TableUtils.*;

public class GeneralSteps {
    @Given("^save \"(.*)\" as \"(.*)\"$")
    public static void saveValues(String value, String key) {
        getScenarioState().put(key, value);
    }

    @Given("^SET (\".*\"\\s)?TABLE VALUES$")
    public static void setValues(String tableName, DataTable dataTable) {
        ScenarioState scenarioState = getScenarioState();
        if (tableName != null && !tableName.isBlank()) {
            scenarioState.put(tableName.trim(), toFlatMultimap(dataTable.asLists()));
            scenarioState.put(tableName.trim(), toRowsMultimap(dataTable));
        } else {
            scenarioState.mergeToRunMap(toFlatMultimap(dataTable.asLists()));
            scenarioState.mergeToRunMap(toRowsMultimap(dataTable));
        }

    }
}
