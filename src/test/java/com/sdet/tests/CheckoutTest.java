package com.sdet.tests;

import com.sdet.framework.base.BaseTest;
import com.sdet.framework.pages.CartPage;
import com.sdet.framework.pages.CheckoutPage;
import com.sdet.framework.pages.HomePage;
import com.sdet.framework.pages.LoginPage;
import com.sdet.framework.pages.ProductsPage;
import com.sdet.framework.utils.Constants;
import com.sdet.framework.utils.Errors;
import com.sdet.framework.utils.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * CheckoutTest — end-to-end tests covering the checkout flow on
 * automationexercise.com from a cart with items through to order confirmation.
 *
 * WHY @BeforeMethod SETS UP CART STATE:
 * --------------------------------------
 * Each checkout test needs a browser session that is:
 *   1. Authenticated (logged in)
 *   2. Has at least one product in the cart
 *   3. Is sitting on the cart page, ready to proceed to checkout
 *
 * Putting this setup in @BeforeMethod rather than inside each @Test method
 * enforces the single-responsibility principle: every @Test describes ONE
 * checkout behaviour, not "login + add product + checkout behaviour". This
 * makes test failures unambiguous — if testCompleteOrderFlow() fails, the
 * cause is in the checkout flow, not in authentication or cart management,
 * because those steps ran in @BeforeMethod and are reported separately.
 *
 * If @BeforeMethod itself fails (e.g. login broken), TestNG marks the setup
 * as failed and skips the @Test body entirely, producing a clear "pre-condition
 * failed" entry in the report rather than a misleading failure inside the test.
 *
 * WHY END-TO-END CHECKOUT TESTS MATTER:
 * ---------------------------------------
 * Unit and integration tests verify individual components (login works, cart
 * updates correctly). End-to-end tests verify that those components work
 * together in a real user journey. The checkout flow crosses five pages
 * (login → home → products → cart → checkout) and involves four server-side
 * operations (authenticate, add item, load address, process payment). A defect
 * in the data handoff between any two pages would not be caught by component
 * tests alone. Only an end-to-end test that walks the complete path will catch:
 *   - Address not loaded from the user account into the checkout summary.
 *   - Cart total not matching the sum of items after navigating to checkout.
 *   - Payment form fields not accepting input due to a JS validation regression.
 *   - Order confirmation page not appearing after a valid payment submission.
 */
public class CheckoutTest extends BaseTest {

    private static final Logger log = LogManager.getLogger(CheckoutTest.class);

    private LoginPage    loginPage;
    private HomePage     homePage;
    private ProductsPage productsPage;
    private CartPage     cartPage;
    private CheckoutPage checkoutPage;

    // -------------------------------------------------------------------------
    // @BeforeMethod — builds the full pre-condition state every test in this
    // class needs: authenticated session + one product in cart + on cart page.
    //
    // Sequence:
    //   1. Instantiate all page objects with the current thread's driver.
    //   2. Navigate to login page and authenticate.
    //   3. Go to the products page and add the first product to cart.
    //   4. Navigate to the cart and confirm the product is present.
    //
    // alwaysRun = true: ensures setup runs even when tests are filtered by group
    // so page objects are never null when a test starts.
    // -------------------------------------------------------------------------
    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        log.debug("Initialising page objects for CheckoutTest.");
        loginPage    = new LoginPage(getDriver());
        homePage     = new HomePage(getDriver());
        productsPage = new ProductsPage(getDriver());
        cartPage     = new CartPage(getDriver());
        checkoutPage = new CheckoutPage(getDriver());

        // --- Step 1: Authenticate ---
        log.info("Pre-condition: Logging in with valid credentials.");
        extentTest.info("Pre-condition: Navigating to Login page.");
        loginPage.navigateToLoginPage();
        loginPage.enterEmail(Constants.VALID_EMAIL);
        loginPage.enterPassword(Constants.VALID_PASSWORD);
        loginPage.clickLoginButton();

        Assert.assertTrue(homePage.isHomePageDisplayed(), Errors.LOGIN_FAILED);
        extentTest.info("Pre-condition: Login successful.");
        log.info("Pre-condition: Login successful.");

