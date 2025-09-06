package tools.ds.modkit.state;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static tools.ds.modkit.util.Reflect.findZeroArgMethod;
import static tools.ds.modkit.util.Reflect.nameOf;

public final class GlobalState {

    // Keys aligned with ScenarioState
    private static final String K_TEST_CASE = "io.cucumber.core.runner.TestCase";
    private static final String K_PICKLE    = "io.cucumber.messages.types.Pickle";
    private static final String K_SCENARIO  = "io.cucumber.messages.types.Scenario";

    private static final GlobalState INSTANCE = new GlobalState();

    /** JVM-wide store */
    private final Map<Object, Object> store = new ConcurrentHashMap<>();

    private GlobalState() {}

    /** JVM-global accessor. */
    public static GlobalState getGlobalState() {
        return INSTANCE;
    }

    /* ---------------- core registry ops (used by InstanceRegistry) ---------------- */

    /** Register the same value under each provided key in the global store. */
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

    /** Remove a single key from the global store. */
    public void remove(Object key) {
        if (key == null) return;
        store.remove(key);
    }

    /** Clear the global store (e.g., between full runs). */
    public void clear() {
        store.clear();
    }

    /* ---------------- convenience accessors ---------------- */

    /** Get any registered value by key (or null). */
    public Object byKey(Object key) {
        return (key == null) ? null : store.get(key);
    }

    /** Get and cast by key (returns null if missing or wrong type). */
    public <T> T byKey(Object key, Class<T> type) {
        if (key == null || type == null) return null;
        Object v = store.get(key);
        return type.isInstance(v) ? type.cast(v) : null;
    }

    /** Returns the (non-public) TestCase instance (as Object), or null. */
    public Object getTestCase() { return byKey(K_TEST_CASE); }

    /** Returns the Pickle instance, or null. */
    public Object getPickle() { return byKey(K_PICKLE); }

    /** Returns the Scenario instance, or null. */
    public Object getScenario() { return byKey(K_SCENARIO); }

    /** Returns test case name (via reflection), or null. */
    public String getTestCaseName() { return nameOf(getTestCase()); }

    /** Returns pickle name (via reflection), or null. */
    public String getPickleName() { return nameOf(getPickle()); }

    /** Returns scenario name (via reflection), or null. */
    public String getScenarioName() { return nameOf(getScenario()); }

    /* ---------------- tiny reflection helper ---------------- */



}
