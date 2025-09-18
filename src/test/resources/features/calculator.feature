Feature: Tiny calculator

  Scenario Outline: wwnew  <A> , <B>
    Given a is 1 and b is 2
    Given a is <A> and b is <B>
    Given a is 2 and b is 2


    Examples:
      | Scenario Tags | A  | B  |
      | @aa           | 22 | 33 |

  Scenario Outline: line <A> , <B>
    Given the string "<A>" is attached as "<B>"

    Then RUN SCENARIOS:
      | Scenario Tags |
      | @bb        |
#      | <Tags>        |

    Examples:
      | Tags | A  | B  |
      | @aa  | 22 | 33 |
#      | @bb  | 44 | 78 |


  @bb
  Scenario Outline: new  <A> , <B>
    Given a is <A> and b is <B>
    Then RUN SCENARIOS:
      | Scenario Tags |
      | @aa           |

    Examples:
      | Scenario Tags | A  | B  |
      | @aa           | 22 | 33 |
      | 33            | 44 | 78 |


  @bb @TagK @aa
  Scenario: scenarioA12
    Given a is 2 and b is 1
  : Given a is 2 and b is 2
  :: Given a is 2 and b is 3
    Given a is 4 and b is 1
#  : Given a is 4 and b is 2
  :: Given a is 4 and b is 3


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


