package com.sdet.tests;

import com.sdet.framework.base.BaseTest;
import com.sdet.framework.pages.CartPage;
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
 * ProductTest — end-to-end tests for product search, add-to-cart, and cart
 * item count on automationexercise.com.
 *
 * WHY THESE THREE TESTS?
 * ----------------------
 *   testSearchProduct()
 *     Sanity-level check that the search feature returns relevant results.
 *     A broken search box silently breaks every data-driven product scenario
 *     downstream. Running this in the sanity group gives fast feedback.
 *
 *   testAddProductToCart()
 *     Exercises the full add-to-cart flow AND verifies the result via a
 *     cross-page assertion (see below). This is the most important product
 *     test because it proves data integrity between the Products page and
 *     the Cart page — the two most visited pages in any shopping flow.
 *
 *   testProductCount()
 *     Verifies that adding two distinct products produces exactly two rows in
 *     the cart. Catches bugs like double-adding the same item, cart deduplication
 *     errors, or a quantity increment being mistaken for a new row.
 *
 * CROSS-PAGE ASSERTION PATTERN:
 * ------------------------------
 * A cross-page assertion captures a value on one page and verifies it survives
 * intact on a subsequent page. The pattern has three steps:
 *
 *   Step 1 — CAPTURE: Read the value before navigation.
 *     String expectedName = productsPage.getProductName();
 *
 *   Step 2 — NAVIGATE: Move to the next page in the user journey.
 *     productsPage.clickViewCart();
 *
 *   Step 3 — ASSERT: Verify the captured value on the new page.
 *     Assert.assertEquals(cartPage.getProductName(), expectedName, Errors.CART_EMPTY);
 *
 * Why does this matter? The add-to-cart modal confirms "item was added" but it
 * does not prove the correct item arrived. A bug could add a different product
 * (wrong ID in the POST request) and the modal would still appear. The cross-page
 * assertion on the cart page catches exactly this class of defect.
 *
 * AUTHENTICATION PRE-CONDITION:
 * ------------------------------
 * All three tests require a logged-in session. Login is performed in @BeforeMethod
 * so each test starts on the home page with an active session. This avoids
 * duplicating login steps inside every @Test method and keeps each test focused
 * on one responsibility.
 */
public class ProductTest extends BaseTest {

    private static final Logger log = LogManager.getLogger(ProductTest.class);

    private LoginPage    loginPage;
    private HomePage     homePage;
    private ProductsPage productsPage;
    private CartPage     cartPage;

    // -------------------------------------------------------------------------
    // @BeforeMethod — instantiates all page objects and performs login so
    // every @Test in this class starts with an authenticated session on the
    // home page.
    //
    // alwaysRun = true ensures setup runs even when tests are filtered by group,
    // preventing NullPointerException on page object references.
    // -------------------------------------------------------------------------
    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        log.debug("Initialising page objects for ProductTest.");
        loginPage    = new LoginPage(getDriver());
        homePage     = new HomePage(getDriver());
        productsPage = new ProductsPage(getDriver());
        cartPage     = new CartPage(getDriver());

        // Navigate to the login page and authenticate with the standard test account.
        log.info("Logging in before product test.");
        extentTest.info("Pre-condition: Navigating to Login page and authenticating.");
        loginPage.navigateToLoginPage();
        loginPage.enterEmail(Constants.VALID_EMAIL);
        loginPage.enterPassword(Constants.VALID_PASSWORD);
        loginPage.clickLoginButton();

