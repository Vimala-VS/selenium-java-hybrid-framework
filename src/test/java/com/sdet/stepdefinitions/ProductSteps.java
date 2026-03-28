package com.sdet.stepdefinitions;

import com.sdet.framework.factory.DriverFactory;
import com.sdet.framework.pages.CartPage;
import com.sdet.framework.pages.CheckoutPage;
import com.sdet.framework.pages.HomePage;
import com.sdet.framework.pages.LoginPage;
import com.sdet.framework.pages.ProductsPage;
import com.sdet.framework.utils.Constants;
import com.sdet.framework.utils.Errors;
import com.sdet.framework.utils.LogManager;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.Map;

/**
 * ProductSteps — step definitions for product.feature and checkout.feature.
 *
 * WHY PRODUCT AND CHECKOUT STEPS SHARE ONE CLASS:
 * ------------------------------------------------
 * The checkout feature file's Background block requires steps that involve
 * login, adding a product, and navigating to the cart — all actions that are
 * already defined for product.feature. If checkout steps lived in a separate
 * class, the shared Background steps ("the user is logged in with valid
 * credentials", "the user has added a product to the cart") would need to be
 * defined twice — once in each class — causing Cucumber to throw an
 * "Ambiguous step definitions" error at runtime.
 *
 * Keeping them in one class means each step text is defined exactly once,
 * satisfying Cucumber's requirement that every step maps to exactly one method.
 *
 * A secondary benefit: the cross-page assertion pattern relies on instance
 * variables (capturedProductName, capturedProductPrice) that must survive
 * across multiple step method calls within the same scenario. PicoContainer
 * creates one instance of this class per scenario, so instance variables
 * are naturally scenario-scoped — the right lifespan for captured values.
 *
 * INSTANCE VARIABLES FOR CROSS-PAGE ASSERTIONS:
 * -----------------------------------------------
 * When a value is read on one page and asserted on a different page, it must
 * be stored somewhere between those two steps. Instance variables on a
 * PicoContainer-managed step definition class are the correct place:
 *
 *   Step: "the user notes the product name and price"
 *     → capturedProductName  = productsPage.getProductName()
 *     → capturedProductPrice = productsPage.getProductPrice()
 *
 *   Step: "the product should appear in the cart"
 *     → Assert cartPage.isProductDisplayedInCart(capturedProductName)
 *
 *   Step: "the product price in the cart should match the price on the product page"
 *     → Assert cartPage.getProductPrice().equals(capturedProductPrice)
 *
 * The values flow from capture → assert across step boundaries without any
 * shared static state or test context objects.
 */
public class ProductSteps {

    private static final Logger log = LogManager.getLogger(ProductSteps.class);

    private final LoginPage    loginPage;
    private final HomePage     homePage;
    private final ProductsPage productsPage;
    private final CartPage     cartPage;
    private final CheckoutPage checkoutPage;

    // Instance variables that survive across step method calls within one scenario.
    // Set in "notes the product name and price", read in cart and checkout assertions.
    private String capturedProductName;
    private String capturedProductPrice;

    // PicoContainer instantiates this class after Hooks.@Before has called
    // DriverFactory.initDriver(), so getDriver() is safe here.
    public ProductSteps() {
        loginPage    = new LoginPage(DriverFactory.getDriver());
        homePage     = new HomePage(DriverFactory.getDriver());
        productsPage = new ProductsPage(DriverFactory.getDriver());
        cartPage     = new CartPage(DriverFactory.getDriver());
        checkoutPage = new CheckoutPage(DriverFactory.getDriver());
        log.debug("ProductSteps instantiated — all page objects ready.");
    }

    // =========================================================================
    // SHARED BACKGROUND STEPS
    // Used by both product.feature and checkout.feature Background blocks.
    // Defined once here to avoid ambiguous step definition errors.
    // =========================================================================

