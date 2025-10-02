package tools.ds.modkit.extensions;

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

import static tools.ds.modkit.blackbox.BlackBoxBootstrap.metaFlag;
import static tools.ds.modkit.blackbox.BlackBoxBootstrap.skipLogging;
import static tools.ds.modkit.coredefinitions.MetaSteps.defaultMatchFlag;
import static tools.ds.modkit.evaluations.AviatorUtil.eval;
import static tools.ds.modkit.extensions.StepExtension.StepFlag.*;

import static tools.ds.modkit.state.ScenarioState.getScenarioState;
import static tools.ds.modkit.util.ExecutionModes.RUN;
import static tools.ds.modkit.util.ExecutionModes.SKIP;
import static tools.ds.modkit.util.KeyFunctions.getUniqueKey;
import static tools.ds.modkit.util.Reflect.getProperty;
import static tools.ds.modkit.util.Reflect.invokeAnyMethod;
import static tools.ds.modkit.util.stepbuilder.StepUtilities.createScenarioPickleStepTestStep;
import static tools.ds.modkit.util.stepbuilder.StepUtilities.getDefinition;

public class StepExtension implements PickleStepTestStep, io.cucumber.plugin.event.Step {

    public Throwable storedThrowable;


    public String evalWithStepMaps(String expression) {
        return String.valueOf(eval(expression, stepParsingMap));
    }

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


//    private List<NodeMap> stepMaps = new ArrayList<>();


    public final io.cucumber.core.gherkin.Pickle parentPickle;
//    public final String pickleKey;

    private DataTable stepDataTable;


    private final boolean isCoreStep;
    private final boolean isDataTableStep;

    private final boolean metaStep;

    private static final Pattern pattern = Pattern.compile("@\\[([^\\[\\]]+)\\]");

//    private static final Class<?> metaClass = tools.ds.modkit.coredefinitions.MetaSteps.class;
//    private static final Class<?> ModularScenarios = tools.ds.modkit.coredefinitions.MetaSteps.class;


    private final Method method;

    //    private final String stepTextOverRide;
    private final boolean isScenarioNameStep;


    public StepExtension(io.cucumber.core.gherkin.Pickle pickle, StepExecution stepExecution,
                         PickleStepTestStep step) {
        this(createScenarioPickleStepTestStep(pickle, step), stepExecution, pickle);
    }

    public StepExtension(PickleStepTestStep step, StepExecution stepExecution,
                         io.cucumber.core.gherkin.Pickle pickle) {
        this(step, stepExecution, pickle, new HashMap<>());
    }

    public StepExtension(PickleStepTestStep step, StepExecution stepExecution,
                         io.cucumber.core.gherkin.Pickle pickle, Map<String, Object> configs) {
        String codeLocation = step.getCodeLocation();
        if (codeLocation == null)
            codeLocation = "";
        if (codeLocation.startsWith(ModularScenarios.class.getCanonicalName() + ".")) {
            this.overRideUUID = skipLogging;
            this.letChildrenInheritMaps = false;
        }

        this.isScenarioNameStep = step.getStep().getText().contains(defaultMatchFlag + "Scenario");
        this.parentPickle = pickle;

        this.isCoreStep = codeLocation.startsWith(MetaSteps.class.getPackageName() + ".");

        this.method = (Method) getProperty(step, "definitionMatch.stepDefinition.stepDefinition.method");

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


//    public void addScenarioMaps(NodeMap... nodes) {
//        for (NodeMap node : nodes) {
//            if (node != null) {
//                this.scenarioMaps.add(node);
//            }
//        }
//    }


//    public void addFirstScenarioMaps(NodeMap... nodes) {
//        for (NodeMap node : nodes) {
//            if (node != null) {
//                this.scenarioMaps.addFirst(node);
//            }
//        }
//    }


//    private List<NodeMap> scenarioMaps = new ArrayList<>();


    private boolean letChildrenInheritMaps = true;

//    public List<NodeMap> getScenarioMapInheritance() {
//        return letChildrenInheritMaps ? scenarioMaps : new ArrayList<>();
//    }

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
        child.stepParsingMap = new ParsingMap(stepParsingMap);
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

    public void insertNextSibling(StepExtension insertNextSibling) {
        if (nextSibling != null)
            insertNextSibling.setNextSibling(nextSibling);
        setNextSibling(insertNextSibling);
        if (parentStep != null)
            parentStep.addChildStep(insertNextSibling);
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
        this.stepDataTable = executionArguments.stream().filter(DataTable.class::isInstance).map(DataTable.class::cast).findFirst().orElse(null);
    }

    private List<Object> executionArguments;
    public ParsingMap stepParsingMap;


    public StepExtension createMessageStep(String newStepText) {
        Map<String,String> map = new HashMap<>();
        map.put("newStepText",  "MESSAGE:\"" + newStepText + "\"");
        map.put("removeArgs",  "true");
        map.put("RANDOMID",  "RANDOMID");

        return updateStep(null, null, null, map);
    }

    public StepExtension modifyStep(String newStepText) {
        Map<String,String> map = new HashMap<>();
        map.put("newStepText", newStepText);
        return updateStep(null, null, null, map);
    }




    private StepExtension updateStep(ParsingMap parsingMap, StepExtension ranParentStep, StepExtension ranPreviousSibling) {
        return updateStep( parsingMap,  ranParentStep,  ranPreviousSibling, new HashMap<>());
    }
    private StepExtension updateStep(ParsingMap parsingMap, StepExtension ranParentStep, StepExtension ranPreviousSibling, Map<String,String> overrides) {
        ParsingMap newParsingMap = parsingMap == null ? this.stepParsingMap : parsingMap;
//        this.stepParsingMap = parsingMap;

//        if (parentStep != null) {
//            scenarioMaps = Stream.concat(parentStep.getScenarioMapInheritance().stream(), scenarioMaps.stream())
//                    .toList();
//        }

//        newParsingMap.overWriteEntries(ParsingMap.MapType.STEP_TABLE, scenarioMaps.toArray(new NodeMap[0]));

        PickleStepArgument argument = overrides.containsKey("removeArgs") ?  null : rootStep.getArgument().orElse(null);
        UnaryOperator<String> external = newParsingMap::resolveWholeText;
        PickleStepArgument newPickleStepArgument = isScenarioNameStep ? null : PickleStepArgUtils.transformPickleArgument(argument, external);
        String newStepText = overrides.getOrDefault("newStepText",  rootStep.getText());

        PickleStep pickleStep = new PickleStep(newPickleStepArgument, rootStep.getAstNodeIds(), rootStep.getId(), rootStep.getType().orElse(null), newParsingMap.resolveWholeText(newStepText));

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
                (overrides.containsKey("RANDOMID") ? UUID.randomUUID() :  getId()),               // java.util.UUID
                getUri(),                // java.net.URI
                newGherikinMessageStep,        // io.cucumber.core.gherkin.Step (public)
                pickleStepDefinitionMatch            // io.cucumber.core.runner.PickleStepDefinitionMatch (package-private instance is fine)
        ), stepExecution, parentPickle);
        System.out.println("aa3: " + newStep);
        System.out.println("aa3 args: " + args);
        newStep.setExecutionArguments(args.stream().map(io.cucumber.core.stepexpression.Argument::getValue).toList());

