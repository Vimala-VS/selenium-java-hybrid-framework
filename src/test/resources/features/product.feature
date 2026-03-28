@product
Feature: Product Search and Cart
  As a logged-in shopper on Automation Exercise
  I want to search for products and add them to my cart
  So that I can review my selections before purchasing

  Background:
    Given the user is logged in with valid credentials
    And the user is on the Home page

  @sanity @regression
  Scenario: Search for a product successfully
    When the user navigates to the Products page
    And the user searches for "Blue Top"
    Then the product "Blue Top" should appear in the search results

  @regression
  Scenario: Add a product to the cart and verify it is saved correctly
    Given the user is on the Products page
    When the user views the details of the first product
    And the user notes the product name and price
    And the user adds the product to the cart
    And the user proceeds to view the cart
    Then the cart page should be displayed
    And the product should appear in the cart
    And the product price in the cart should match the price on the product page

  @regression
  Scenario: Verify cart item count increases when products are added
    Given the user is on the Products page
    When the user adds the first product to the cart
    And the user chooses to continue shopping
    Then the cart should contain 1 item
    When the user adds another product to the cart
    And the user chooses to continue shopping
    Then the cart should contain 2 items
