//package tools.ds.modkit.builtin;
//
//import net.bytebuddy.asm.Advice;
//
//public final class PickleNameAdvice {
//    @Advice.OnMethodExit(onThrowable = Throwable.class)
//    static void exit(@Advice.Return(readOnly = false) String name) {
//        try {
//            String prefix = System.getProperty("modkit.namePrefix", "★ ");
//            if (prefix != null && !prefix.isEmpty() && name != null && !name.startsWith(prefix)) {
//                name = prefix + name;
//            }
//        } catch (Throwable ignore) { }
//    }
//    private PickleNameAdvice() {}
//}