        newStep.childSteps.addAll(childSteps);

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
        newStep.stepParsingMap = newParsingMap;
        newStep.nextSibling = nextSibling;
        newStep.nestingLevel = nestingLevel;
        newStep.stepTags = stepTags;
        newStep.isTemplateStep = false;
        newStep.templateStep = this;
        newStep.letChildrenInheritMaps = letChildrenInheritMaps;

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

        StepExtension currentStep = nextSibling;
        if (nextSibling.isTemplateStep) {
            currentStep = nextSibling.updateStep(getScenarioState().getParsingMap(), null, this);
            nextSibling = currentStep;
            currentStep.previousSibling = this;
        }
        return currentStep.run(ranTestCase, ranBus, ranState, ranExecutionMode);
    }

    public StepExtension runFirstChild() {
        if (childSteps.isEmpty())
            return null;
        Integer r = ((List<Result>) getProperty(ranState, "stepResults")).size();
        return childSteps.getFirst().updateStep(getScenarioState().getParsingMap(), this, null).run(ranTestCase, ranBus, ranState, ranExecutionMode);

    }

//    public StepExtension run(StepExtension previousExecution) {
//        if (templateStep) {
//            return updateStep(getScenarioState().getParsingMap()).run(previousExecution);
//        }
//        return run(previousExecution.ranTestCase, previousExecution.ranBus, previousExecution.ranState, previousExecution.ranExecutionMode);
//    }


    public StepExtension run() {
        return run(ranTestCase, ranBus, ranState, ranExecutionMode);
    }

    public StepExtension run(TestCase testCase, EventBus bus, TestCaseState state, Object executionMode) {

        if (nextSibling != null && nextSibling.metaStep) {
            if (nextSibling.isDataTableStep) {
                StepExtension updatedDataTableStep = nextSibling.updateStep(getScenarioState().getParsingMap(), null, null);
                nextSibling = nextSibling.nextSibling;
            }

        }


        getScenarioState().setCurrentStep(this);

        getScenarioState().register(this, getUniqueKey(this));

        skipped = stepExecution.isScenarioComplete();


        executionMode = shouldRun() ? RUN(executionMode) : SKIP(executionMode);
        Object returnObj = invokeAnyMethod(delegate, "run", testCase, bus, state, executionMode);
        List<Result> results = ((List<Result>) getProperty(state, "stepResults"));
        result = results.getLast();



        if (result.getStatus().equals(Status.FAILED) || result.getStatus().equals(Status.UNDEFINED)) {
            Throwable throwable = result.getError();
            if (throwable != null && (throwable.getClass().equals(SoftException.class) || throwable.getClass().equals(SoftRuntimeException.class)))
                setSoftFail();
            else
                setHardFail();
        }

        if (!(containsAnyStepFlags(StepFlag.TRY) || stepExecution.isScenarioComplete() || containsAnyStepFlags(onScenarioFlags))) {
            if (isSoftFail())
                stepExecution.setScenarioSoftFail();
            else if (isHardFail())
                stepExecution.setScenarioHardFail();
        }


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
        if (parentStep == null)
            return true;

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
        return "\u00A0\u00A0\u00A0\u00A0\u00A0".repeat(nestingLevel) + (gherikinMessageStep.getText().replaceFirst(defaultMatchFlag, ""));
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