        // --- Step 2: Add a product to the cart ---
        log.info("Pre-condition: Navigating to Products page and adding first product to cart.");
        extentTest.info("Pre-condition: Navigating to Products page.");
        homePage.clickProducts();
        Assert.assertTrue(productsPage.isProductsPageDisplayed(), Errors.PRODUCT_NOT_FOUND);

        extentTest.info("Pre-condition: Adding first product to cart.");
        productsPage.addFirstProductToCart();

        // Navigate to cart via the modal link rather than the nav bar so we
        // dismiss the modal and land on the cart page in one action.
        extentTest.info("Pre-condition: Clicking 'View Cart' to navigate to cart page.");
        productsPage.clickViewCart();

        // --- Step 3: Confirm cart state before handing off to the @Test ---
        Assert.assertTrue(cartPage.isCartPageDisplayed(), Errors.CART_EMPTY);
        Assert.assertEquals(cartPage.getCartItemCount(), 1,
                "Pre-condition failed: expected 1 item in cart before checkout test, but found: "
                + cartPage.getCartItemCount());
        extentTest.info("Pre-condition: Cart confirmed — 1 item present. Ready for checkout.");
        log.info("Pre-condition complete: authenticated session with 1 item in cart.");
    }

    // =========================================================================
    // TESTS
    // =========================================================================

    /**
     * testCheckoutPageDisplayed — verifies that clicking "Proceed To Checkout"
     * from a cart with items lands the authenticated user on the checkout page
     * and that a delivery address is populated from the account.
     *
     * Sanity group: the most fundamental checkout assertion. If this fails,
     * no other checkout test can meaningfully run.
     *
     * Steps:
     *   1. Click Proceed To Checkout from the cart.
     *   2. Assert the checkout page heading is visible.
     *   3. Assert the delivery address block is not empty.
     */
    @Test(groups = {"sanity", "regression"}, description = "Checkout page loads with delivery address populated")
    public void testCheckoutPageDisplayed() {
        log.info("=== TEST: testCheckoutPageDisplayed ===");

        extentTest.info("Clicking 'Proceed To Checkout' from cart.");
        cartPage.proceedToCheckout();

        extentTest.info("Asserting Checkout page is displayed.");
        Assert.assertTrue(checkoutPage.isCheckoutPageDisplayed(), Errors.CHECKOUT_FAILED);
        log.info("Checkout page displayed — assertion passed.");

        extentTest.info("Asserting delivery address is populated.");
        String deliveryAddress = checkoutPage.getDeliveryAddress();
        extentTest.info("Delivery address retrieved: " + deliveryAddress);
        Assert.assertFalse(deliveryAddress.isEmpty(),
                "Delivery address block is empty — account address not loaded on checkout page.");
        log.info("Delivery address present: '{}' — assertion passed.", deliveryAddress);

        extentTest.pass("testCheckoutPageDisplayed passed — checkout page loaded with address.");
    }

    /**
     * testCompleteOrderFlow — verifies the full end-to-end purchase journey from
     * the cart page through address review, payment entry, and order confirmation.
     *
     * This is the most critical test in the suite because it exercises every
     * server-side operation in a single user journey. A pass here provides
     * high confidence that the purchase flow is fully operational.
     *
     * Steps:
     *   1. Proceed to checkout — verify checkout page.
     *   2. Capture and assert delivery address is present.
     *   3. Enter an order comment.
     *   4. Click Place Order to advance to payment.
     *   5. Fill all payment fields.
     *   6. Click Pay and Confirm Order.
     *   7. Assert order confirmation heading is displayed.
     *   8. Assert confirmation message content contains expected text.
     */
    @Test(groups = {"regression"}, description = "Full order flow from checkout to confirmation")
    public void testCompleteOrderFlow() {
        log.info("=== TEST: testCompleteOrderFlow ===");

        extentTest.info("Clicking 'Proceed To Checkout' from cart.");
        cartPage.proceedToCheckout();

        extentTest.info("Asserting Checkout page is displayed.");
        Assert.assertTrue(checkoutPage.isCheckoutPageDisplayed(), Errors.CHECKOUT_FAILED);

        extentTest.info("Capturing and asserting delivery address.");
        String deliveryAddress = checkoutPage.getDeliveryAddress();
        Assert.assertFalse(deliveryAddress.isEmpty(),
                "Delivery address is empty — cannot proceed with order.");
        extentTest.info("Delivery address confirmed: " + deliveryAddress);
        log.info("Delivery address present: '{}'", deliveryAddress);

        extentTest.info("Entering order comment: 'Test order'");
        checkoutPage.enterOrderComment("Test order");

        extentTest.info("Clicking 'Place Order' to advance to the payment form.");
        checkoutPage.clickPlaceOrder();
        log.info("Place Order clicked — payment form should now be visible.");

        extentTest.info("Entering payment details.");
        checkoutPage.enterPaymentDetails(
                "Test User",
                "4111111111111111",
                "123",
                "12",
                "2025"
        );
        log.info("Payment details entered — cardholder: 'Test User', card ending 1111.");

        extentTest.info("Clicking 'Pay and Confirm Order'.");
        checkoutPage.confirmOrder();
        log.info("Confirm Order clicked — awaiting confirmation page.");

        extentTest.info("Asserting order confirmation is displayed.");
        Assert.assertTrue(checkoutPage.isOrderConfirmed(), Errors.CHECKOUT_FAILED);
        log.info("Order confirmed — confirmation heading visible.");

        extentTest.info("Asserting confirmation message content.");
        String confirmationMessage = checkoutPage.getOrderConfirmationMessage();
        extentTest.info("Confirmation message: " + confirmationMessage);
        Assert.assertFalse(confirmationMessage.isEmpty(),
                "Order confirmation message is empty after successful order.");
        Assert.assertTrue(
                confirmationMessage.toLowerCase().contains("congratulations"),
                "Confirmation message does not contain 'Congratulations'. Actual: " + confirmationMessage);
        log.info("Confirmation message verified: '{}'", confirmationMessage);

        extentTest.pass("testCompleteOrderFlow passed — order placed and confirmed successfully.");
    }

    /**
     * testOrderTotal — verifies that the order total displayed on the checkout
     * summary is populated and contains the expected currency symbol.
     *
     * WHY A SEPARATE TOTAL TEST?
     * testCompleteOrderFlow verifies the confirmation page, not the order summary.
     * This test specifically guards against the total field being empty or showing
     * a raw number without a currency prefix — both of which indicate a rendering
     * or locale defect that would not fail the confirmation assertion.
     *
     * Steps:
     *   1. Proceed to checkout.
     *   2. Capture the order total string.
     *   3. Assert it is not empty.
     *   4. Assert it contains "Rs." (the currency symbol used by this site).
     */
    @Test(groups = {"regression"}, description = "Order total is displayed with correct currency symbol")
    public void testOrderTotal() {
        log.info("=== TEST: testOrderTotal ===");

        extentTest.info("Clicking 'Proceed To Checkout' from cart.");
        cartPage.proceedToCheckout();

        extentTest.info("Asserting Checkout page is displayed.");
        Assert.assertTrue(checkoutPage.isCheckoutPageDisplayed(), Errors.CHECKOUT_FAILED);

        extentTest.info("Capturing order total from checkout summary.");
        String orderTotal = checkoutPage.getOrderTotal();
        extentTest.info("Order total displayed: '" + orderTotal + "'");
        log.info("Order total captured: '{}'", orderTotal);

        extentTest.info("Asserting order total is not empty.");
        Assert.assertFalse(orderTotal.isEmpty(),
                "Order total is empty — total amount not rendered in the checkout summary.");
        log.info("Order total is not empty — assertion passed.");

        extentTest.info("Asserting order total contains currency symbol 'Rs.'");
        Assert.assertTrue(orderTotal.contains("Rs."),
                "Order total '" + orderTotal + "' does not contain expected currency symbol 'Rs.'");
        log.info("Currency symbol 'Rs.' present in total '{}' — assertion passed.", orderTotal);

        extentTest.pass("testOrderTotal passed — total '" + orderTotal + "' is present and correctly formatted.");
    }
}
