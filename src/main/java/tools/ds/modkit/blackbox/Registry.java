package tools.ds.modkit.blackbox;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Registry {
    private static final Map<String, Plans.MethodPlan> METHOD_PLANS = new ConcurrentHashMap<>();
    private static final Map<String, Plans.CtorPlan>   CTOR_PLANS   = new ConcurrentHashMap<>();

    private Registry() {}

    public static void register(Plans.MethodPlan plan) {
        METHOD_PLANS.put(plan.key(), plan);
    }

    public static void register(Plans.CtorPlan plan) {
        CTOR_PLANS.put(plan.key(), plan);
    }

    public static Plans.MethodPlan methodPlan(String key) { return METHOD_PLANS.get(key); }
    public static Plans.CtorPlan   ctorPlan(String key)   { return CTOR_PLANS.get(key); }

    public static Collection<Plans.MethodPlan> allMethodPlans() { return METHOD_PLANS.values(); }
    public static Collection<Plans.CtorPlan>   allCtorPlans()   { return CTOR_PLANS.values(); }
}
