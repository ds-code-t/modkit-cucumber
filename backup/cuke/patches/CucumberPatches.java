package tools.ds.cuke.patches;

import tools.ds.modkit.MethodOverride;
import tools.ds.modkit.OverrideRegistry;
import tools.ds.modkit.PatchProvider;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public final class CucumberPatches implements PatchProvider {

    private static final boolean DEBUG  = Boolean.parseBoolean(System.getProperty("modkit.debug", "true"));
    private static final String  SUFFIX = " [instrumented]";

    @Override
    public void register(OverrideRegistry reg) {
        if (DEBUG) System.err.println("[patches] Registering CucumberPatches…");

        // --- Pickle name paths (console and some reporters) ---
        reg.override("io.cucumber.core.gherkin.Pickle", "getName", new Class<?>[]{}, noOp()); // marker for type open/match
        reg.override("io.cucumber.core.gherkin.messages.GherkinMessagesPickle", "getName", new Class<?>[]{}, noOp());
        reg.override("io.cucumber.core.runner.TestCase", "getName", new Class<?>[]{}, noOp());

        // --- JUnit Platform (what IDE run panel reads) ---
        // Match via interface (helps type matching)
        reg.override("org.junit.platform.engine.TestDescriptor", "getDisplayName", new Class<?>[]{}, noOp());
        reg.override("org.junit.platform.engine.TestDescriptor", "getLegacyReportingName", new Class<?>[]{}, noOp());

        // Concrete cucumber descriptors
        reg.override("io.cucumber.junit.platform.engine.CucumberTestDescriptor$PickleDescriptor",
                "getDisplayName", new Class<?>[]{}, noOp());
        reg.override("io.cucumber.junit.platform.engine.CucumberTestDescriptor$FeatureDescriptor",
                "getDisplayName", new Class<?>[]{}, noOp());

        // **Key**: also include the *declaring* JUnit class so we can open its package and advise its methods.
        reg.override("org.junit.platform.engine.support.descriptor.AbstractTestDescriptor",
                "getDisplayName", new Class<?>[]{}, noOp());
        reg.override("org.junit.platform.engine.support.descriptor.AbstractTestDescriptor",
                "getLegacyReportingName", new Class<?>[]{}, noOp());
    }

    /**
     * We’re now doing the actual return-value tweak with Byte Buddy Advice,
     * so the registry entries are just “markers” (no-ops) to drive matching/opening.
     */
    private static MethodOverride noOp() {
        return new MethodOverride() {
            @Override public Object invoke(Object self, Object[] args, Callable<?> zuper, Method origin) throws Throwable {
                return zuper.call(); // unchanged
            }
        };
    }
}
