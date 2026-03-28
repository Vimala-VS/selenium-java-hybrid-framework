package com.sdet.framework.utils;

/**
 * Errors — centralised repository of assertion failure messages used across
 * the entire test suite.
 *
 * WHY CENTRALISE ERROR MESSAGES?
 * --------------------------------
 * When an assertion fails, the message printed in the test report and console
 * is the primary diagnostic tool. Poor messages like "expected true but was false"
 * tell the reader nothing about what went wrong or where to look. Good messages
 * like "Login failed — home page not displayed after submitting valid credentials"
 * let a developer or CI engineer understand the failure without opening the test code.
 *
 * Centralising messages here solves two additional problems:
 *
 *   1. CONSISTENCY — the same scenario failing in different test classes always
 *      produces the same message. Reports become searchable: grep for
 *      "Product not found in search results" to find every occurrence across runs.
 *
 *   2. MAINTENANCE — when a message needs to be improved (e.g. made more specific
 *      for a new team member), the change is made once here and every assertion
 *      that uses it immediately benefits.
 *
 * Usage in step definitions or test classes:
 *
 *   Assert.assertTrue(homePage.isUserLoggedIn(), Errors.USER_NOT_LOGGED_IN);
 *   Assert.assertTrue(cartPage.isProductDisplayedInCart(name), Errors.CART_EMPTY);
 *   Assert.assertEquals(page.getPageTitle(), expected, Errors.PAGE_TITLE_MISMATCH);
 *
 * The message is only printed when the assertion FAILS, so it always describes
 * a failure state — write them from the perspective of what went wrong.
 */
public class Errors {

    // Prevent instantiation — static constants holder only.
    private Errors() {}

    // =========================================================================
    // AUTHENTICATION ERRORS
    // =========================================================================

    // Displayed when a valid login attempt does not redirect to the home page.
    // Indicates the login action itself failed or the redirect did not occur.
    public static final String LOGIN_FAILED = "Login failed — home page not displayed after submitting valid credentials.";

    // Displayed when submitting invalid credentials does not show an error message.
    // Indicates the application silently accepted bad credentials (security risk)
    // or the error element locator has changed.
    public static final String INVALID_LOGIN = "Error message not displayed for invalid login credentials.";

    // Displayed when no active user session is detected on the home page.
    // Used after actions that require authentication to confirm the user
    // is still logged in and the session was not dropped unexpectedly.
    public static final String USER_NOT_LOGGED_IN = "User is not logged in — 'Logged in as' indicator not visible in navigation bar.";

    // =========================================================================
    // PRODUCT ERRORS
    // =========================================================================

    // Displayed when a product search returns no visible results.
    // Could indicate a missing product, a broken search filter, or a locator
    // change on the products page.
    public static final String PRODUCT_NOT_FOUND = "Product not found in search results — no matching product card displayed after search.";

    // =========================================================================
    // CART ERRORS
    // =========================================================================

    // Displayed when the expected product is not visible on the cart page.
    // Used in cross-page assertions after addFirstProductToCart() or
    // addToCartFromDetailPage() to confirm the correct item was added.
    public static final String CART_EMPTY = "Product not displayed in cart — expected product row not found in cart table.";

    // =========================================================================
    // CHECKOUT ERRORS
    // =========================================================================

    // Displayed when the order confirmation heading is not visible after
    // confirmOrder() is called. Indicates payment was rejected, the confirmation
    // element locator has changed, or the checkout flow was interrupted.
    public static final String CHECKOUT_FAILED = "Order confirmation not displayed — 'Order Placed!' heading not visible after payment submission.";

    // =========================================================================
    // NAVIGATION ERRORS
    // =========================================================================

    // Displayed when driver.getTitle() does not match the expected page title
    // constant from Constants.java. Indicates the browser is on the wrong page
    // or the application changed the title of an existing page.
    public static final String PAGE_TITLE_MISMATCH = "Page title does not match expected — browser may be on the wrong page or the page title has changed.";
}
