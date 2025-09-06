//package tools.ds.modkit.adv;
//
//import net.bytebuddy.asm.Advice;
//
//import java.util.Collections;
//import java.util.List;
//
//public final class GenericAdvices {
//    private GenericAdvices() {}
//
//    /** Prefix returned String with -Dmodkit.namePrefix (default "★ "). */
//    public static final class StringReturnPrefixAdvice {
//        @Advice.OnMethodExit(onThrowable = Throwable.class)
//        static void exit(@Advice.Return(readOnly = false) String value) {
//            String prefix = System.getProperty("modkit.namePrefix", "ZZZ ");
//            if (value != null && prefix != null && !prefix.isEmpty() && !value.startsWith(prefix)) {
//                value = prefix + value;
//            }
//        }
//    }
//
//    /** Suffix returned String with -Dmodkit.idSuffix (default "::mod"). */
//    public static final class StringReturnSuffixAdvice {
//        @Advice.OnMethodExit(onThrowable = Throwable.class)
//        static void exit(@Advice.Return(readOnly = false) String value) {
//            String suffix = System.getProperty("modkit.idSuffix", "::mod");
//            if (value != null && suffix != null && !suffix.isEmpty() && !value.endsWith(suffix)) {
//                value = value + suffix;
//            }
//        }
//    }
//
//    /** Return empty list when -Dmodkit.breakSteps=true (proof of interception). */
//    public static final class EmptyListReturnAdvice {
//        @Advice.OnMethodExit(onThrowable = Throwable.class)
//        static void exit(@Advice.Return(readOnly = false) List<?> value) {
//            if (Boolean.getBoolean("modkit.breakSteps")) {
//                value = Collections.emptyList();
//            }
//        }
//    }
//
//    /** Constructor arg string prefix (use with the index you weave for). */
//    public static final class CtorStringArg2PrefixAdvice {
//        @Advice.OnMethodEnter
//        static void enter(@Advice.Argument(value = 2, readOnly = false) String arg2) {
//            String prefix = System.getProperty("modkit.namePrefix", "★ ");
//            if (arg2 != null && prefix != null && !prefix.isEmpty() && !arg2.startsWith(prefix)) {
//                arg2 = prefix + arg2;
//            }
//        }
//    }
//}
