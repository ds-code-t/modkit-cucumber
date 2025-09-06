package tools.ds;

import net.bytebuddy.asm.Advice;

public class FixedNameAdvice {
    @Advice.OnMethodExit
    public static void exit(@Advice.Return(readOnly = false) String returnValue) {
        System.err.println("Modifying getScenarioDesignation");
        returnValue = "ZZZZZ";
    }
}