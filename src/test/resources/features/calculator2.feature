Feature: Tiny calculator2
  As a developer
  I want to verify my Cucumber customizations
  So that I can ship a reliable testing library

  Scenario: sum  <A> , <B>
    Given a is 3 and b is 6
    Then the result should be 9
    Given a is 1 and b is 6
    Given a is 2 and b is 6

  Scenario Outline: line <A> , <B>
    Given a is <A> and b is <B>
    Given I add them <A> , <B>
    Then the result should be <C>

    Examples:
      | A  | B  | C |
      | 11 | 22 | 33  |
      | 33 | 44 | 78  |

  Scenario: scenarioA1
    Given a is 2 and b is 5
#    Given a is 2 and b is 5

  Scenario: Add twss
    Given a is 2 and b is 5
    Given a is 2 and b is 5
    Given test DataTable sd
      | Test | A<d>    | B   |
      | 1    | a<HH>aa | bbb |


  Scenario: Add two numbers a
  : @[sd]  Given a is 2 and b is 3
  :  @[sd]  When I add them s @[aaa] W #sdd :: @ww
    Then the result should be 5


#  Scenario Outline: line <A> , <B>
#    Given a is 2 and b is 3
#    Given I add them <A> , <B>
#     Given I add them <A> , <B>
#    Then the result should be 5
#
#    Examples:
#      | A    | B    |
#      | 11 | 22 |
#      | 33 | 44 |


#  Scenario: Add two numbers b
#    Given a is 2 and b is 3
#    When I add them
#    Then the result should be 5
#
#
#
#  Scenario: Add two numbers 2 c
#    Given a is 2 and b is 3
#    When I add them
#    Then the result should be 5
#
#
#  Scenario: Add two numbers d
#    Given a is 2 and b is 3
#    When I add them
#    Then the result should be 5
#
#  Scenario: Add two numbers 3 e
#    Given a is 2 and b is 3
#    When I add them
#    Then the result should be 5
#
#
#  Scenario: Add two numbers f
#    Given a is 2 and b is 3
#    When I add them
#    Then the result should be 5

#
#  @wip
#  Scenario: Add bigger numbers (work in progress)
#    Given a is 20 and b is 30
#    When I add them
#    Then the result should be 50
