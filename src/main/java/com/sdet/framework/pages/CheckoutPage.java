package com.sdet.framework.pages;

import com.sdet.framework.base.BasePage;
import com.sdet.framework.utils.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * CheckoutPage — Page Object for https://www.automationexercise.com/checkout
 *
 * FULL CHECKOUT FLOW ON AUTOMATIONEXERCISE.COM:
 * ---------------------------------------------
 * The checkout process spans two distinct page states that are handled by this
 * single class. The URL stays at /checkout throughout; the page content changes
 * as the user progresses through each step.
 *
 * STEP 1 — Order Review (isCheckoutPageDisplayed):
 *   The page loads showing:
 *   - Delivery address pulled from the logged-in user's account details.
 *   - Billing address (same as delivery on this site).
 *   - An order summary table listing every product in the cart with its price.
 *   - A comment textarea for optional delivery instructions.
 *   - A "Place Order" button that advances to the payment step.
 *
 *   NOTE: The checkout page is only reachable by a logged-in user. If the user
 *   is not logged in, CartPage.proceedToCheckout() redirects to a modal asking
 *   to log in or continue as guest. Tests must ensure the user is authenticated
 *   before reaching this page.
 *
 * STEP 2 — Payment (after clicking Place Order):
 *   The page transitions to a payment form containing:
 *   - Cardholder name
 *   - Card number
 *   - CVC code
 *   - Expiry month and year
 *   - "Pay and Confirm Order" button
 *
 *   On submission, the site processes the (simulated) payment and redirects to
 *   an order confirmation view showing "Congratulations! Your order has been
 *   confirmed!" and a success message.
 *
 * STEP 3 — Order Confirmation (isOrderConfirmed):
 *   After confirmOrder() is called, the page shows the success state.
 *   isOrderConfirmed() and getOrderConfirmationMessage() verify this final state.
 *
 * This single class covers all three steps because the URL does not change
 * between them — splitting by URL would create unnecessary classes for what
 * is logically one checkout journey.
 */
public class CheckoutPage extends BasePage {

    private static final Logger log = LogManager.getLogger(CheckoutPage.class);

    // =========================================================================
    // LOCATORS — private, static, final.
    // Grouped by checkout step for readability.
    // =========================================================================

    // --- Step 1: Order Review ------------------------------------------------

    // Heading that confirms the order review step is displayed.
    private static final By CHECKOUT_HEADING = By.xpath("//h2[text()='Checkout']");

    // Full delivery address block shown in the left address panel.
    // The li elements inside this ul compose the full address — getText() on the
    // ul returns all lines concatenated, which is sufficient for address assertions.
    private static final By DELIVERY_ADDRESS = By.cssSelector("#address_delivery");

    // Full billing address block shown in the right address panel.
    private static final By BILLING_ADDRESS = By.cssSelector("#address_invoice");

    // Product name cell in the order summary table.
    private static final By ORDER_PRODUCT_NAME = By.cssSelector("td.cart_description h4 a");

    // Product price cell in the order summary table.
    private static final By ORDER_PRODUCT_PRICE = By.cssSelector("td.cart_price p");

    // Total amount row at the bottom of the order summary (e.g. "Rs. 1500").
    private static final By ORDER_TOTAL = By.cssSelector("td.cart_total p.cart_total_price");

    // Optional delivery comment textarea above the Place Order button.
    private static final By ORDER_COMMENT = By.cssSelector("textarea[name='message']");

    // "Place Order" button — advances the page to the payment form (Step 2).
    private static final By PLACE_ORDER_BUTTON = By.xpath("//a[text()='Place Order']");

    // --- Step 2: Payment -----------------------------------------------------

    // Cardholder name input on the payment form.
    private static final By PAYMENT_NAME = By.cssSelector("input[data-qa='name-on-card']");

    // 16-digit card number input.
    private static final By PAYMENT_CARD_NUMBER = By.cssSelector("input[data-qa='card-number']");

