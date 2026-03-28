@checkout
Feature: Checkout and Order Placement
  As a logged-in shopper with items in my cart on Automation Exercise
  I want to proceed through the checkout process
  So that I can place an order and receive confirmation

  Background:
    Given the user is logged in with valid credentials
    And the user has added a product to the cart
    And the user is on the Cart page

  @sanity @regression
  Scenario: Checkout page displays the delivery address correctly
    When the user proceeds to checkout
    Then the Checkout page should be displayed
    And the delivery address should be shown on the page

  @regression
  Scenario: Complete an order from checkout through to confirmation
    When the user proceeds to checkout
    Then the Checkout page should be displayed
    And the delivery address should be shown on the page
    When the user enters "Test order" as a comment for the delivery
    And the user clicks Place Order
    And the user enters the following payment details:
      | cardholderName | Test User        |
      | cardNumber     | 4111111111111111 |
      | cvc            | 123              |
      | expiryMonth    | 12               |
      | expiryYear     | 2025             |
    And the user confirms the order
    Then the order confirmation page should be displayed
    And the confirmation message should congratulate the user

  @regression
  Scenario: Order total is displayed with the correct currency on the checkout page
    When the user proceeds to checkout
    Then the Checkout page should be displayed
    And the order total should be visible
    And the order total should show the amount in rupees
