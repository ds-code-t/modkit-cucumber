package tools.ds.modkit.mappings;


import static tools.ds.modkit.mappings.GlobalMappings.GLOBALS;


public class ParsingMap extends MappingProcessor {


    public static final String overrideMapKey = "overrideMap";
    public static final String scenarioMapKey = "scenarioMap";
    public static final String stepMapKey = "stepMap";
    public static final String runMapKey = "runMap";
    public static final String globalMapKey = "globalMap";
    public static final String DefaultMapKey = "DefaultMap";



    public ParsingMap(NodeMap runMap) {
        super(overrideMapKey, scenarioMapKey, stepMapKey, runMapKey, globalMapKey, DefaultMapKey);
        addEntries(overrideMapKey, new NodeMap());
//        maps.put(passedScenarioMap, null);
//        maps.put(scenarioMap, null);
        addEntries(runMapKey, runMap);
        addEntries(globalMapKey, GLOBALS);
        addEntries(DefaultMapKey, new NodeMap());
    }










}