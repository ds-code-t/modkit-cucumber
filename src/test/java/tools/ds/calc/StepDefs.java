package tools.ds.calc;


import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import tools.ds.modkit.status.SoftRuntimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;


public class StepDefs {
    static {
        System.out.println("@@StepDefs");
//        EnsureInstalled.ensureOrDie();
    }
    private int a, b, result;


    @When("^test DataTable (.*)$")
    public void dttest(String name, DataTable dataTable) {
        System.out.println("@@DataTable: " + name);
        System.out.println(dataTable);
    }

//    @io.cucumber.java.BeforeAll
//   public static void before_or_after_all() throws InterruptedException {
//        System.out.println("@@before_or_after_all");
//        EnsureInstalled.ensureOrDie();
//        Thread.sleep(3000L);
//    }

    @Given("a is {int} and b is {int}")
    public void a_is_and_b_is(int a, int b) throws InterruptedException {
        Thread.sleep(500L);
        System.out.println("@@a_is_and_b_is");
        this.a = a;
        this.b = b;
    }

    @When("^I add them (.*)$")
    public void i_add_them(String string) {
        System.out.println("@@i_add_them: " + string);
        result = a + b;
        System.out.println("@@result: " + result);

//        io.cucumber.messages.types.Scenario sc =  getScenarioState().getMessageScenario();
//        System.out.println("@@--sc: " + sc);
//        System.out.println("@@--sc- getExamples: " + sc.getExamples());
//        System.out.println("@@--sc.getName " + sc.getName());
//        System.out.println("@@--steps:::::::::" );
//        sc.getSteps().forEach(System.out::println);
//        System.out.println("@@\n\n----------" );
        Object tc = getScenarioState().getTestCase();
        String tcName = getScenarioState().getTestCaseName();
        String pickleName = getScenarioState().getPickleName();
        String scenarioName = getScenarioState().getScenarioName();

        System.out.println("@@tcName: " + tcName);
        System.out.println("@@pickleName: " + pickleName);
        System.out.println("@@scenarioName: " + scenarioName);



        System.out.println("@@Steps::: " );
//        getScenarioState().getSteps().forEach(System.out::println);

    }
    @Then("the result should be {int}")
    public void the_result_should_be(int expected)  {
        System.out.println("@@the_result_should_be");
        try {
            assertEquals(expected, result, "Sum mismatch");
        }
        catch (Throwable e)
        {
            System.out.println("@@e " + e);
            throw new SoftRuntimeException(e);
        }
    }
}
