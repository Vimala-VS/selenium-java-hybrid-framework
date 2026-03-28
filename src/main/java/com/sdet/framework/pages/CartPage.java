package com.sdet.framework.pages;

import com.sdet.framework.base.BasePage;
import com.sdet.framework.utils.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * CartPage — Page Object for https://www.automationexercise.com/view_cart
 *
 * CROSS-PAGE ASSERTIONS AND WHY THEY MATTER:
 * -------------------------------------------
 * A test that only asserts "the product was added" on the products page is
 * incomplete. The add-to-cart confirmation modal says the item was added, but
 * it does not prove the cart actually contains the right product with the right
 * price and quantity. The only authoritative source of truth is the cart page
 * itself.
 *
 * Cross-page assertions — asserting a value that was captured on one page
 * (e.g. the product name from ProductsPage.getProductName()) against the value
 * displayed on a different page (CartPage.getProductName()) — are what prove
 * the application correctly transferred data between pages. Without them, a bug
 * that adds the wrong item, the wrong quantity, or the wrong price would pass
 * every test that only checks the modal.
 *
 * HOW CART VERIFICATION WORKS ON AUTOMATIONEXERCISE.COM:
 * -------------------------------------------------------
 * The cart page renders each item as a <tr> row inside the #cart_info_table.
 * Each row contains:
 *   - Product image + name link  (td.cart_description)
 *   - Unit price                 (td.cart_price)
 *   - Quantity input             (td.cart_quantity)
 *   - Total price                (td.cart_total)
 *   - Delete button              (td.cart_delete)
 *
 * To verify the correct product is in the cart, the step definition:
 *   1. Captures the product name from ProductsPage before navigating away.
 *   2. Navigates to the cart.
 *   3. Calls isProductDisplayedInCart(expectedName) which searches the name
 *      column for a matching link text.
 *   4. Asserts price and quantity independently.
 *
 * getCartItemCount() counts <tr> rows to confirm the expected number of
 * distinct products are present — useful for multi-add scenarios.
 */
public class CartPage extends BasePage {

    private static final Logger log = LogManager.getLogger(CartPage.class);

    // =========================================================================
    // LOCATORS — private, static, final.
    // =========================================================================

    // Heading that confirms the cart page has loaded.
    private static final By CART_PAGE_TITLE = By.xpath("//li[@class='active' and text()='Shopping Cart']");

    // All product rows in the cart table — used for item count and row iteration.
    private static final By CART_ITEM_ROWS = By.cssSelector("tbody tr");

    // Product name link inside the first cart row's description cell.
    // Returns the visible link text (e.g. "Blue Top").
    private static final By PRODUCT_NAME = By.cssSelector("td.cart_description h4 a");

    // Unit price cell in the first cart row (e.g. "Rs. 500").
    private static final By PRODUCT_PRICE = By.cssSelector("td.cart_price p");

    // Quantity displayed in the first cart row's quantity cell.
    private static final By PRODUCT_QUANTITY = By.cssSelector("td.cart_quantity button");

    // Delete (×) button that removes a product row from the cart.
    // Targets the first delete button; see removeProduct() for details.
    private static final By DELETE_PRODUCT_BUTTON = By.cssSelector("td.cart_delete a.cart_quantity_delete");

    // "Proceed To Checkout" button at the bottom of the cart summary section.
    private static final By PROCEED_TO_CHECKOUT_BUTTON = By.cssSelector("a.btn.btn-default.check_out");

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public CartPage(WebDriver driver) {
        super(driver);
        log.debug("CartPage instantiated.");
    }

    // =========================================================================
    // ACTION METHODS
    // =========================================================================

    /**
     * isCartPageDisplayed() — returns true when the "Shopping Cart" breadcrumb
     * heading is visible, confirming the browser has landed on the cart page.
     *
     * Always call this as the first assertion in any cart scenario step to
     * catch navigation failures before attempting to read cart contents.
     */
    public boolean isCartPageDisplayed() {
        log.info("Checking if Cart page is displayed.");
        return isElementDisplayed(CART_PAGE_TITLE);
    }

