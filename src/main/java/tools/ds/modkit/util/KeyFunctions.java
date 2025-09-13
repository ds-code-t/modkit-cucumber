package tools.ds.modkit.util;

public class KeyFunctions {


    public static String getUniqueKey(io.cucumber.plugin.event.PickleStepTestStep step){
        return step.getUri() + ":"  +  step.getStep().getLine();
    }

}