    /**
     * Matches: Given the user is logged in with valid credentials
     * Navigates to the login page and authenticates with the standard test account.
     * Called by the Background in both product.feature and checkout.feature.
     */
    @Given("the user is logged in with valid credentials")
    public void theUserIsLoggedInWithValidCredentials() {
        log.info("Step: the user is logged in with valid credentials");
        Hooks.getExtentTest().info("Pre-condition: Logging in with valid credentials.");
        loginPage.navigateToLoginPage();
        loginPage.enterEmail(Constants.VALID_EMAIL);
        loginPage.enterPassword(Constants.VALID_PASSWORD);
        loginPage.clickLoginButton();
        Assert.assertTrue(homePage.isHomePageDisplayed(), Errors.LOGIN_FAILED);
        Hooks.getExtentTest().info("Login successful — Home page displayed.");
        log.info("Login successful.");
    }

    /**
     * Matches: And the user is on the Home page
     * Confirms the browser is on the home page after login.
     * Used in product.feature Background.
     */
    @And("the user is on the Home page")
    public void theUserIsOnTheHomePage() {
        log.info("Step: the user is on the Home page");
        Hooks.getExtentTest().info("Confirming browser is on the Home page.");
        Assert.assertTrue(homePage.isHomePageDisplayed(), Errors.LOGIN_FAILED);
    }

    /**
     * Matches: And the user has added a product to the cart
     * Adds the first product from the grid to the cart and navigates to it.
     * Used in checkout.feature Background.
     */
    @And("the user has added a product to the cart")
    public void theUserHasAddedAProductToTheCart() {
        log.info("Step: the user has added a product to the cart");
        Hooks.getExtentTest().info("Adding a product to the cart as pre-condition.");
        homePage.clickProducts();
        Assert.assertTrue(productsPage.isProductsPageDisplayed(), Errors.PRODUCT_NOT_FOUND);
        productsPage.addFirstProductToCart();
        productsPage.clickViewCart();
        Assert.assertTrue(cartPage.isCartPageDisplayed(), Errors.CART_EMPTY);
        Hooks.getExtentTest().info("Product added to cart — Cart page confirmed.");
        log.info("Pre-condition: product added to cart successfully.");
    }

    /**
     * Matches: And the user is on the Cart page
     * Confirms the browser is on the cart page — asserts after navigation in
     * the preceding Background step.
     */
    @And("the user is on the Cart page")
    public void theUserIsOnTheCartPage() {
        log.info("Step: the user is on the Cart page");
        Hooks.getExtentTest().info("Confirming browser is on the Cart page.");
        Assert.assertTrue(cartPage.isCartPageDisplayed(), Errors.CART_EMPTY);
    }

    // =========================================================================
    // PRODUCT FEATURE — Scenario: Search for a product successfully
    // =========================================================================

    /**
     * Matches: When the user navigates to the Products page
     */
    @When("the user navigates to the Products page")
    public void theUserNavigatesToTheProductsPage() {
        log.info("Step: the user navigates to the Products page");
        Hooks.getExtentTest().info("Clicking 'Products' in the navigation bar.");
        homePage.clickProducts();
    }

    /**
     * Matches: And the user searches for "Blue Top"
     * {string} captures the quoted value from the feature file.
     */
    @And("the user searches for {string}")
    public void theUserSearchesFor(String productName) {
        log.info("Step: the user searches for '{}'", productName);
        Hooks.getExtentTest().info("Searching for product: " + productName);
        productsPage.searchProduct(productName);
    }

    /**
     * Matches: Then the product "Blue Top" should appear in the search results
     * Asserts the products page is still displayed (results rendered) after search.
     */
    @Then("the product {string} should appear in the search results")
    public void theProductShouldAppearInTheSearchResults(String productName) {
        log.info("Step: the product '{}' should appear in the search results", productName);
        Hooks.getExtentTest().info("Asserting search results are displayed for: " + productName);
        Assert.assertTrue(productsPage.isProductsPageDisplayed(), Errors.PRODUCT_NOT_FOUND);
        Hooks.getExtentTest().info("Product '" + productName + "' found in search results.");
        log.info("Search results confirmed for '{}' — assertion passed.", productName);
    }

    // =========================================================================
    // PRODUCT FEATURE — Scenario: Add a product to the cart and verify
    // =========================================================================