    /**
     * getProductName() — returns the name of the first product in the cart.
     *
     * Used in cross-page assertions: capture the name on ProductsPage, navigate
     * to the cart, then assert that this method returns the same value.
     */
    public String getProductName() {
        log.info("Retrieving product name from cart.");
        String name = getElementText(PRODUCT_NAME);
        log.debug("Cart product name: '{}'", name);
        return name;
    }

    /**
     * getProductPrice() — returns the displayed unit price of the first product
     * in the cart (e.g. "Rs. 500").
     *
     * The raw string including the currency symbol is returned so the step
     * definition can assert the exact displayed value or strip the prefix
     * and compare numeric values.
     */
    public String getProductPrice() {
        log.info("Retrieving product price from cart.");
        String price = getElementText(PRODUCT_PRICE);
        log.debug("Cart product price: '{}'", price);
        return price;
    }

    /**
     * getProductQuantity() — returns the quantity of the first product in the
     * cart as a String (e.g. "1", "3").
     *
     * The step definition is responsible for parsing this to an integer if a
     * numeric comparison is needed. Returning String keeps the page object free
     * of parsing logic that belongs to the test layer.
     */
    public String getProductQuantity() {
        log.info("Retrieving product quantity from cart.");
        String quantity = getElementText(PRODUCT_QUANTITY);
        log.debug("Cart product quantity: '{}'", quantity);
        return quantity;
    }

    /**
     * removeProduct() — clicks the delete button on the first product row,
     * removing it from the cart.
     *
     * After calling this, use isProductDisplayedInCart() or getCartItemCount()
     * to confirm the row was removed. The cart table re-renders after deletion,
     * so any previously captured WebElement references to rows will be stale.
     */
    public void removeProduct() {
        log.info("Removing first product from cart.");
        clickElement(DELETE_PRODUCT_BUTTON);
        log.debug("Delete button clicked — product row should be removed.");
    }

    /**
     * isProductDisplayedInCart(productName) — searches every product name cell
     * in the cart for a case-insensitive match against the given name.
     *
     * WHY THIS MATTERS FOR CROSS-PAGE ASSERTIONS:
     * The name captured from ProductsPage.getProductName() is compared here to
     * prove the correct item arrived in the cart. A simple isElementDisplayed()
     * check would only confirm some product is present — not the right one.
     *
     * @param productName  the name to search for (case-insensitive partial match)
     * @return true if at least one cart row contains the given product name
     */
    public boolean isProductDisplayedInCart(String productName) {
        log.info("Checking if product '{}' is present in the cart.", productName);
        // Wait for at least one row to be present before reading all rows.
        waitForVisibility(PRODUCT_NAME);
        List<WebElement> nameElements = driver.findElements(PRODUCT_NAME);
        for (WebElement nameElement : nameElements) {
            if (nameElement.getText().trim().equalsIgnoreCase(productName)) {
                log.debug("Product '{}' found in cart.", productName);
                return true;
            }
        }
        log.warn("Product '{}' NOT found in cart. Total rows checked: {}", productName, nameElements.size());
        return false;
    }

    /**
     * proceedToCheckout() — clicks the "Proceed To Checkout" button at the
     * bottom of the cart page.
     *
     * If the user is not logged in, the site shows a login/guest modal before
     * navigating to the checkout address page. The step definition should handle
     * that conditional navigation separately.
     */
    public void proceedToCheckout() {
        log.info("Clicking 'Proceed To Checkout' button.");
        clickElement(PROCEED_TO_CHECKOUT_BUTTON);
        log.debug("Checkout initiated from cart page.");
    }

    /**
     * getCartItemCount() — returns the number of distinct product rows currently
     * displayed in the cart table.
     *
     * A count of 0 means the cart is empty. Use this in multi-add scenarios to
     * assert that the expected number of unique products were added before
     * proceeding to checkout.
     */
    public int getCartItemCount() {
        log.info("Counting the number of items in the cart.");
        List<WebElement> rows = driver.findElements(CART_ITEM_ROWS);
        int count = rows.size();
        log.debug("Cart item count: {}", count);
        return count;
    }
}
