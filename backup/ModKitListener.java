package tools.ds;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import java.lang.instrument.Instrumentation;

public class ModKitListener implements TestExecutionListener {
    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        Instrumentation inst = ByteBuddyAgent.install();
        ModKit.apply(inst);
    }
}