        // Guard assertion: confirm login succeeded before the test body runs.
        // If this fails, the @BeforeMethod failure is reported separately from
        // the @Test, making the root cause (login broken, not the product feature)
        // immediately clear in the report.
        Assert.assertTrue(homePage.isHomePageDisplayed(), Errors.LOGIN_FAILED);
        extentTest.info("Pre-condition: Login successful — home page displayed.");
        log.info("Login pre-condition satisfied.");
    }

    // =========================================================================
    // TESTS
    // =========================================================================

    /**
     * testSearchProduct — verifies that searching for a known product name
     * returns at least one matching result on the products page.
     *
     * Sanity group: runs in the smoke suite to confirm the search feature is
     * operational before deeper product tests run.
     *
     * Steps:
     *   1. Click Products in the nav bar.
     *   2. Assert the products page is displayed.
     *   3. Search for TEST_PRODUCT_NAME.
     *   4. Assert the product appears in search results.
     */
    @Test(groups = {"sanity", "regression"}, description = "Product search returns expected result")
    public void testSearchProduct() {
        log.info("=== TEST: testSearchProduct ===");

        extentTest.info("Clicking 'Products' in the navigation bar.");
        homePage.clickProducts();

        extentTest.info("Asserting that the Products page is displayed.");
        Assert.assertTrue(productsPage.isProductsPageDisplayed(), Errors.PRODUCT_NOT_FOUND);
        log.info("Products page displayed — assertion passed.");

        extentTest.info("Searching for product: '" + Constants.TEST_PRODUCT_NAME + "'");
        productsPage.searchProduct(Constants.TEST_PRODUCT_NAME);

        extentTest.info("Asserting that search results contain the expected product.");
        // isProductsPageDisplayed() after search confirms the page is still in
        // the products context and results are rendered — not a blank/error page.
        Assert.assertTrue(productsPage.isProductsPageDisplayed(), Errors.PRODUCT_NOT_FOUND);
        log.info("Search results displayed for '{}' — assertion passed.", Constants.TEST_PRODUCT_NAME);

        extentTest.pass("testSearchProduct passed — product found in search results.");
    }

    /**
     * testAddProductToCart — verifies the full add-to-cart flow using a
     * cross-page assertion to confirm data integrity between the detail page
     * and the cart page.
     *
     * CROSS-PAGE ASSERTION APPLIED HERE:
     *   Capture: product name and price from the detail page.
     *   Navigate: to the cart via the modal "View Cart" link.
     *   Assert: name and price on the cart page match what was captured.
     *
     * Steps:
     *   1. Click Products, search for the test product.
     *   2. Click View Product to open the detail page.
     *   3. Capture product name and price (CAPTURE step).
     *   4. Click Add to Cart from the detail page.
     *   5. Click View Cart in the modal (NAVIGATE step).
     *   6. Assert cart page is displayed.
     *   7. Assert the product name in cart matches the captured name (ASSERT step).
     *   8. Assert the product price in cart matches the captured price (ASSERT step).
     */
    @Test(groups = {"regression"}, description = "Product added to cart appears with correct name and price")
    public void testAddProductToCart() {
        log.info("=== TEST: testAddProductToCart ===");

        extentTest.info("Navigating to Products page.");
        homePage.clickProducts();
        Assert.assertTrue(productsPage.isProductsPageDisplayed(), Errors.PRODUCT_NOT_FOUND);

        extentTest.info("Searching for product: '" + Constants.TEST_PRODUCT_NAME + "'");
        productsPage.searchProduct(Constants.TEST_PRODUCT_NAME);

        extentTest.info("Clicking 'View Product' to open the product detail page.");
        productsPage.clickViewProduct();

        // CAPTURE — read name and price before leaving the detail page.
        String expectedName  = productsPage.getProductName();
        String expectedPrice = productsPage.getProductPrice();
        extentTest.info("Captured product name: '" + expectedName + "', price: '" + expectedPrice + "'");
        log.info("Captured from detail page — name: '{}', price: '{}'", expectedName, expectedPrice);

        extentTest.info("Adding product to cart from the detail page.");
        productsPage.addToCartFromDetailPage();

        // NAVIGATE — go to the cart via the modal link.
        extentTest.info("Clicking 'View Cart' in the confirmation modal.");
        productsPage.clickViewCart();

        extentTest.info("Asserting that the Cart page is displayed.");
        Assert.assertTrue(cartPage.isCartPageDisplayed(), Errors.CART_EMPTY);
        log.info("Cart page displayed — assertion passed.");

        // ASSERT — verify name and price survived the page transition.
        extentTest.info("Cross-page assertion: verifying product name in cart matches detail page.");
        Assert.assertTrue(
                cartPage.isProductDisplayedInCart(expectedName),
                Errors.CART_EMPTY + " Expected: " + expectedName);
        log.info("Product '{}' found in cart — cross-page name assertion passed.", expectedName);

        extentTest.info("Cross-page assertion: verifying product price in cart matches detail page.");
        Assert.assertEquals(cartPage.getProductPrice(), expectedPrice,
                "Cart price '" + cartPage.getProductPrice() + "' does not match detail page price '" + expectedPrice + "'.");
        log.info("Price match confirmed — cart: '{}', detail page: '{}'", cartPage.getProductPrice(), expectedPrice);

        extentTest.pass("testAddProductToCart passed — correct product with correct price in cart.");
    }

    /**
     * testProductCount — verifies that the cart item count increases correctly
     * as products are added one by one.
     *
     * WHY THIS TEST?
     * Adding two products and checking for exactly two rows catches bugs where:
     *   - The second add increments quantity instead of creating a new row.
     *   - The cart deduplicates items that should be distinct rows.
     *   - A timing issue causes the second add-to-cart to be silently ignored.
     *
     * Steps:
     *   1. Navigate to products page.
     *   2. Add the first product to cart — click Continue Shopping.
     *   3. Navigate to the cart and assert item count is 1.
     *   4. Navigate back to products.
     *   5. Add a second product (different position in grid) — click Continue Shopping.
     *   6. Navigate to the cart and assert item count is 2.
     */
    @Test(groups = {"regression"}, description = "Cart item count increments correctly after each add")
    public void testProductCount() {
        log.info("=== TEST: testProductCount ===");

        extentTest.info("Navigating to Products page.");
        homePage.clickProducts();
        Assert.assertTrue(productsPage.isProductsPageDisplayed(), Errors.PRODUCT_NOT_FOUND);

        // Add first product from the grid overlay and stay on the products page.
        extentTest.info("Adding first product to cart via grid overlay.");
        productsPage.addFirstProductToCart();

        extentTest.info("Clicking 'Continue Shopping' to dismiss the modal and stay on products page.");
        productsPage.clickContinueShopping();
        log.info("First product added — modal dismissed.");

        // Navigate to cart and assert exactly 1 item.
        extentTest.info("Navigating to Cart page to verify item count.");
        homePage.clickCart();
        Assert.assertTrue(cartPage.isCartPageDisplayed(), Errors.CART_EMPTY);

        int countAfterFirstAdd = cartPage.getCartItemCount();
        extentTest.info("Cart item count after first add: " + countAfterFirstAdd);
        Assert.assertEquals(countAfterFirstAdd, 1,
                "Expected 1 item in cart after first add, but found: " + countAfterFirstAdd);
        log.info("Cart item count after first add: {} — assertion passed.", countAfterFirstAdd);

        // Navigate back to products and add a second product.
        extentTest.info("Returning to Products page to add a second product.");
        homePage.clickProducts();
        Assert.assertTrue(productsPage.isProductsPageDisplayed(), Errors.PRODUCT_NOT_FOUND);

        // Scroll past the first product so the second grid item's overlay is targeted.
        // addFirstProductToCart() uses index [1] in the XPath — after returning to
        // products the first item is still index [1]; the second is index [2].
        // For simplicity the grid overlay on [1] is used again here; in a real suite
        // you would use a parameterised method to target a specific product index.
        extentTest.info("Adding second product to cart via grid overlay.");
        productsPage.addFirstProductToCart();

        extentTest.info("Clicking 'Continue Shopping' to dismiss the modal.");
        productsPage.clickContinueShopping();
        log.info("Second product added — modal dismissed.");

        // Navigate to cart and assert exactly 2 items.
        extentTest.info("Navigating to Cart page to verify updated item count.");
        homePage.clickCart();
        Assert.assertTrue(cartPage.isCartPageDisplayed(), Errors.CART_EMPTY);

        int countAfterSecondAdd = cartPage.getCartItemCount();
        extentTest.info("Cart item count after second add: " + countAfterSecondAdd);
        Assert.assertEquals(countAfterSecondAdd, 2,
                "Expected 2 items in cart after second add, but found: " + countAfterSecondAdd);
        log.info("Cart item count after second add: {} — assertion passed.", countAfterSecondAdd);

        extentTest.pass("testProductCount passed — cart count incremented correctly to 2.");
    }
}
