package tools.ds.modkit.state;

import io.cucumber.core.backend.TestCaseState;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.runner.Runner;
import io.cucumber.plugin.event.TestCase;
import tools.ds.modkit.executions.StepExecution;
import tools.ds.modkit.mappings.ParsingMap;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static tools.ds.modkit.util.Reflect.getProperty;
import static tools.ds.modkit.util.Reflect.nameOf;

public final class ScenarioState {

    public ParsingMap getTestMap() {
        return testMap;
    }

    // Canonical keys (unchanged)
    private ParsingMap testMap = new ParsingMap();

    public static final String K_TEST_CASE = "io.cucumber.core.runner.TestCase";
//    private static final String K_PICKLE = "io.cucumber.messages.types.Pickle";
public static final String K_PICKLE = "io.cucumber.core.gherkin.messages.GherkinMessagesPickle";
    public static final String K_SCENARIO = "io.cucumber.messages.types.Scenario";
    public static final String K_RUNNER = "io.cucumber.core.runner.Runner";

    /**
     * No initial value — you must call beginNew() or set(...).
     */
    private static final ThreadLocal<ScenarioState> STATE_TL = ThreadLocal.withInitial(ScenarioState::new);

    /**
     * Per-thread store lives inside the ScenarioState instance.
     */
    private final Map<Object, Object> store = new ConcurrentHashMap<>();


    public  TestCase testCase;
    public  io.cucumber.core.gherkin.Pickle scenarioPickle;
    public  EventBus bus;
    public  TestCaseState state;

    public  StepExecution stepExecution;
    public  Runner runner;

    public EventBus getBus() {
        return bus;
    }

    public TestCaseState getTestCaseState() {
        return state;
    }

//    private ScenarioState(TestCase testCase, EventBus bus, io.cucumber.core.backend.TestCaseState state) {
//        this.bus = bus;
//        this.state = state;
//        this.testCase = testCase;
//        this.scenarioPickle = (io.cucumber.core.gherkin.Pickle) getProperty(testCase, "pickle");
//        this.stepExecution = new StepExecution(testCase);
//    }

    public static void setScenarioStateValues(TestCase testCase, EventBus bus, io.cucumber.core.backend.TestCaseState state) {
        ScenarioState scenarioState = getScenarioState();
        scenarioState.bus = bus;
        scenarioState.state = state;
        scenarioState.testCase = testCase;
        scenarioState.scenarioPickle = (io.cucumber.core.gherkin.Pickle) getProperty(testCase, "pickle");
        scenarioState.stepExecution = new StepExecution(testCase);
        scenarioState.runner = scenarioState.getRunner();
        scenarioState.clear();
    }

    public static ScenarioState getScenarioState() {
        return STATE_TL.get();
    }

    public StepExecution getStepExecution() {
        return stepExecution;
    }

    public TestCase getTestCase() {
        return testCase;
    }



    /* ========================= lifecycle ========================= */

//    /**
//     * Install a fresh ScenarioState on this thread, cleaning up any previous one.
//     */
//    public static ScenarioState beginNew(TestCase testCase, EventBus bus, io.cucumber.core.backend.TestCaseState state) {
//        ScenarioState prev = STATE_TL.get();
//        if (prev != null) prev.onClose();
//        ScenarioState next = new ScenarioState(testCase, bus, state);
//        STATE_TL.set(next);
//        return next;
//    }

    /**
     * Replace with a provided instance (rare). Cleans up any previous one.
     */
    public static void set(ScenarioState replacement) {
        Objects.requireNonNull(replacement, "replacement");
        ScenarioState prev = STATE_TL.get();
        if (prev != null && prev != replacement) prev.onClose();
        STATE_TL.set(replacement);
    }



    /**
     * End of scenario: clear and detach from thread to avoid leaks.
     */
    public static void end() {
        ScenarioState prev = STATE_TL.get();
        if (prev != null) prev.onClose();
        STATE_TL.remove();
    }

    /**
     * Optional: clear contents but keep this instance bound to the thread.
     */
    public void clear() {
        store.clear();
    }

    /**
     * Internal cleanup hook.
     */
    private void onClose() {
        store.clear();
    }

    /* ========================= registry ops (thread-local) ========================= */

    /**
     * Register the same value under each provided key for this thread.
     */
    public void register(Object value, Object... keys) {
        System.out.println("@@State register-value: " + value );
        System.out.println("@@State register-keys: " + Arrays.asList(keys) );
        if (value == null) return;
        if (keys == null || keys.length == 0) {
            store.put(value.getClass(), value);
            return;
        }
        for (Object k : keys) {
            if (k != null) store.put(k, value);
        }
    }

    public Object get(Object key) {
        return (key == null) ? null : store.get(key);
    }

    public <T> T get(Object key, Class<T> type) {
        if (key == null || type == null) return null;
        Object v = store.get(key);
        return type.isInstance(v) ? type.cast(v) : null;
    }

    public void remove(Object key) {
        if (key == null) return;
        store.remove(key);
    }

    /* ========================= convenience getters ========================= */
    public io.cucumber.core.gherkin.Pickle getScenarioPickle() {
        return scenarioPickle;
    }

    public Object getScenario() {
        return get(K_SCENARIO);
    }

    public Runner getRunner() {
        System.out.println("@@##$#$ store : " + store);
        if(runner == null)
            return (Runner) get(K_RUNNER);
        return runner;
    }

    public String getTestCaseName() {
        return nameOf(getTestCase());
    }

    public String getPickleName() {
        return nameOf(getScenarioPickle());
    }

    public String getPickleLanguage() {
        return getScenarioPickle().getLanguage();
    }
    public String getScenarioName() {
        return nameOf(getScenario());
    }


}
