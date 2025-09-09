package tools.ds.modkit.state;

import io.cucumber.core.backend.TestCaseState;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.runner.Runner;
import io.cucumber.plugin.event.TestCase;
import tools.ds.modkit.executions.StepExecution;
import tools.ds.modkit.mappings.ParsingMap;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static tools.ds.modkit.util.Reflect.nameOf;

public final class ScenarioState {

    public ParsingMap getTestMap() {
        return testMap;
    }

    // Canonical keys (unchanged)
    private ParsingMap testMap = new ParsingMap();

    private static final String K_TEST_CASE = "io.cucumber.core.runner.TestCase";
    private static final String K_PICKLE    = "io.cucumber.messages.types.Pickle";
    private static final String K_SCENARIO  = "io.cucumber.messages.types.Scenario";
    private static final String K_RUNNER  = "io.cucumber.core.runner.Runner";

    /** No initial value — you must call beginNew() or set(...). */
    private static final ThreadLocal<ScenarioState> STATE_TL = new ThreadLocal<>();

    /** Per-thread store lives inside the ScenarioState instance. */
    private final Map<Object, Object> store = new ConcurrentHashMap<>();


    public final TestCase testCase;
    public final  EventBus bus;
    public final TestCaseState state;

    public final StepExecution stepExecution;

    public EventBus getBus() {
        return bus;
    }

    public TestCaseState getTestCaseState() {
        return state;
    }

    private ScenarioState(TestCase testCase, EventBus bus, io.cucumber.core.backend.TestCaseState state) {
        this.bus = bus;
        this.state = state;
        this.testCase = testCase;
        this.stepExecution = new StepExecution(testCase);
    }
    public static ScenarioState getScenarioState() { return STATE_TL.get(); }

    public StepExecution getStepExecution()
    {
        return stepExecution;
    }

    public TestCase getTestCase()
    {
        return testCase;
    }



    /* ========================= lifecycle ========================= */

    /** Install a fresh ScenarioState on this thread, cleaning up any previous one. */
    public static ScenarioState beginNew(TestCase testCase, EventBus bus, io.cucumber.core.backend.TestCaseState state) {
        ScenarioState prev = STATE_TL.get();
        if (prev != null) prev.onClose();
        ScenarioState next = new ScenarioState(testCase, bus,  state);
        STATE_TL.set(next);
        return next;
    }

    /** Replace with a provided instance (rare). Cleans up any previous one. */
    public static void set(ScenarioState replacement) {
        Objects.requireNonNull(replacement, "replacement");
        ScenarioState prev = STATE_TL.get();
        if (prev != null && prev != replacement) prev.onClose();
        STATE_TL.set(replacement);
    }

    /** Get current state (may be null if beginNew/set wasn’t called). */
    public static ScenarioState current() {
        return STATE_TL.get();
    }

    /** End of scenario: clear and detach from thread to avoid leaks. */
    public static void end() {
        ScenarioState prev = STATE_TL.get();
        if (prev != null) prev.onClose();
        STATE_TL.remove();
    }

    /** Optional: clear contents but keep this instance bound to the thread. */
    public void clear() {
        store.clear();
    }

    /** Internal cleanup hook. */
    private void onClose() {
        store.clear();
    }

    /* ========================= registry ops (thread-local) ========================= */

    /** Register the same value under each provided key for this thread. */
    public void register(Object value, Object... keys) {
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

    public Object getPickle()    { return get(K_PICKLE); }
    public Object getScenario()  { return get(K_SCENARIO); }
    public Runner getRunner()  { return (Runner) get(K_RUNNER); }

    public String getTestCaseName() { return nameOf(getTestCase()); }
    public String getPickleName()   { return nameOf(getPickle()); }
    public String getScenarioName() { return nameOf(getScenario()); }


}