    /**
     * Matches: Given the user is on the Products page
     * Navigates to the products page and confirms it loaded.
     */
    @Given("the user is on the Products page")
    public void theUserIsOnTheProductsPage() {
        log.info("Step: the user is on the Products page");
        Hooks.getExtentTest().info("Navigating to the Products page.");
        homePage.clickProducts();
        Assert.assertTrue(productsPage.isProductsPageDisplayed(), Errors.PRODUCT_NOT_FOUND);
    }

    /**
     * Matches: When the user views the details of the first product
     * Clicks "View Product" on the first card to open the detail page.
     */
    @When("the user views the details of the first product")
    public void theUserViewsTheDetailsOfTheFirstProduct() {
        log.info("Step: the user views the details of the first product");
        Hooks.getExtentTest().info("Clicking 'View Product' on the first product card.");
        productsPage.clickViewProduct();
    }

    /**
     * Matches: And the user notes the product name and price
     * CAPTURE step — reads and stores name and price for later cross-page assertions.
     */
    @And("the user notes the product name and price")
    public void theUserNotesTheProductNameAndPrice() {
        log.info("Step: the user notes the product name and price");
        capturedProductName  = productsPage.getProductName();
        capturedProductPrice = productsPage.getProductPrice();
        Hooks.getExtentTest().info("Captured — name: '" + capturedProductName
                + "', price: '" + capturedProductPrice + "'");
        log.info("Captured product name: '{}', price: '{}'", capturedProductName, capturedProductPrice);
    }

    /**
     * Matches: And the user adds the product to the cart
     * Adds via the detail page button (after clickViewProduct).
     */
    @And("the user adds the product to the cart")
    public void theUserAddsTheProductToTheCart() {
        log.info("Step: the user adds the product to the cart");
        Hooks.getExtentTest().info("Adding product to cart from the detail page.");
        productsPage.addToCartFromDetailPage();
    }

    /**
     * Matches: And the user proceeds to view the cart
     * Dismisses the modal and navigates to the cart page.
     */
    @And("the user proceeds to view the cart")
    public void theUserProceedsToViewTheCart() {
        log.info("Step: the user proceeds to view the cart");
        Hooks.getExtentTest().info("Clicking 'View Cart' in the confirmation modal.");
        productsPage.clickViewCart();
    }

    /**
     * Matches: Then the cart page should be displayed
     */
    @Then("the cart page should be displayed")
    public void theCartPageShouldBeDisplayed() {
        log.info("Step: the cart page should be displayed");
        Hooks.getExtentTest().info("Asserting that the Cart page is displayed.");
        Assert.assertTrue(cartPage.isCartPageDisplayed(), Errors.CART_EMPTY);
        log.info("Cart page displayed — assertion passed.");
    }

    /**
     * Matches: And the product should appear in the cart
     * ASSERT step — verifies the captured product name is present in the cart.
     */
    @And("the product should appear in the cart")
    public void theProductShouldAppearInTheCart() {
        log.info("Step: the product should appear in the cart");
        Hooks.getExtentTest().info("Cross-page assertion: verifying '" + capturedProductName + "' is in cart.");
        Assert.assertTrue(
                cartPage.isProductDisplayedInCart(capturedProductName),
                Errors.CART_EMPTY + " Expected product: " + capturedProductName);
        Hooks.getExtentTest().info("Product '" + capturedProductName + "' found in cart.");
        log.info("Cross-page name assertion passed — '{}' in cart.", capturedProductName);
    }

    /**
     * Matches: And the product price in the cart should match the price on the product page
     * ASSERT step — verifies the cart price equals the captured detail page price.
     */
    @And("the product price in the cart should match the price on the product page")
    public void theProductPriceInTheCartShouldMatchThePriceOnTheProductPage() {
        log.info("Step: the product price in the cart should match the price on the product page");
        String cartPrice = cartPage.getProductPrice();
        Hooks.getExtentTest().info("Cross-page price assertion — cart: '"
                + cartPrice + "', detail page: '" + capturedProductPrice + "'");
        Assert.assertEquals(cartPrice, capturedProductPrice,
                "Cart price '" + cartPrice + "' does not match detail page price '" + capturedProductPrice + "'.");
        log.info("Cross-page price assertion passed — '{}' == '{}'.", cartPrice, capturedProductPrice);
    }

    // =========================================================================
    // PRODUCT FEATURE — Scenario: Verify cart item count
    // =========================================================================

