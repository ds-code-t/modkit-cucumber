package tools.ds.modkit.blackbox;

import io.cucumber.plugin.event.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tools.ds.modkit.blackbox.Plans.*;
import static tools.ds.modkit.util.Reflect.getProperty;
import static utilities.StringUtilities.startsWithColonOrAtBracket;

public final class BlackBoxBootstrap {

//\u206A – INHIBIT SYMMETRIC SWAPPING (deprecated)
//\u206B – ACTIVATE SYMMETRIC SWAPPING (deprecated)
//\u206C – INHIBIT ARABIC FORM SHAPING (deprecated)
//\u206D – ACTIVATE ARABIC FORM SHAPING (deprecated)
//\u206E – NATIONAL DIGIT SHAPES (deprecated)
//\u206F – NOMINAL DIGIT SHAPES (deprecated)



    public static final String metaFlag = "\u206A";

    private static final Pattern LINE_SWAP_PATTERN = Pattern.compile(
            "^((?:\\s+(?::|@\\[)\\S*)+)(\\s+[A-Z*].*$)",
            Pattern.MULTILINE
    );
    public static void register() {
        System.out.println("@@register DSL");





//        // In BlackBoxBootstrap.register()
//// PickleStepTestStep#run — decide to bypass or run; and post-process return
//        Registry.register(
//                on("io.cucumber.core.runner.PickleStepTestStep", "run", 4)
//                        .returns("io.cucumber.core.runner.ExecutionMode")
//
//                        // BEFORE: inspect/mutate args if you want
//                        .before(args -> {
//                            System.out.println("[modkit] PkStep#run BEFORE args=" + java.util.Arrays.toString(args));
//                            // args[0]=TestCase, args[1]=EventBus, args[2]=TestCaseState, args[3]=ExecutionMode
//                            // (Optional) you can tweak args[3] (the incoming ExecutionMode) here if desired.
//                        })
////                io.cucumber.core.runner.ExecutionMode.SKIP;
//                        // AROUND: if true => skip original and return value from supplier
//                        .around(
//                                args -> {
//                                    // ----- your skip logic here (based only on inputs) -----
//                                    Object testCase       = args[0];
//                                    Object eventBus       = args[1];
//                                    Object testCaseState  = args[2];
//                                    Object incomingMode   = args[3];
//
//                                    // Example placeholder: skip when a sysprop is set
////                                    boolean skip = Boolean.getBoolean("modkit.skip.this.step");
//                                    boolean skip = true;
//
//                                    if (skip) {
//                                        System.out.println("[modkit] PkStep#run BYPASS (return incoming mode)");
//                                    }
//                                    return skip; // true => bypass original
//                                },
//                                args -> {
//                                    // Value to return when bypassing (must be an ExecutionMode)
//                                    return args[3]; // e.g., keep the incoming mode
//                                }
//                        )
//
//                        // AFTER: see the original return and optionally change it
//                        .after((args, ret, thr) -> {
//                            System.out.println("[modkit] PkStep#run AFTER ret=" + ret + " thr=" + thr);
//
//                            // ----- your return-munging logic here -----
//                            // Example: force return to the incoming mode if a flag is set
//                            if (Boolean.getBoolean("modkit.force.return.incoming.mode")) {
//                                return args[3];
//                            }
//                            return ret; // keep original
//                        })
//
//                        .build()
//        );






        CtorRegistryDSL.threadRegisterConstructed(
                List.of(
                        "io.cucumber.core.runner.TestCase",
                        "io.cucumber.messages.types.Pickle",
                        "io.cucumber.messages.types.Scenario"
                ),
                "current-scenario" // optional extra key (same value stored under multiple keys)
        );

//// Or global:
//        CtorRegistryDSL.globalRegisterConstructed(
//                List.of(
//                        "io.cucumber.core.runner.TestCase",
//                        "io.cucumber.messages.types.Pickle",
//                        "io.cucumber.messages.types.Scenario"
//                ),
//                "current-scenario" // optional extra key (same value stored under multiple keys)
//        );



        // --- io.cucumber.messages.types.Scenario#getName() ---
        Registry.register(
                on("io.cucumber.messages.types.Scenario", "getName", 0)
                        .returns("java.lang.String")
                        .after((args, ret, thr) -> {
                            String in = (ret == null ? "<null>" : String.valueOf(ret));
                            System.err.println("[modkit] messages.types.Scenario#getName BEFORE: " + in
                                    + (thr != null ? " (threw: " + thr + ")" : ""));
                            String out = "ZZ9" + (ret == null ? "" : in);
                            System.err.println("[modkit] messages.types.Scenario#getName AFTER : " + out);
                            return out;
                        })
                        .build()
        );


        // --- io.cucumber.core.gherkin.messages.GherkinMessagesPickle#getName() ---
        Registry.register(
                on("io.cucumber.core.gherkin.messages.GherkinMessagesPickle", "getName", 0)
                        .returns("java.lang.String")
                        .after((args, ret, thr) -> {
                            String in = (ret == null ? "<null>" : String.valueOf(ret));
                            System.err.println("[modkit] GherkinMessagesPickle#getName BEFORE: " + in
                                    + (thr != null ? " (threw: " + thr + ")" : ""));
                            String out = (ret == null ? "" : in.replace("ZZ9", ""));
                            System.err.println("[modkit] GherkinMessagesPickle#getName AFTER : " + out);
                            return out;
                        })
                        .build()
        );

// Modify the input *Token* before match_StepLine executes
//        Registry.register(
//                on("io.cucumber.gherkin.GherkinTokenMatcher", "match_StepLine", 1)
//                        .before(args -> {
//                            // Log raw args
//                            System.out.println("[modkit] match_StepLine args=" + Arrays.toString(args));
//
//                            // Grab token + line (quick reflection; adjust field/method names as needed)
//                            Object token = args[0];
//                            Object line = getProperty(token, "line");
//                            String matchedText = (String) getProperty(token, "matchedText");
//                            String matchedKeyword = (String) getProperty(token, "matchedKeyword");
//                            String keywordType = (String) getProperty(token, "keywordType");
//                            System.out.println("@@matchedText: " + matchedText);
//                            System.out.println("@@matchedKeyword: " + matchedKeyword);
//
//                            String text = (String) getProperty(line, "text");
//                            System.out.println("@@line: " + line);
//                            System.out.println("@@text: " + text);
//
//
//                        })
//                        .build()
//        );



//        Registry.register(
//                onCtor("io.cucumber.gherkin.TokenScanner", 1)
//                        .before(args -> {
//                            if (args != null && args.length == 1 && args[0] instanceof String src) {
//                                // debug
//                                System.out.println("[modkit] TokenScanner<ctor> BEFORE len=" + src.length());
//
//                                Matcher matcher = LINE_SWAP_PATTERN.matcher(src);
//                                args[0]= matcher.replaceAll("$2->$1");
//
//
//                                System.out.println("@@args[0]: "  +  args[0]);
//                            }
//                        })
//                        .build()
//        );

        // Replace the String return of EncodingParser.readWithEncodingFromSource(byte[])
// by swapping two groups on each line and inserting "->"
        Registry.register(
                on("io.cucumber.gherkin.EncodingParser", "readWithEncodingFromSource", 1)
                        .returns("java.lang.String")
                        .after((args, ret, thr) -> {
                            String original = (ret == null) ? "" : (String) ret;
                            Matcher matcher = LINE_SWAP_PATTERN.matcher(original);
                            String newStringReturn = matcher.replaceAll("$2"+metaFlag+"$1");
                            System.out.println("@@newStringReturn : " + newStringReturn);
                            return newStringReturn;
                        })
                        .build()
        );



// Mutate the `data` (2nd arg) of io.cucumber.messages.types.Source(String uri, String data, SourceMediaType mediaType)
//        Registry.register(
//                onCtor("io.cucumber.messages.types.Source", 3)
//                        .before(args -> {
//                            if (args != null && args.length == 3 && args[1] instanceof String data) {
//                                Matcher matcher = LINE_SWAP_PATTERN.matcher(data);
//                                args[1]  = matcher.replaceAll("$2->$1");
//                                System.out.println("@@ args[1]: " +  args[1]);
//                            }
//                        })
//                        .build()
//        );


//        Registry.register(
//                onCtor("io.cucumber.gherkin.Line", 1)  // <-- CtorPlan.Builder
//                        .before(args -> {
//                            if (args != null && args.length == 1 && args[0] instanceof String s && startsWithColonOrAtBracket(s)) {
//                                System.out.println("@@s: " + s);
//                                args[0] = s.replaceAll("^(.*?\\s)([A-Z*].*)$", "$2 $1");
//                                System.out.println("@@ args[0]: " + args[0]);
//                            }
//                        })
//                        .build()
//        );


// --- io.cucumber.gherkin.Line#getText() ---
//        Registry.register(
//                on("io.cucumber.gherkin.Line", "getText", 0)
//                        .returns("java.lang.String")
//                        .before(args -> System.out.println("[modkit] Line#getText <BEFORE>"))
//                        .after((args, ret, thr) -> {
//                            System.out.println("[modkit] Line#getText <AFTER> ret=" + ret + (thr != null ? " thr=" + thr : ""));
//                            // return ret;            // no-op
//                            // or mutate to prove it sticks:
//                            return (ret == null) ? null : ret + " <<patched>>";
//                        })
//                        .build()
//        );

// (optional) also peek at the raw text used to build `text`
//        Registry.register(
//                on("io.cucumber.gherkin.Line", "getRawText", 0)
//                        .returns("java.lang.String")
//                        .after((args, ret, thr) -> {
//                            System.out.println("[modkit] Line#getRawText ret=" + ret);
//                            return ret;
//                        })
//                        .build()
//        );


    }


    private BlackBoxBootstrap() {
    }
}
