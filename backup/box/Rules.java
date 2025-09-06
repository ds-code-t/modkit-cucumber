//package tools.ds.modkit.box;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public final class Rules {
//    private Rules() {}
//
//    public static final class TypeRule {
//        public final String typeName;
//        public final List<MethodRule> methods = new ArrayList<>();
//        public final List<CtorRule> ctors = new ArrayList<>();
//
//        private TypeRule(String typeName) { this.typeName = typeName; }
//
//        public static TypeRule of(String typeName) { return new TypeRule(typeName); }
//
//        public TypeRule method(String name, int argCount, String returnTypeFqcn, Class<?> advice) {
//            methods.add(new MethodRule(name, argCount, returnTypeFqcn, advice));
//            return this;
//        }
//
//        public TypeRule ctor(int argCount, Class<?> advice) {
//            ctors.add(new CtorRule(argCount, advice));
//            return this;
//        }
//    }
//
//    public static final class MethodRule {
//        public final String name;
//        public final int argCount;
//        public final String returnTypeFqcn; // nullable for “any return type”
//        public final Class<?> adviceClass;
//
//        public MethodRule(String name, int argCount, String returnTypeFqcn, Class<?> adviceClass) {
//            this.name = name;
//            this.argCount = argCount;
//            this.returnTypeFqcn = returnTypeFqcn;
//            this.adviceClass = adviceClass;
//        }
//    }
//
//    public static final class CtorRule {
//        public final int argCount;
//        public final Class<?> adviceClass;
//
//        public CtorRule(int argCount, Class<?> adviceClass) {
//            this.argCount = argCount;
//            this.adviceClass = adviceClass;
//        }
//    }
//}
