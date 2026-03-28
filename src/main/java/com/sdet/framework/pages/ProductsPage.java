package com.sdet.framework.pages;

import com.sdet.framework.base.BasePage;
import com.sdet.framework.utils.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * ProductsPage — Page Object for https://www.automationexercise.com/products
 *
 * SEARCH AND ADD-TO-CART FLOW ON AUTOMATIONEXERCISE.COM:
 * -------------------------------------------------------
 * The products page has two distinct sub-flows that this class covers:
 *
 * Flow 1 — Search then View:
 *   1. User types a keyword into the search box and clicks the Search button.
 *   2. The page filters the product grid in place (no full page reload).
 *   3. Each matching product card shows a "View Product" link.
 *   4. Clicking "View Product" navigates to the individual product detail page
 *      where the full description, price, and quantity selector are available.
 *
 * Flow 2 — Add to Cart from the product grid (without entering detail page):
 *   1. Hovering over a product card reveals an "Add to cart" overlay button.
 *   2. Clicking it triggers a modal dialog with two choices:
 *        - "Continue Shopping" — closes the modal, stays on the products page.
 *        - "View Cart"         — closes the modal and navigates to the cart.
 *   3. The modal must be dismissed before any further interaction is possible.
 *
 * This class handles both flows. The locators for "Add to cart", "Continue
 * Shopping", and "View Cart" all target the modal and overlay elements that
 * are only visible during Flow 2.
 *
 * LOCATOR STRATEGY NOTE:
 * ----------------------
 * automationexercise.com uses data-qa attributes on form elements but not on
 * product grid items. Product cards are identified by their structural CSS
 * selectors and class names. These are stable across the site's current build
 * but should be re-verified if the site undergoes a major redesign.
 */
public class ProductsPage extends BasePage {

    private static final Logger log = LogManager.getLogger(ProductsPage.class);

    // =========================================================================
    // LOCATORS — private, static, final.
    // =========================================================================

    // --- Page identity -------------------------------------------------------

    // Heading that confirms the products catalogue page has loaded.
    private static final By PRODUCTS_HEADING = By.xpath("//h2[text()='All Products']");

    // --- Search section -------------------------------------------------------

    // Search keyword input field below the products heading.
    private static final By SEARCH_INPUT = By.cssSelector("input#search_product");

    // Magnifying glass / Submit button that triggers the product search.
    private static final By SEARCH_BUTTON = By.cssSelector("button#submit_search");

    // Container that holds all product cards after a search or on initial load.
    // Used to confirm search results are rendered before interacting with cards.
    private static final By PRODUCT_LIST = By.cssSelector("div.features_items div.product-image-wrapper");

    // --- Product card (grid view) --------------------------------------------

    // "View Product" link on the first product card in the grid.
    // Navigates to the individual product detail page.
    private static final By FIRST_VIEW_PRODUCT_LINK = By.xpath("(//a[contains(@href,'/product_details/')])[1]");

    // "Add to cart" overlay button that appears on hover over the first product card.
    // The data-product-id attribute exists on all add-to-cart buttons;
    // the [1] index targets the first one in the current list.
    private static final By FIRST_ADD_TO_CART_BUTTON = By.xpath("(//a[@class='btn btn-default add-to-cart'])[1]");

    // --- Product detail page (after clicking View Product) -------------------

    // Product name heading on the detail page.
    private static final By PRODUCT_NAME = By.cssSelector("div.product-information h2");

    // Product price shown on the detail page (e.g. "Rs. 500").
    private static final By PRODUCT_PRICE = By.cssSelector("div.product-information span span");

    // "Add to Cart" button on the product detail page (different from the grid overlay).
    private static final By ADD_TO_CART_BUTTON = By.cssSelector("button.cart");

    // --- Modal dialog (appears after adding a product to cart) ---------------

    // "Continue Shopping" button inside the confirmation modal.
    // Closes the modal and keeps the user on the current page.
    private static final By CONTINUE_SHOPPING_BUTTON = By.cssSelector("button[data-dismiss='modal']");

    // "View Cart" link inside the confirmation modal.
    // Closes the modal and navigates to the cart page.
    private static final By VIEW_CART_LINK = By.xpath("//u[text()='View Cart']");

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public ProductsPage(WebDriver driver) {
        super(driver);
        log.debug("ProductsPage instantiated.");
    }

    // =========================================================================
    // ACTION METHODS
    // =========================================================================

