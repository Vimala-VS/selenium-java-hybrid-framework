@login
Feature: User Authentication
  As a registered user of Automation Exercise
  I want to log in to my account
  So that I can access my profile, orders, and shopping features

  Background:
    Given the user is on the Login page

  @sanity @regression
  Scenario: Successful login with valid credentials
    When the user enters a valid email address
    And the user enters a valid password
    And the user clicks the Login button
    Then the Home page should be displayed
    And the user should be logged in to their account

  @regression
  Scenario: Failed login with invalid credentials
    When the user enters an unregistered email address
    And the user enters an incorrect password
    And the user clicks the Login button
    Then an error message should appear on the Login page
    And the user should remain on the Login page

  @regression
  Scenario Outline: Login with multiple user credentials
    When the user enters "<email>" as the email address
    And the user enters "<password>" as the password
    And the user clicks the Login button
    Then the login result should be "<expectedResult>"

    Examples:
      | email                   | password  | expectedResult |
      | valid@email.com         | Valid@123 | success        |
      | invalid@email.com       | wrong     | failure        |
