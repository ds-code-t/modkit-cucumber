package tools.ds.modkit.extensions;

import io.cucumber.core.backend.TestCaseState;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.runner.Runner;
import io.cucumber.gherkin.GherkinDialects;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleStepArgument;
import io.cucumber.plugin.event.*;
import tools.ds.modkit.executions.StepExecution;
import tools.ds.modkit.mappings.ParsingMap;
import tools.ds.modkit.status.SoftException;
import tools.ds.modkit.status.SoftRuntimeException;
import tools.ds.modkit.util.PickleStepArgUtils;
import tools.ds.modkit.util.Reflect;

import java.net.URI;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static tools.ds.modkit.blackbox.BlackBoxBootstrap.metaFlag;
import static tools.ds.modkit.extensions.StepExtension.StepFlag.*;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.util.ExecutionModes.RUN;
import static tools.ds.modkit.util.ExecutionModes.SKIP;
import static tools.ds.modkit.util.KeyFunctions.getUniqueKey;
import static tools.ds.modkit.util.Reflect.getProperty;
import static tools.ds.modkit.util.Reflect.invokeAnyMethod;
//import static tools.ds.modkit.util.stepbuilder.GherkinMessagesStepBuilder.cloneWithPickleStep;
import static tools.ds.modkit.util.stepbuilder.StepUtilities.matchStepToStepDefinition;

public class StepExtension implements PickleStepTestStep, io.cucumber.plugin.event.Step {

    private boolean templateStep = true;

    public final PickleStepTestStep delegate;
    public final PickleStep rootStep;
    public final io.cucumber.core.gherkin.Step gherikinMessageStep;
    public final String rootText;
    public final String metaData;
    public int nestingLevel;
    public List<String> stepTags = new ArrayList<>();
    public final List<StepExtension> childSteps = new ArrayList<>();
    public StepExtension parentStep;
    public StepExtension previousSibling;
    public StepExtension nextSibling;
    public final StepExecution stepExecution;
    public Result result;
//    public final ParsingMap parsingMap;

    private static final Pattern pattern = Pattern.compile("@\\[([^\\[\\]]+)\\]");