    /**
     * isProductsPageDisplayed() — returns true when the "All Products" heading
     * is visible, confirming the browser is on the products catalogue page.
     *
     * Called as a guard assertion before any search or add-to-cart action to
     * ensure the page has finished loading and the product grid is present.
     */
    public boolean isProductsPageDisplayed() {
        log.info("Checking if Products page is displayed.");
        return isElementDisplayed(PRODUCTS_HEADING);
    }

    /**
     * searchProduct(productName) — types a keyword into the search box and
     * submits the search.
     *
     * FLOW: enterText clears any previous keyword → types the new one →
     * clickElement submits → the grid filters in place → product cards matching
     * the keyword remain visible; non-matching cards are hidden.
     *
     * After this method returns, callers should assert that at least one product
     * card is displayed before interacting with a result.
     */
    public void searchProduct(String productName) {
        log.info("Searching for product: '{}'", productName);
        enterText(SEARCH_INPUT, productName);
        clickElement(SEARCH_BUTTON);
        // Wait for the product grid to re-render after the search filter is applied
        // before the caller attempts to interact with any result cards.
        waitForVisibility(PRODUCT_LIST);
        log.debug("Search submitted and results loaded for: '{}'", productName);
    }

    /**
     * clickViewProduct() — clicks the "View Product" link on the first product
     * card in the current grid (filtered or unfiltered).
     *
     * Navigates to the product detail page where name, price, description,
     * and the add-to-cart button with quantity control are available.
     */
    public void clickViewProduct() {
        log.info("Clicking 'View Product' on the first product card.");
        clickElement(FIRST_VIEW_PRODUCT_LINK);
    }

    /**
     * addFirstProductToCart() — triggers the "Add to cart" overlay on the first
     * product card in the grid.
     *
     * IMPORTANT: This action opens a modal dialog. The test must call either
     * clickContinueShopping() or clickViewCart() immediately after to dismiss
     * the modal — failure to do so leaves the modal open, causing all
     * subsequent findElement calls to fail with ElementNotInteractableException.
     */
    public void addFirstProductToCart() {
        log.info("Adding first product to cart via grid overlay button.");
        clickElement(FIRST_ADD_TO_CART_BUTTON);
        log.debug("Product added to cart — modal should now be visible.");
    }

    /**
     * clickContinueShopping() — dismisses the add-to-cart confirmation modal
     * and keeps the browser on the products page.
     *
     * Use this when the scenario needs to add multiple products before
     * proceeding to the cart.
     */
    public void clickContinueShopping() {
        log.info("Clicking 'Continue Shopping' to dismiss the cart modal.");
        // Wait for the modal button to be clickable before interacting —
        // the modal has a CSS fade-in animation and the button is not
        // immediately interactable when the modal first appears.
        waitForClickable(CONTINUE_SHOPPING_BUTTON).click();
        log.debug("Modal dismissed — continuing on Products page.");
    }

    /**
     * clickViewCart() — dismisses the add-to-cart confirmation modal and
     * navigates to the cart page.
     *
     * Use this when the scenario is ready to proceed directly to checkout
     * after adding a product.
     */
    public void clickViewCart() {
        log.info("Clicking 'View Cart' link in the cart modal.");
        waitForVisibility(VIEW_CART_LINK);
        clickElement(VIEW_CART_LINK);
        log.debug("Navigating to Cart page via modal link.");
    }

    /**
     * addToCartFromDetailPage() — clicks the "Add to Cart" button on the product
     * detail page (reached via clickViewProduct()).
     *
     * This is distinct from addFirstProductToCart() which uses the hover overlay
     * on the grid. Both actions open the same confirmation modal, so the caller
     * must still call clickContinueShopping() or clickViewCart() after this.
     */
    public void addToCartFromDetailPage() {
        log.info("Adding product to cart from the product detail page.");
        clickElement(ADD_TO_CART_BUTTON);
        log.debug("Add to cart clicked on detail page — modal should now be visible.");
    }

    /**
     * getProductName() — retrieves the product name heading from the product
     * detail page.
     *
     * Called after clickViewProduct() to capture the name for assertion.
     * The caller should store this value before any further navigation since
     * going back to the grid will lose the detail page context.
     */
    public String getProductName() {
        log.info("Retrieving product name from detail page.");
        String name = getElementText(PRODUCT_NAME);
        log.debug("Product name: '{}'", name);
        return name;
    }

    /**
     * getProductPrice() — retrieves the product price string from the product
     * detail page (e.g. "Rs. 500").
     *
     * Returns the raw text including currency symbol so step definitions can
     * assert the exact displayed value or parse it as needed.
     */
    public String getProductPrice() {
        log.info("Retrieving product price from detail page.");
        String price = getElementText(PRODUCT_PRICE);
        log.debug("Product price: '{}'", price);
        return price;
    }
}