    // 3-digit CVC / CVV input.
    private static final By PAYMENT_CVC = By.cssSelector("input[data-qa='cvc']");

    // Expiry month (MM) input — expects a 2-digit month string, e.g. "03".
    private static final By PAYMENT_EXPIRY_MONTH = By.cssSelector("input[data-qa='expiry-month']");

    // Expiry year (YYYY) input — expects a 4-digit year string, e.g. "2027".
    private static final By PAYMENT_EXPIRY_YEAR = By.cssSelector("input[data-qa='expiry-year']");

    // "Pay and Confirm Order" submit button on the payment form.
    private static final By CONFIRM_ORDER_BUTTON = By.cssSelector("button[data-qa='pay-button']");

    // --- Step 3: Order Confirmation ------------------------------------------

    // Success heading shown after payment is accepted.
    private static final By ORDER_CONFIRMATION_HEADING = By.cssSelector("h2[data-qa='order-placed']");

    // Paragraph below the heading with the full confirmation message.
    private static final By ORDER_CONFIRMATION_MESSAGE = By.xpath(
            "//p[contains(text(),'Congratulations! Your order has been confirmed!')]");

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public CheckoutPage(WebDriver driver) {
        super(driver);
        log.debug("CheckoutPage instantiated.");
    }

    // =========================================================================
    // ACTION METHODS
    // =========================================================================

    /**
     * isCheckoutPageDisplayed() — returns true when the "Checkout" heading is
     * visible, confirming the browser is on the order review step.
     *
     * Call this immediately after CartPage.proceedToCheckout() to confirm the
     * user was authenticated and the checkout page loaded correctly before
     * attempting to read address details or place the order.
     */
    public boolean isCheckoutPageDisplayed() {
        log.info("Checking if Checkout page is displayed.");
        return isElementDisplayed(CHECKOUT_HEADING);
    }

    /**
     * getDeliveryAddress() — returns the full delivery address block text.
     *
     * The text includes all address lines joined by newlines (name, street,
     * city, state, zip, country, phone). Used in cross-page assertions to
     * confirm the address displayed matches what was entered during registration.
     */
    public String getDeliveryAddress() {
        log.info("Retrieving delivery address from checkout page.");
        String address = getElementText(DELIVERY_ADDRESS);
        log.debug("Delivery address: '{}'", address);
        return address;
    }

    /**
     * getBillingAddress() — returns the full billing address block text.
     *
     * On automationexercise.com the billing address mirrors the delivery address
     * from the user's account. This method is provided so tests can assert both
     * panels independently if needed.
     */
    public String getBillingAddress() {
        log.info("Retrieving billing address from checkout page.");
        String address = getElementText(BILLING_ADDRESS);
        log.debug("Billing address: '{}'", address);
        return address;
    }

    /**
     * enterOrderComment(comment) — types an optional delivery note into the
     * comment textarea above the Place Order button.
     */
    public void enterOrderComment(String comment) {
        log.info("Entering order comment: '{}'", comment);
        enterText(ORDER_COMMENT, comment);
    }

    /**
     * clickPlaceOrder() — submits the order review form and advances to the
     * payment step.
     *
     * After this call the payment form fields become visible. The test must
     * call enterPaymentDetails() and then confirmOrder() to complete the purchase.
     */
    public void clickPlaceOrder() {
        log.info("Clicking 'Place Order' button.");
        clickElement(PLACE_ORDER_BUTTON);
        log.debug("Navigated to payment form (Step 2).");
    }

