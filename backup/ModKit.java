package tools.ds;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

//import net.bytebuddy.dynamic.loading.PackageOpeningStrategy;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;

public class ModKit {
    private static final List<Registration> registrations = new ArrayList<>();

    public static void addAdvice(String className, String methodName, Class<?> adviceClass) {
        registrations.add(new Registration(className, methodName, adviceClass));
    }

    public static void apply(Instrumentation inst) {

        AgentBuilder builder = new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.TypeStrategy.Default.REBASE)
                .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError())
                .with(new DebugListener());

        for (Registration reg : registrations) {
            ElementMatcher<TypeDescription> typeMatcher = ElementMatchers.named(reg.className);
            ElementMatcher<? super MethodDescription> methodMatcher = named(reg.methodName).and(ElementMatchers.takesNoArguments()).and(ElementMatchers.returns(String.class));
            builder = builder.type(typeMatcher).transform((b, t, c, m, p) -> b.visit(net.bytebuddy.asm.Advice.to(reg.adviceClass).on(methodMatcher)));
        }

        builder.installOn(inst);
    }

    // Specific registration (add more as needed)
    static {
        addAdvice("io.cucumber.core.runner.TestCase", "getScenarioDesignation", FixedNameAdvice.class);
        addAdvice("io.cucumber.core.runner.TestCase", "getName", FixedNameAdvice.class);
        addAdvice("io.cucumber.core.runner.TestCase", "getKeyword", FixedNameAdvice.class);
        }
    private static class Registration {
        final String className;
        final String methodName;
        final Class<?> adviceClass;

        Registration(String className, String methodName, Class<?> adviceClass) {
            this.className = className;
            this.methodName = methodName;
            this.adviceClass = adviceClass;
        }
    }

    private static class DebugListener implements AgentBuilder.Listener {
        @Override
        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
            if (typeName.equals("io.cucumber.core.runner.TestCase")) {
                System.err.println("[ModKit] Discovered TestCase, loaded: " + loaded);
            }
        }

        @Override
        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
            System.err.println("[ModKit] Transformed " + typeDescription.getName());
        }

        @Override
        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
            if (typeDescription.getName().equals("io.cucumber.core.runner.TestCase")) {
                System.err.println("[ModKit] Ignored target: " + typeDescription.getName());
            }
        }

        @Override
        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
            System.err.println("[ModKit] Error for " + typeName + ": " + throwable.getMessage());
            throwable.printStackTrace(System.err);
        }

        @Override
        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
            if (typeName.equals("io.cucumber.core.runner.TestCase")) {
                System.err.println("[ModKit] Complete for TestCase, loaded: " + loaded);
            }
        }
    }
}