Feature: Tiny calculator

  @bb @TagK @aa
  Scenario: scenarioA12
    Given a is 2 and b is 5


  Scenario: scenario test dtable
    Given test dtable
      | Scenario | A  | B   |
      | 1        | 11 | 222 |

  Scenario: scenario test dtable2
    Given a is 2 and b is 5
    Given test dtable2
      | Scenario | A  | B   |
      | 1        | 11 | 222 |


  Scenario: getScenarios 1
    Then RUN SCENARIOS:
      | Scenario Tags |
      | @aa           |


