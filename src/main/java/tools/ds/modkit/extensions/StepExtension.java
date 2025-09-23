package tools.ds.modkit.extensions;

import com.google.common.collect.LinkedListMultimap;
import io.cucumber.core.backend.TestCaseState;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.datatable.DataTable;
import io.cucumber.gherkin.GherkinDialects;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleStepArgument;
import io.cucumber.plugin.event.*;
import tools.ds.modkit.coredefinitions.MetaSteps;
import tools.ds.modkit.coredefinitions.ModularScenarios;
import tools.ds.modkit.executions.StepExecution;
import tools.ds.modkit.mappings.NodeMap;
import tools.ds.modkit.mappings.ParsingMap;
import tools.ds.modkit.status.SoftException;
import tools.ds.modkit.status.SoftRuntimeException;
import tools.ds.modkit.util.PickleStepArgUtils;
import tools.ds.modkit.util.Reflect;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tools.ds.modkit.blackbox.BlackBoxBootstrap.metaFlag;
import static tools.ds.modkit.blackbox.BlackBoxBootstrap.skipLogging;
import static tools.ds.modkit.coredefinitions.MetaSteps.defaultMatchFlag;
import static tools.ds.modkit.extensions.StepExtension.StepFlag.*;
import static tools.ds.modkit.mappings.ParsingMap.scenarioMapKey;
import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.util.ExecutionModes.RUN;
import static tools.ds.modkit.util.ExecutionModes.SKIP;
import static tools.ds.modkit.util.KeyFunctions.getUniqueKey;
import static tools.ds.modkit.util.Reflect.getProperty;
import static tools.ds.modkit.util.Reflect.invokeAnyMethod;
import static tools.ds.modkit.util.TableUtils.toFlatMultimap;
import static tools.ds.modkit.util.TableUtils.toListOfMultimap;
import static tools.ds.modkit.util.stepbuilder.StepUtilities.createScenarioPickleStepTestStep;
import static tools.ds.modkit.util.stepbuilder.StepUtilities.getDefinition;

public class StepExtension implements PickleStepTestStep, io.cucumber.plugin.event.Step {

    @Override
    public String toString() {
        return getStepText();
    }

    private StepExtension templateStep;
    private boolean isTemplateStep = true;
//    private  boolean skipLogging;

    public final PickleStepTestStep delegate;
    public final PickleStep rootStep;
    public final io.cucumber.core.gherkin.Step gherikinMessageStep;
    public final String rootText;
    public final String metaData;


    private List<NodeMap> stepMaps = new ArrayList<>();


    public final io.cucumber.core.gherkin.Pickle parentPickle;
//    public final String pickleKey;

    private DataTable stepDataTable;

    //    public void addScenarioMaps(NodeMap nodeMap) {
//        this.scenarioMaps.add(nodeMap);
//    }
    private final boolean isCoreStep;
    private final boolean isDataTableStep;

    private final boolean metaStep;

    private static final Pattern pattern = Pattern.compile("@\\[([^\\[\\]]+)\\]");

//    private static final Class<?> metaClass = tools.ds.modkit.coredefinitions.MetaSteps.class;
//    private static final Class<?> ModularScenarios = tools.ds.modkit.coredefinitions.MetaSteps.class;


    private final Method method;

    private final String stepTextOverRide;
    private final boolean isScenarioNameStep;


    public StepExtension(io.cucumber.core.gherkin.Pickle pickle, StepExecution stepExecution,
                         PickleStepTestStep step) {
        this(createScenarioPickleStepTestStep(pickle, step), stepExecution, pickle);
    }

//    public StepExtension(PickleStepTestStep step, StepExecution stepExecution,
//                         io.cucumber.core.gherkin.Pickle pickle) {
//        this(step, stepExecution, pickle, false, false, null);
//    }

//
//    public StepExtension(PickleStepTestStep step, StepExecution stepExecution,
//                         io.cucumber.core.gherkin.Pickle pickle, boolean isContainerStep, boolean isScenarioNameStep, String stepTextOverRide) {

    public StepExtension(PickleStepTestStep step, StepExecution stepExecution,
                         io.cucumber.core.gherkin.Pickle pickle) {
        this(step, stepExecution, pickle, new HashMap<>());
    }

