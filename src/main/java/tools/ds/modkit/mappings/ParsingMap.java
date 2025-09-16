package tools.ds.modkit.mappings;


import java.util.*;
import java.util.regex.*;


import static tools.ds.modkit.mappings.AviatorUtil.eval;
import static tools.ds.modkit.mappings.AviatorUtil.evalToBoolean;
import static tools.ds.modkit.mappings.GlobalMappings.GLOBALS;


import static tools.ds.modkit.mappings.KeyParser.Kind.SINGLE;
import static tools.ds.modkit.mappings.KeyParser.parseKey;


public class ParsingMap  extends MappingProcessor {


    private static final String overrideMap = "overrideMap";
    private static final String scenarioMap = "scenarioMap";
    private static final String scenarioMapDefaults = "scenarioMapDefaults";
    private static final String runMap = "runMap";
    private static final String globalMap = "globalMap";
    private static final String DefaultMap = "DefaultMap";


    public ParsingMap() {
        maps.put(overrideMap, new NodeMap());
        maps.put(scenarioMap, new NodeMap());
        maps.put(scenarioMapDefaults, new NodeMap());
        maps.put(runMap, new NodeMap());
        maps.put(globalMap, GLOBALS);
        maps.put(DefaultMap, new NodeMap());
    }


}