    /**
     * enterPaymentDetails(name, cardNumber, cvc, expiryMonth, expiryYear) —
     * fills in all five payment form fields in the correct order.
     *
     * Parameters:
     *   name         — cardholder name as it appears on the card
     *   cardNumber   — 16-digit card number (no spaces or dashes)
     *   cvc          — 3-digit security code
     *   expiryMonth  — 2-digit month, e.g. "03"
     *   expiryYear   — 4-digit year, e.g. "2027"
     *
     * All five fields must be filled before calling confirmOrder(); submitting
     * with any field blank causes a browser-level validation error.
     */
    public void enterPaymentDetails(String name, String cardNumber,
                                    String cvc, String expiryMonth, String expiryYear) {
        log.info("Entering payment details for cardholder: '{}'", name);
        enterText(PAYMENT_NAME, name);
        enterText(PAYMENT_CARD_NUMBER, cardNumber);
        enterText(PAYMENT_CVC, cvc);
        enterText(PAYMENT_EXPIRY_MONTH, expiryMonth);
        enterText(PAYMENT_EXPIRY_YEAR, expiryYear);
        log.debug("All payment fields populated.");
    }

    /**
     * confirmOrder() — clicks the "Pay and Confirm Order" button.
     *
     * On success the page transitions to the order confirmation state (Step 3)
     * and displays the success heading and message. Call isOrderConfirmed()
     * after this to assert the order completed successfully.
     */
    public void confirmOrder() {
        log.info("Clicking 'Pay and Confirm Order' button.");
        clickElement(CONFIRM_ORDER_BUTTON);
        log.debug("Order confirmation submitted.");
    }

    /**
     * isOrderConfirmed() — returns true when the order confirmation heading
     * ("Order Placed!") is visible after payment is accepted.
     *
     * This is the terminal assertion of the entire checkout flow. If it returns
     * false the test knows the order did not complete — without needing to parse
     * any error message — and can fail with a clear, intentional assertion error.
     */
    public boolean isOrderConfirmed() {
        log.info("Checking if order confirmation is displayed.");
        boolean confirmed = isElementDisplayed(ORDER_CONFIRMATION_HEADING);
        log.debug("Order confirmed: {}", confirmed);
        return confirmed;
    }

    /**
     * getOrderConfirmationMessage() — returns the full confirmation paragraph
     * text shown after a successful order.
     *
     * Used to assert the exact wording of the success message — useful for
     * catching regressions where the message text changes unexpectedly.
     */
    public String getOrderConfirmationMessage() {
        log.info("Retrieving order confirmation message.");
        String message = getElementText(ORDER_CONFIRMATION_MESSAGE);
        log.debug("Order confirmation message: '{}'", message);
        return message;
    }

    /**
     * getOrderSummaryProductName() — returns the product name from the order
     * summary table on the checkout review page (Step 1).
     *
     * Used in cross-page assertions to confirm the item displayed in the
     * checkout summary matches the product that was added from ProductsPage
     * or CartPage — catching any data-loss between the cart and checkout.
     */
    public String getOrderSummaryProductName() {
        log.info("Retrieving product name from order summary.");
        String name = getElementText(ORDER_PRODUCT_NAME);
        log.debug("Order summary product name: '{}'", name);
        return name;
    }

    /**
     * getOrderSummaryProductPrice() — returns the unit price string from the
     * order summary table (e.g. "Rs. 500").
     *
     * Used alongside getOrderSummaryProductName() to assert the checkout summary
     * reflects the correct product and price before the order is placed.
     */
    public String getOrderSummaryProductPrice() {
        log.info("Retrieving product price from order summary.");
        String price = getElementText(ORDER_PRODUCT_PRICE);
        log.debug("Order summary product price: '{}'", price);
        return price;
    }

    /**
     * getOrderTotal() — returns the total price string from the order summary
     * (e.g. "Rs. 1500").
     *
     * Used in cross-page assertions to verify the total displayed at checkout
     * matches the sum of items that were in the cart before proceeding.
     */
    public String getOrderTotal() {
        log.info("Retrieving order total from checkout summary.");
        String total = getElementText(ORDER_TOTAL);
        log.debug("Order total: '{}'", total);
        return total;
    }
}