    public StepExtension(PickleStepTestStep step, StepExecution stepExecution,
                         io.cucumber.core.gherkin.Pickle pickle, Map<String, Object> configs) {
        System.out.println("@@step: " + step.getStepText());
        if (step.getCodeLocation().startsWith(ModularScenarios.class.getCanonicalName() + ".")) {
            this.overRideUUID = skipLogging;
            this.letChildrenInheritMaps = false;
        }
        this.stepTextOverRide = (String) configs.getOrDefault("stepTextOverRide", null);
        this.isScenarioNameStep = (boolean) configs.getOrDefault("isScenarioNameStep", false);

        this.parentPickle = pickle;

        this.isCoreStep = step.getCodeLocation().startsWith(MetaSteps.class.getPackageName() + ".");

        this.method = (Method) getProperty(step, "definitionMatch.stepDefinition.stepDefinition.method");
        System.out.println("@@this.method:: " + method);

//        this.pickleKey = getPickleKey(pickle);
        this.stepExecution = stepExecution;
        this.delegate = step;
        this.gherikinMessageStep = (io.cucumber.core.gherkin.Step) getProperty(delegate, "step");
        this.rootStep = (PickleStep) getProperty(gherikinMessageStep, "pickleStep");
        String[] strings = ((String) getProperty(rootStep, "text")).split(metaFlag);
        rootText = strings[0].trim();
        metaData = strings.length == 1 ? "" : strings[1].trim();
        Matcher matcher = pattern.matcher(metaData);

        isDataTableStep = isCoreStep && method.getName().equals("getDataTable");

        metaStep = isDataTableStep;


        while (matcher.find()) {
            stepTags.add(matcher.group().substring(1).replaceAll("\\[|\\]", ""));
        }


        for (String s : stepTags)
            try {
                stepFlags.add(StepFlag.valueOf(s.trim()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Illegal flag at " + getLocation() + " : " + s + " | Allowed: " + Arrays.stream(StepFlag.values()).map(Enum::name).collect(Collectors.joining(", ")));
            }
        nestingLevel = (int) matcher.replaceAll("").chars().filter(ch -> ch == ':').count();


    }


    public void addScenarioMaps(NodeMap... nodes) {
        for (NodeMap node : nodes) {
            if (node != null) {
                this.scenarioMaps.add(node);
            }
        }
    }


    public void addFirstScenarioMaps(NodeMap... nodes) {
        for (NodeMap node : nodes) {
            if (node != null) {
                this.scenarioMaps.addFirst(node);
            }
        }
    }


    private List<NodeMap> scenarioMaps = new ArrayList<>();


    private boolean letChildrenInheritMaps = true;

    public List<NodeMap> getScenarioMapInheritance() {
        return letChildrenInheritMaps ? scenarioMaps : new ArrayList<>();
    }

    public int getNestingLevel() {
        return nestingLevel;
    }

    public void setNestingLevel(int nestingLevel) {
        this.nestingLevel = nestingLevel;
    }

    private int nestingLevel;
    public List<String> stepTags = new ArrayList<>();

    public List<StepExtension> getChildSteps() {
        return childSteps;
    }

    public void addChildStep(StepExtension child) {
        child.parentStep = this;
        childSteps.add(child);
    }

    private final List<StepExtension> childSteps = new ArrayList<>();


    public StepExtension getParentStep() {
        return parentStep;
    }

    public void setParentStep(StepExtension parentStep) {
        parentStep.childSteps.add(this);
        this.parentStep = parentStep;
    }

    public StepExtension getPreviousSibling() {
        return previousSibling;
    }

    public void setPreviousSibling(StepExtension previousSibling) {
        previousSibling.nextSibling = this;
        this.previousSibling = previousSibling;
    }


    public StepExtension getNextSibling() {
        return nextSibling;
    }

    public void setNextSibling(StepExtension nextSibling) {
        nextSibling.previousSibling = this;
        this.nextSibling = nextSibling;
    }

    private StepExtension parentStep;
    private StepExtension previousSibling;
    private StepExtension nextSibling;
    public final StepExecution stepExecution;
    public Result result;


    public List<Object> getExecutionArguments() {
        return executionArguments;
    }

    public void setExecutionArguments(List<Object> executionArguments) {
        this.executionArguments = executionArguments;
        System.out.println("@@step text::- " + getStepText());
        System.out.println("@@executionArguments.size()::- " + executionArguments.size());
        this.stepDataTable = executionArguments.stream().filter(DataTable.class::isInstance).map(DataTable.class::cast).findFirst().orElse(null);
        System.out.println("@@ this.stepDataTable::- " + this.stepDataTable);
    }

    private List<Object> executionArguments;
//    public final ParsingMap parsingMap;


    public StepExtension updateStep(ParsingMap parsingMap, StepExtension ranParentStep, StepExtension ranPreviousSibling) {
        System.out.println("@@updateStep=1 : " + getStepText());

        if (parentStep != null) {
            scenarioMaps = Stream.concat(parentStep.getScenarioMapInheritance().stream(), scenarioMaps.stream())
                    .toList();
        }

        parsingMap.overWriteEntries(scenarioMapKey, scenarioMaps.toArray(new NodeMap[0]));

        PickleStepArgument argument = rootStep.getArgument().orElse(null);
        UnaryOperator<String> external = parsingMap::resolveWholeText;
        PickleStepArgument newPickleStepArgument = isScenarioNameStep ? null : PickleStepArgUtils.transformPickleArgument(argument, external);
        String newStepText = isScenarioNameStep ? stepTextOverRide : rootStep.getText();
        System.out.println("@@updateStep=1a : " + newStepText);
        System.out.println("@@updateStep=1b : " + parsingMap.resolveWholeText(newStepText));

        PickleStep pickleStep = new PickleStep(newPickleStepArgument, rootStep.getAstNodeIds(), rootStep.getId(), rootStep.getType().orElse(null), parsingMap.resolveWholeText(newStepText));
        io.cucumber.core.gherkin.Step newGherikinMessageStep = (io.cucumber.core.gherkin.Step) Reflect.newInstance(
                "io.cucumber.core.gherkin.messages.GherkinMessagesStep",
                pickleStep,
                GherkinDialects.getDialect(getScenarioState().getPickleLanguage()).orElse(GherkinDialects.getDialect("en").get()),
                gherikinMessageStep.getPreviousGivenWhenThenKeyword(),
                gherikinMessageStep.getLocation(),
                gherikinMessageStep.getKeyword()
        );

        Object pickleStepDefinitionMatch = getDefinition(getScenarioState().getRunner(), getScenarioState().getScenarioPickle(), newGherikinMessageStep);
        List<io.cucumber.core.stepexpression.Argument> args = (List<io.cucumber.core.stepexpression.Argument>) getProperty(pickleStepDefinitionMatch, "arguments");
        StepExtension newStep = new StepExtension((PickleStepTestStep) Reflect.newInstance(
                "io.cucumber.core.runner.PickleStepTestStep",
                getId(),               // java.util.UUID
                getUri(),                // java.net.URI
                newGherikinMessageStep,        // io.cucumber.core.gherkin.Step (public)
                pickleStepDefinitionMatch            // io.cucumber.core.runner.PickleStepDefinitionMatch (package-private instance is fine)
        ), stepExecution, parentPickle);

        newStep.setExecutionArguments(args.stream().map(io.cucumber.core.stepexpression.Argument::getValue).toList());


        newStep.childSteps.addAll(childSteps);
        System.out.println("@@update scenarioMaps: " + scenarioMaps);
        newStep.scenarioMaps.addAll(scenarioMaps);

        if (ranParentStep != null) {
            newStep.parentStep = ranParentStep;
            ranParentStep.childSteps.set(ranParentStep.childSteps.indexOf(this), newStep);
        } else {
            newStep.parentStep = parentStep;
        }

        if (ranPreviousSibling != null) {
            newStep.previousSibling = ranPreviousSibling;
            ranPreviousSibling.nextSibling = newStep;
        } else {
            newStep.previousSibling = previousSibling;
        }
        newStep.nextSibling = nextSibling;
        newStep.nestingLevel = nestingLevel;
        newStep.stepTags = stepTags;
        newStep.isTemplateStep = false;
        newStep.templateStep = this;
        newStep.letChildrenInheritMaps = letChildrenInheritMaps;

        System.out.println("@@newStep-text " + newStep.getStepText());
        System.out.println("@@unpadted-text " + getStepText());
        System.out.println("@@newStep.nextSibling=== " + newStep.nextSibling);
        System.out.println("@@updateStep=2 : " + newStep.getStepText());
        return newStep;
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

    TestCase ranTestCase;
    EventBus ranBus;
    TestCaseState ranState;
    Object ranExecutionMode;


    public StepExtension runNextSibling() {
        if (nextSibling == null)
            return null;
        System.out.println("@@runNextSibling: " + nextSibling.getStepText());

        StepExtension currentStep = nextSibling;
        if (nextSibling.isTemplateStep) {
            currentStep = nextSibling.updateStep(getScenarioState().getParsingMap(), null, this);
            nextSibling = currentStep;
            currentStep.previousSibling = this;
        }
        return currentStep.run(ranTestCase, ranBus, ranState, ranExecutionMode);
    }

    public StepExtension runFirstChild() {
        System.out.println("\n\n========\n@@runFirstChild: " + this);
        System.out.println("@@Children: " + childSteps.size());
        if (childSteps.isEmpty())
            return null;
        System.out.println("@@FirstChild: " + childSteps.getFirst());
        System.out.println("@@stepResults2== " + getStepText());
        Integer r = ((List<Result>) getProperty(ranState, "stepResults")).size();
        System.out.println("@@result = " + r);
        return childSteps.getFirst().updateStep(getScenarioState().getParsingMap(), this, null).run(ranTestCase, ranBus, ranState, ranExecutionMode);

    }

//    public StepExtension run(StepExtension previousExecution) {
//        if (templateStep) {
//            return updateStep(getScenarioState().getParsingMap()).run(previousExecution);
//        }
//        return run(previousExecution.ranTestCase, previousExecution.ranBus, previousExecution.ranState, previousExecution.ranExecutionMode);
//    }


    public StepExtension run(TestCase testCase, EventBus bus, TestCaseState state, Object executionMode) {
        System.out.println("@@run-step: " + getStepText());


        if (nextSibling != null && nextSibling.metaStep) {
            if (nextSibling.isDataTableStep) {
                System.out.println("@@nextSibling text1: " + (nextSibling == null ? "null" : nextSibling.getStepText()));
                StepExtension updatedDataTableStep = nextSibling.updateStep(getScenarioState().getParsingMap(), null, null);
                System.out.println("@@nextSibling.stepDataTable = " + nextSibling.stepDataTable);
                System.out.println("@@updatedDataTableStep.stepDataTable = " + updatedDataTableStep.stepDataTable);
                for (LinkedListMultimap<?, ?> map : toListOfMultimap(updatedDataTableStep.stepDataTable)) {
                    stepMaps.add(new NodeMap(map));
                }
                stepMaps.add(new NodeMap(toFlatMultimap(updatedDataTableStep.stepDataTable.asLists())));
                nextSibling = nextSibling.nextSibling;
                System.out.println("@@nextSibling text2: " + (nextSibling == null ? "null" : nextSibling.getStepText()));
            }

        }


        getScenarioState().setCurrentStep(this);

        getScenarioState().register(this, getUniqueKey(this));

        skipped = stepExecution.isScenarioComplete();

        executionMode = shouldRun() ? RUN(executionMode) : SKIP(executionMode);

        Object returnObj = invokeAnyMethod(delegate, "run", testCase, bus, state, executionMode);
        System.out.println("@@stepResults== " + getStepText());
        List<Result> results = ((List<Result>) getProperty(state, "stepResults"));
        result = results.getLast();

        System.out.println("\n\n===\n@@Step- " + getStepText());
        System.out.println("@@REsults- " + result);

        if (result.getStatus().equals(Status.FAILED) || result.getStatus().equals(Status.UNDEFINED)) {
            Throwable throwable = result.getError();
            System.out.println("@@throwable: " + throwable);
            if (throwable != null && (throwable.getClass().equals(SoftException.class) || throwable.getClass().equals(SoftRuntimeException.class)))
                setSoftFail();
            else
                setHardFail();
        }

        if (!(containsAnyStepFlags(StepFlag.TRY) || stepExecution.isScenarioComplete() || containsAnyStepFlags(onScenarioFlags))) {
            System.out.println("@@isHardFail(): " + isHardFail());
            System.out.println("@@isSoftFail(): " + isSoftFail());

            System.out.println("@@stepExecution.isScenarioComplete() " + stepExecution.isScenarioComplete());
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
        System.out.println("@@childStep-: " + childSteps);


//        for (StepExtension step : childSteps) {
//            step.skipped = stepExecution.isScenarioComplete() || isFail() || skipped;
//            intersectWith(onScenarioFlags);
//            System.out.println("@@child steps: " + step.getStepText());
//            step.run(testCase, bus, state, executionMode);
//        }
        ranTestCase = testCase;
        ranBus = bus;
        ranState = state;
        ranExecutionMode = returnObj;

        StepExtension ranStep = runFirstChild();

        return runNextSibling();
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


    public void intersectWith(StepFlag... items) {
        if (parentStep == null)
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
        return isScenarioNameStep ? parentPickle.getLocation().getLine() : delegate.getStepLine();
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

    public UUID overRideUUID = null;

    @Override
    public UUID getId() {
        return overRideUUID == null ? delegate.getId() : overRideUUID;
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
        return "\u00A0\u00A0\u00A0\u00A0\u00A0".repeat(nestingLevel) + (isScenarioNameStep ? stepTextOverRide : gherikinMessageStep.getText().replaceFirst(defaultMatchFlag, ""));
    }

    @Override
    public int getLine() {
        return isScenarioNameStep ? parentPickle.getLocation().getLine() : gherikinMessageStep.getLine();
    }

    @Override
    public Location getLocation() {
        return isScenarioNameStep ? parentPickle.getLocation() : gherikinMessageStep.getLocation();
    }


}