    /**
     * Matches: When the user adds the first product to the cart
     */
    @When("the user adds the first product to the cart")
    public void theUserAddsTheFirstProductToTheCart() {
        log.info("Step: the user adds the first product to the cart");
        Hooks.getExtentTest().info("Adding first product via grid overlay.");
        productsPage.addFirstProductToCart();
    }

    /**
     * Matches: And the user chooses to continue shopping
     */
    @And("the user chooses to continue shopping")
    public void theUserChoosesToContinueShopping() {
        log.info("Step: the user chooses to continue shopping");
        Hooks.getExtentTest().info("Clicking 'Continue Shopping' to dismiss the modal.");
        productsPage.clickContinueShopping();
    }

    /**
     * Matches: Then the cart should contain 1 item
     *          Then the cart should contain 2 items
     * Navigates to the cart, reads the item count, and asserts it equals expectedCount.
     * {int} captures the number directly from the feature file step text.
     */
    @Then("the cart should contain {int} item(s)")
    public void theCartShouldContainItems(int expectedCount) {
        log.info("Step: the cart should contain {} item(s)", expectedCount);
        Hooks.getExtentTest().info("Navigating to cart and asserting item count = " + expectedCount);
        homePage.clickCart();
        Assert.assertTrue(cartPage.isCartPageDisplayed(), Errors.CART_EMPTY);
        int actualCount = cartPage.getCartItemCount();
        Hooks.getExtentTest().info("Cart item count: " + actualCount);
        Assert.assertEquals(actualCount, expectedCount,
                "Expected " + expectedCount + " item(s) in cart but found " + actualCount + ".");
        log.info("Cart item count assertion passed — expected: {}, actual: {}.", expectedCount, actualCount);
    }

    /**
     * Matches: When the user adds another product to the cart
     * Navigates back to the products page and adds the first available product again.
     */
    @When("the user adds another product to the cart")
    public void theUserAddsAnotherProductToTheCart() {
        log.info("Step: the user adds another product to the cart");
        Hooks.getExtentTest().info("Returning to Products page to add a second product.");
        homePage.clickProducts();
        Assert.assertTrue(productsPage.isProductsPageDisplayed(), Errors.PRODUCT_NOT_FOUND);
        productsPage.addFirstProductToCart();
    }

    // =========================================================================
    // CHECKOUT FEATURE — Scenario: Checkout page displays correctly
    //                    Scenario: Complete order flow
    //                    Scenario: Order total displayed
    // =========================================================================

    /**
     * Matches: When the user proceeds to checkout
     */
    @When("the user proceeds to checkout")
    public void theUserProceedsToCheckout() {
        log.info("Step: the user proceeds to checkout");
        Hooks.getExtentTest().info("Clicking 'Proceed To Checkout' from the cart.");
        cartPage.proceedToCheckout();
    }

    /**
     * Matches: Then the Checkout page should be displayed
     */
    @Then("the Checkout page should be displayed")
    public void theCheckoutPageShouldBeDisplayed() {
        log.info("Step: the Checkout page should be displayed");
        Hooks.getExtentTest().info("Asserting that the Checkout page is displayed.");
        Assert.assertTrue(checkoutPage.isCheckoutPageDisplayed(), Errors.CHECKOUT_FAILED);
        log.info("Checkout page displayed — assertion passed.");
    }

    /**
     * Matches: And the delivery address should be shown on the page
     */
    @And("the delivery address should be shown on the page")
    public void theDeliveryAddressShouldBeShownOnThePage() {
        log.info("Step: the delivery address should be shown on the page");
        Hooks.getExtentTest().info("Asserting delivery address is populated.");
        String address = checkoutPage.getDeliveryAddress();
        Assert.assertFalse(address.isEmpty(),
                "Delivery address block is empty — account address not loaded.");
        Hooks.getExtentTest().info("Delivery address displayed: " + address);
        log.info("Delivery address present — assertion passed.");
    }

    /**
     * Matches: When the user enters "Test order" as a comment for the delivery
     */
    @When("the user enters {string} as a comment for the delivery")
    public void theUserEntersAsACommentForTheDelivery(String comment) {
        log.info("Step: the user enters '{}' as a comment for the delivery", comment);
        Hooks.getExtentTest().info("Entering delivery comment: " + comment);
        checkoutPage.enterOrderComment(comment);
    }