    public StepExtension(PickleStepTestStep step, StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        this.delegate = step;
        this.gherikinMessageStep = (io.cucumber.core.gherkin.Step) getProperty(delegate, "step");
        this.rootStep = (PickleStep) getProperty(gherikinMessageStep, "pickleStep");
        String[] strings = ((String) getProperty(rootStep, "text")).split(metaFlag);
        rootText = strings[0].trim();
        metaData = strings.length == 1 ? "" : strings[1].trim();
        Matcher matcher = pattern.matcher(metaData);
        while (matcher.find()) {
            stepTags.add(matcher.group().substring(1).replaceAll("\\[|\\]", ""));
        }

//        EnumSet<StepFlag> stepFlags = EnumSet.noneOf(StepFlag.class);
        for (String s : stepTags)
            try {
                stepFlags.add(StepFlag.valueOf(s.trim()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Illegal flag at " + getLocation() + " : " + s + " | Allowed: " + Arrays.stream(StepFlag.values()).map(Enum::name).collect(Collectors.joining(", ")));
            }


        nestingLevel = (int) matcher.replaceAll("").chars().filter(ch -> ch == ':').count();
    }

    public StepExtension updateStep(ParsingMap parsingMap) {
        PickleStepArgument argument = rootStep.getArgument().orElse(null);
        UnaryOperator<String> external = parsingMap::resolveWholeText;
        PickleStepArgument newPickleStepArgument = PickleStepArgUtils.transformPickleArgument(argument, external);
        PickleStep pickleStep = new PickleStep(newPickleStepArgument, rootStep.getAstNodeIds(), rootStep.getId(), rootStep.getType().orElse(null), parsingMap.resolveWholeText(rootStep.getText()));
//        io.cucumber.core.gherkin.messages.GherkinMessagesStep
        io.cucumber.core.gherkin.Step newGherikinMessageStep = (io.cucumber.core.gherkin.Step) Reflect.newInstance(
                "io.cucumber.core.gherkin.messages.GherkinMessagesStep",
                pickleStep,
                GherkinDialects.getDialect(getScenarioState().getPickleLanguage()).orElse(GherkinDialects.getDialect("en").get()),
                gherikinMessageStep.getPreviousGivenWhenThenKeyword(),
                gherikinMessageStep.getLocation(),
                gherikinMessageStep.getKeyword()
        );

        Object pickleStepDefinitionMatch = getUpdatedDefinition(getScenarioState().getRunner(), getScenarioState().getScenarioPickle(), newGherikinMessageStep);
        StepExtension newStep = new StepExtension((PickleStepTestStep) Reflect.newInstance(
                "io.cucumber.core.runner.PickleStepTestStep",
                getId(),               // java.util.UUID
                getUri(),                // java.net.URI
                newGherikinMessageStep,        // io.cucumber.core.gherkin.Step (public)
                pickleStepDefinitionMatch            // io.cucumber.core.runner.PickleStepDefinitionMatch (package-private instance is fine)
        ), stepExecution);

        newStep.childSteps.addAll(childSteps);
        newStep.parentStep = parentStep;
        newStep.previousSibling = previousSibling;
        newStep.nextSibling = nextSibling;
        newStep.nestingLevel = nestingLevel;
        newStep.stepTags = stepTags;

        newStep.templateStep = false;
        return newStep;
    }

    public static Object getUpdatedDefinition(Runner runner, io.cucumber.core.gherkin.Pickle scenarioPickle, io.cucumber.core.gherkin.Step step) {
        System.out.println("\n##@@runner: " + runner);
        System.out.println("##@@scenarioPickle: " + scenarioPickle);
        System.out.println("##@@step: " + step);
        return matchStepToStepDefinition(runner, scenarioPickle, step);
    }


    public boolean isFail() {
        return hardFail || softFail;
    }

    public boolean isHardFail() {
        return hardFail;
    }

    public boolean isSoftFail() {
        return softFail;
    }

    public void setHardFail() {
        this.hardFail = true;
        postRunFlags.add(PostRunFlag.HARD_FAILED);
        postRunFlags.add(PostRunFlag.FAILED);
    }

    public void setSoftFail() {
        postRunFlags.add(PostRunFlag.SOFT_FAILED);
        postRunFlags.add(PostRunFlag.FAILED);
        this.softFail = true;
    }

    private boolean hardFail = false;
    private boolean softFail = false;

    private boolean skipped = false;

    public Object run(TestCase testCase, EventBus bus, TestCaseState state, Object executionMode) {
        if (templateStep) {
            return updateStep(getScenarioState().getTestMap()).run(testCase, bus, state, executionMode);
        }

        getScenarioState().register(this, getUniqueKey(this));

        skipped = stepExecution.isScenarioComplete();

        executionMode = shouldRun() ? RUN(executionMode) : SKIP(executionMode);


        Object returnObj = invokeAnyMethod(delegate, "run", testCase, bus, state, executionMode);
        result = ((List<Result>) getProperty(state, "stepResults")).getLast();

        System.out.println("\n\n===\n@@Step- " + getStepText());
        System.out.println("@@REsults- " + result);

        if (result.getStatus().equals(Status.FAILED)) {

            Throwable throwable = result.getError();
            if (throwable != null && (throwable.getClass().equals(SoftException.class) || throwable.getClass().equals(SoftRuntimeException.class)))
                setSoftFail();
            else
                setHardFail();
        }

        if (!(containsAnyStepFlags(StepFlag.TRY) || stepExecution.isScenarioComplete() || containsAnyStepFlags(onScenarioFlags))) {
            System.out.println("@@isHardFail(): " + isHardFail());
            System.out.println("@@isSoftFail(): " + isSoftFail());

            System.out.println("@@stepExecution.isScenarioComplete() "+  stepExecution.isScenarioComplete() );
            System.out.println("@@!(stepExecution.isScenarioComplete() || containsAnyStepFlags(onScenarioFlags)) " + !(stepExecution.isScenarioComplete() || containsAnyStepFlags(onScenarioFlags)));
                if (isSoftFail())
                    stepExecution.setScenarioSoftFail();
                else if (isHardFail())
                    stepExecution.setScenarioHardFail();

            System.out.println("@@isScenarioHardFail(): " + stepExecution.isScenarioHardFail());
            System.out.println("@@isSoftFail(): " + stepExecution.isScenarioSoftFail());
            System.out.println("@@isScenarioComplete(): " + stepExecution.isScenarioComplete());

        }


        System.out.println("@@PARENT:: steps: " + getStepText());
        for (StepExtension step : childSteps) {
            step.skipped = stepExecution.isScenarioComplete() || isFail() || skipped;
            intersectWith(onScenarioFlags);
            System.out.println("@@child steps: " + step.getStepText());
            step.run(testCase, bus, state, executionMode);
        }
        return returnObj;
    }


    public enum StepFlag {
        ALWAYS_RUN, ON_SCENARIO_FAIL, ON_SCENARIO_SOFT_FAIL, ON_SCENARIO_HARD_FAIL, ON_SCENARIO_PASS, ON_SCENARIO_END, TRY, SKIP, IGNORE
    }

    private final StepFlag[] onScenarioFlags = new StepFlag[]{ON_SCENARIO_FAIL, ON_SCENARIO_SOFT_FAIL, ON_SCENARIO_HARD_FAIL, ON_SCENARIO_PASS, ON_SCENARIO_END};

    public boolean containsAnyStepFlags(StepFlag... inputFlags) {
        return Arrays.stream(inputFlags).anyMatch(stepFlags::contains);
    }

    public boolean containsAllStepFlags(StepFlag... inputFlags) {
        return stepFlags.containsAll(Arrays.asList(inputFlags));
    }

    private final Set<StepFlag> stepFlags = EnumSet.noneOf(StepFlag.class);


    public void intersectWith( StepFlag... items) {
        if(parentStep == null)
            return;
        for (StepFlag f : items) if (parentStep.stepFlags.contains(f)) stepFlags.add(f);
    }

    public enum PostRunFlag {
        SKIPPED, IGNORED, FAILED, SOFT_FAILED, HARD_FAILED, PASSED
    }

    public boolean containsAnyPostRunFlags(PostRunFlag... inputFlags) {
        return Arrays.stream(inputFlags).anyMatch(postRunFlags::contains);
    }

    public boolean containsAllPostRunFlags(PostRunFlag... inputFlags) {
        return postRunFlags.containsAll(Arrays.asList(inputFlags));
    }

    private final Set<PostRunFlag> postRunFlags = EnumSet.noneOf(PostRunFlag.class);


    public boolean shouldRun() {
        System.out.println("@@stepFlags: " + stepFlags);
//        if (containsAnyStepFlags(onScenarioFlags))
//            stepExecution.setScenarioComplete();

        if (containsAnyStepFlags(StepFlag.ALWAYS_RUN))
            return true;

        if (containsAnyStepFlags(ON_SCENARIO_FAIL)) {
            return (stepExecution.isScenarioFailed());
        }

        if (containsAnyStepFlags(StepFlag.ON_SCENARIO_SOFT_FAIL))
            return isSoftFail();
        if (containsAnyStepFlags(StepFlag.ON_SCENARIO_HARD_FAIL))
            return isHardFail();
        if (containsAnyStepFlags(ON_SCENARIO_PASS))
            return !(isHardFail() || isSoftFail());

        System.out.println("@@skipped: " + skipped);
        return !skipped;
//        if(skipped)
//            return false;
//
//        return !stepExecution.isScenarioComplete();
    }


    @Override
    public String getPattern() {
        return delegate.getPattern();
    }

    @Override
    public Step getStep() {
        return this;
    }

    @Override
    public List<Argument> getDefinitionArgument() {
        return delegate.getDefinitionArgument();
    }

    @Override
    public StepArgument getStepArgument() {
        return delegate.getStepArgument();
    }

    @Override
    public int getStepLine() {
        return delegate.getStepLine();
    }

    @Override
    public URI getUri() {
        return delegate.getUri();
    }

    @Override
    public String getStepText() {
        return delegate.getStepText();
    }

    @Override
    public String getCodeLocation() {
        return delegate.getCodeLocation();
    }

    @Override
    public UUID getId() {
        return delegate.getId();
    }


    // from gherikin step

    @Override
    public StepArgument getArgument() {
        return gherikinMessageStep.getArgument();
    }

    @Override
    public String getKeyword() {
        return gherikinMessageStep.getKeyword();
    }

    @Override
    public String getText() {
        return " : ".repeat(nestingLevel) + gherikinMessageStep.getText();
    }

    @Override
    public int getLine() {
        return gherikinMessageStep.getLine();
    }

    @Override
    public Location getLocation() {
        return gherikinMessageStep.getLocation();
    }


}
