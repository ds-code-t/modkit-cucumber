package tools.ds.cuke.launcher;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import tools.ds.modkit.OverrideRegistry;
import tools.ds.modkit.PatchProvider;
import tools.ds.modkit.StringSuffixAdvice;
import tools.ds.modkit.AppendSuffixToDisplayNameCtor;

import java.lang.instrument.Instrumentation;
import java.lang.module.ModuleDescriptor;
import java.security.ProtectionDomain;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

public final class ByteBuddyLauncherListener implements LauncherSessionListener {

    private static final boolean DEBUG =
            Boolean.parseBoolean(System.getProperty("modkit.debug", "true"));
    private static volatile boolean INSTALLED = false;

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        if (INSTALLED) return;
        INSTALLED = true;

        System.setProperty("jdk.attach.allowAttachSelf", "true");
        if (DEBUG) System.err.println("[listener] Starting ByteBuddyLauncherListener…");

        // 1) Install agent
        Instrumentation inst = ByteBuddyAgent.install();
        if (DEBUG) System.err.println("[listener] Instrumentation acquired: " + (inst != null));

        // 2) Load patches
        OverrideRegistry reg = OverrideRegistry.get();
        List<String> providers = new ArrayList<>();
        ServiceLoader.load(PatchProvider.class, ByteBuddyLauncherListener.class.getClassLoader())
                .forEach(p -> {
                    try { p.register(reg); providers.add(p.getClass().getName()); }
                    catch (Throwable t) { System.err.println("[listener] PatchProvider registration failed: " + p + " -> " + t); }
                });
        if (DEBUG) System.err.println("[listener] Loaded providers: " + providers);

        Set<String> targetTypes = reg.targetTypeNames();
        if (targetTypes.isEmpty()) {
            System.err.println("[listener] No overrides registered; skipping instrumentation.");
            return;
        }
        if (DEBUG) System.err.println("[listener] Target types: " + targetTypes);

        // 3) Open JPMS packages
        for (String fqn : targetTypes) tryOpenModuleFor(inst, fqn);

        // 4) Methods we’ll advise (zero-arg, String-returning)
        final Set<String> stringZeroArgMethods = new LinkedHashSet<>();
        reg.entriesSnapshot().forEach(e -> { if (e.paramTypes.isEmpty()) stringZeroArgMethods.add(e.methodName); });
        if (DEBUG) System.err.println("[listener] String zero-arg methods: " + stringZeroArgMethods);

        // 5) Type matcher (targets and their supertypes), exclude interfaces
        net.bytebuddy.matcher.ElementMatcher.Junction<? super TypeDescription> typeMatcher = none();
        for (String fqn : targetTypes) {
            typeMatcher = typeMatcher.or(named(fqn)).or(hasSuperType(named(fqn)));
        }
        typeMatcher = typeMatcher.and(not(isInterface()));

        // 6) Quiet-ish listener (log only interesting packages)
        final Set<String> debugPkgs = reg.targetPackagePrefixes();
        AgentBuilder.Listener bbListener = new AgentBuilder.Listener() {
            private boolean interesting(String tn) {
                if (!DEBUG || tn == null) return false;
                for (String p : debugPkgs) if (!p.isEmpty() && tn.startsWith(p)) return true;
                return false;
            }
            @Override public void onDiscovery(String tn, ClassLoader cl, JavaModule m, boolean l) { }
            @Override public void onIgnored(TypeDescription td, ClassLoader cl, JavaModule m, boolean l) { }
            @Override public void onComplete(String tn, ClassLoader cl, JavaModule m, boolean l) { }
            @Override public void onTransformation(TypeDescription td, ClassLoader cl, JavaModule m, boolean l, DynamicType dt) {
                if (interesting(td.getName())) System.err.println("[bb] TRANSFORM " + td.getName());
            }
            @Override public void onError(String tn, ClassLoader cl, JavaModule m, boolean l, Throwable t) {
                if (interesting(tn)) System.err.println("[bb] ERROR " + tn + " -> " + t);
            }
        };

        // 7) Install advice transformer
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)  // Byte Buddy will retransform matching, already-loaded classes on install
                .with(bbListener)
                .ignore(nameStartsWith("net.bytebuddy.")
                        .or(nameStartsWith("org.slf4j."))
                        .or(isSynthetic()))
                .type(typeMatcher)
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    DynamicType.Builder<?> b = builder;

                    // Append suffix on getters
                    for (String methodName : stringZeroArgMethods) {
                        b = b.visit(Advice.to(StringSuffixAdvice.class)
                                .on(named(methodName)
                                        .and(takesArguments(0))
                                        .and(returns(String.class))
                                        .and(not(isStatic()))));
                    }

                    // Best-effort: update cached displayName right after construction
                    if ("org.junit.platform.engine.support.descriptor.AbstractTestDescriptor"
                            .equals(typeDescription.getName())) {
                        b = b.visit(Advice.to(AppendSuffixToDisplayNameCtor.class).on(isConstructor()));
                    }

                    return b;
                })
                .installOn(inst);

        System.err.println("[listener] Override framework installed. Targets: " + targetTypes);
    }

    @Override public void launcherSessionClosed(LauncherSession session) {
        if (DEBUG) System.err.println("[listener] Session closed.");
    }

    private void tryOpenModuleFor(Instrumentation inst, String fqn) {
        try {
            int idx = fqn.lastIndexOf('.');
            Class<?> clazz = Class.forName(fqn);
            Module target = clazz.getModule();
            Module ours = ByteBuddyLauncherListener.class.getModule();
            String pkg = (idx > 0) ? fqn.substring(0, idx) : "";
            Map<String, Set<Module>> opens = (pkg.isEmpty()) ? Map.of() : Map.of(pkg, Set.of(ours));
            inst.redefineModule(target, Set.of(ours), Map.of(), opens, Set.of(), Map.of());
            ModuleDescriptor d = target.getDescriptor();
            if (DEBUG) System.err.println("[open] " + (d == null ? "(unnamed)" : d.name()) + " :: opens " + pkg);
        } catch (Throwable t) {
            if (DEBUG) System.err.println("[open] skip " + fqn + " -> " + t);
        }
    }
}