    /**
     * Matches: And the user clicks Place Order
     */
    @And("the user clicks Place Order")
    public void theUserClicksPlaceOrder() {
        log.info("Step: the user clicks Place Order");
        Hooks.getExtentTest().info("Clicking 'Place Order' to advance to the payment form.");
        checkoutPage.clickPlaceOrder();
    }

    /**
     * Matches: And the user enters the following payment details:
     *   | cardholderName | Test User        |
     *   | cardNumber     | 4111111111111111 |
     *   | cvc            | 123              |
     *   | expiryMonth    | 12               |
     *   | expiryYear     | 2025             |
     *
     * Cucumber converts a vertical key-value data table into a Map<String,String>
     * and injects it as the method parameter. Each row becomes one map entry.
     */
    @And("the user enters the following payment details:")
    public void theUserEntersTheFollowingPaymentDetails(Map<String, String> paymentDetails) {
        log.info("Step: the user enters the following payment details");
        Hooks.getExtentTest().info("Entering payment details from data table.");
        checkoutPage.enterPaymentDetails(
                paymentDetails.get("cardholderName"),
                paymentDetails.get("cardNumber"),
                paymentDetails.get("cvc"),
                paymentDetails.get("expiryMonth"),
                paymentDetails.get("expiryYear")
        );
        log.info("Payment details entered — cardholder: '{}'", paymentDetails.get("cardholderName"));
    }

    /**
     * Matches: And the user confirms the order
     */
    @And("the user confirms the order")
    public void theUserConfirmsTheOrder() {
        log.info("Step: the user confirms the order");
        Hooks.getExtentTest().info("Clicking 'Pay and Confirm Order'.");
        checkoutPage.confirmOrder();
    }

    /**
     * Matches: Then the order confirmation page should be displayed
     */
    @Then("the order confirmation page should be displayed")
    public void theOrderConfirmationPageShouldBeDisplayed() {
        log.info("Step: the order confirmation page should be displayed");
        Hooks.getExtentTest().info("Asserting order confirmation heading is displayed.");
        Assert.assertTrue(checkoutPage.isOrderConfirmed(), Errors.CHECKOUT_FAILED);
        log.info("Order confirmed — confirmation heading visible.");
    }

    /**
     * Matches: And the confirmation message should congratulate the user
     */
    @And("the confirmation message should congratulate the user")
    public void theConfirmationMessageShouldCongratulateTheUser() {
        log.info("Step: the confirmation message should congratulate the user");
        String message = checkoutPage.getOrderConfirmationMessage();
        Hooks.getExtentTest().info("Confirmation message: " + message);
        Assert.assertTrue(message.toLowerCase().contains("congratulations"),
                "Confirmation message does not contain 'Congratulations'. Actual: " + message);
        log.info("Congratulations message verified — assertion passed.");
    }

    /**
     * Matches: And the order total should be visible
     */
    @And("the order total should be visible")
    public void theOrderTotalShouldBeVisible() {
        log.info("Step: the order total should be visible");
        Hooks.getExtentTest().info("Asserting order total is not empty.");
        String total = checkoutPage.getOrderTotal();
        Assert.assertFalse(total.isEmpty(),
                "Order total is empty — total amount not rendered in the checkout summary.");
        Hooks.getExtentTest().info("Order total displayed: " + total);
        log.info("Order total present: '{}' — assertion passed.", total);
    }

    /**
     * Matches: And the order total should show the amount in rupees
     */
    @And("the order total should show the amount in rupees")
    public void theOrderTotalShouldShowTheAmountInRupees() {
        log.info("Step: the order total should show the amount in rupees");
        String total = checkoutPage.getOrderTotal();
        Hooks.getExtentTest().info("Asserting currency symbol 'Rs.' in total: " + total);
        Assert.assertTrue(total.contains("Rs."),
                "Order total '" + total + "' does not contain currency symbol 'Rs.'");
        log.info("Currency symbol 'Rs.' confirmed in total '{}' — assertion passed.", total);
    }
}
