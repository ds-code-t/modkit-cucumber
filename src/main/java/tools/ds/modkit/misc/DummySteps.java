package tools.ds.modkit.misc;

import io.cucumber.java.en.When;

public class DummySteps {

    @When("^IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*)(?: ELSE-IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*))?(?: ELSE-IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*))?(?: ELSE-IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*))?(?: ELSE-IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*))?(?: ELSE-IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*))?(?: ELSE: (.*))?$")
    public void dummyIF() {

    }

    @When("^((?:(?!IF:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*)(?: ELSE-IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*))?(?: ELSE-IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*))?(?: ELSE-IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*))?(?: ELSE-IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*))?(?: ELSE-IF: ((?:(?!THEN:).)*) THEN: ((?:(?!(?:ELSE:|ELSE-IF:)).)*))?(?: ELSE: (.*))?$")
    public void dummyNoIF() {
    }

}
