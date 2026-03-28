package com.sdet.framework.pages;

import com.sdet.framework.base.BasePage;
import com.sdet.framework.utils.ConfigReader;
import com.sdet.framework.utils.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * HomePage — Page Object for https://www.automationexercise.com
 *
 * The home page is the entry point for most test scenarios. After a successful
 * login, the site redirects here. After a logout, the site also returns here.
 * This makes HomePage the most frequently used page for pre- and post-condition
 * verification across the entire suite.
 *
 * WHY isHomePageDisplayed() MATTERS FOR TEST ASSERTIONS:
 * -------------------------------------------------------
 * A test that navigates to the site and immediately interacts with elements
 * without first confirming it is on the right page will produce misleading
 * failures. If the browser lands on a 404, a CAPTCHA, or a redirect, the
 * next findElement call fails with NoSuchElementException — and the error
 * message says nothing about the real cause (wrong page).
 *
 * isHomePageDisplayed() acts as a guard assertion at the start of any scenario
 * that requires the home page as a precondition. If it returns false, the test
 * fails with a clear, intentional message ("Expected to be on the Home page")
 * rather than a cryptic element-not-found error three steps later.
 *
 * It is also used as the post-condition check after login and logout actions
 * to confirm the application behaved as expected before the scenario continues.
 */
public class HomePage extends BasePage {

    private static final Logger log = LogManager.getLogger(HomePage.class);

    // =========================================================================
    // LOCATORS — private, static, final.
    // All locators are scoped to this class only. Callers interact through
    // the public action methods, never through raw By references.
    // =========================================================================

    // Site logo in the top-left — its visibility confirms the page fully loaded.
    private static final By LOGO = By.cssSelector("img[src='/static/images/home/logo.png']");

    // "Login / Signup" link in the top navigation bar.
    private static final By NAV_LOGIN_SIGNUP = By.cssSelector("a[href='/login']");

    // "Cart" link in the top navigation bar.
    private static final By NAV_CART = By.cssSelector("a[href='/view_cart']");

    // "Products" link in the top navigation bar.
    private static final By NAV_PRODUCTS = By.cssSelector("a[href='/products']");

    // Logged-in username displayed in the nav bar as "Logged in as <name>".
    // The <b> tag wraps the actual username text within the anchor element.
    private static final By LOGGED_IN_USERNAME = By.cssSelector("a[href='/delete_account'] b");

    // Logout link — only visible when a user is logged in.
    private static final By NAV_LOGOUT = By.cssSelector("a[href='/logout']");

    // "Logged in as" anchor used to detect whether a user session is active.
    // Checking for this element is more reliable than checking the username text
    // because it is present even before the username text has fully rendered.
    private static final By LOGGED_IN_INDICATOR = By.xpath("//a[contains(.,'Logged in as')]");

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public HomePage(WebDriver driver) {
        super(driver);
        log.debug("HomePage instantiated.");
    }

    // =========================================================================
    // ACTION METHODS
    // =========================================================================

    /**
     * isHomePageDisplayed() — returns true when the site logo is visible.
     *
     * WHY the logo and not the page title?
     * The <title> tag is set even on error pages. The logo element only exists
     * when the home page has loaded correctly, making it a more trustworthy
     * indicator. This method is used as both a navigation guard (confirm we
     * are on the home page before acting) and a post-condition check (confirm
     * we were returned here after login / logout).
     */
    public boolean isHomePageDisplayed() {
        log.info("Checking if Home page is displayed (logo visibility).");
        return isElementDisplayed(LOGO);
    }

    /**
     * clickLoginSignup() — clicks the "Login / Signup" link in the nav bar.
     * Called at the start of any scenario that needs to reach the login page
     * from the home page rather than navigating directly via URL.
     */
    public void clickLoginSignup() {
        log.info("Clicking 'Login / Signup' navigation link.");
        clickElement(NAV_LOGIN_SIGNUP);
    }

    /**
     * clickProducts() — navigates to the Products catalogue page.
     * Used in shopping and search scenarios that begin from the home page.
     */
    public void clickProducts() {
        log.info("Clicking 'Products' navigation link.");
        clickElement(NAV_PRODUCTS);
    }

    /**
     * clickCart() — navigates to the shopping cart page.
     * Used in checkout scenarios to proceed from home to cart.
     */
    public void clickCart() {
        log.info("Clicking 'Cart' navigation link.");
        clickElement(NAV_CART);
    }

    /**
     * isUserLoggedIn() — returns true when the "Logged in as" nav element is
     * present and visible, confirming an active user session.
     *
     * Use this immediately after a login action to assert the login succeeded
     * before the scenario continues with authenticated actions (e.g. checkout).
     */
    public boolean isUserLoggedIn() {
        log.info("Checking if a user is currently logged in.");
        boolean loggedIn = isElementDisplayed(LOGGED_IN_INDICATOR);
        log.debug("User logged in: {}", loggedIn);
        return loggedIn;
    }

    /**
     * getLoggedInUsername() — reads the username text from the nav bar.
     * Returns the name displayed after "Logged in as", e.g. "John Doe".
     * Used by step definitions to assert the correct account is logged in.
     */
    public String getLoggedInUsername() {
        log.info("Retrieving logged-in username from navigation bar.");
        String username = getElementText(LOGGED_IN_USERNAME);
        log.debug("Logged-in username: '{}'", username);
        return username;
    }

    /**
     * clickLogout() — clicks the Logout link, ending the current session.
     * After this the site redirects to the login page. Step definitions
     * should assert isHomePageDisplayed() or LoginPage.isLoginPageDisplayed()
     * after calling this method to confirm the logout completed.
     */
    public void clickLogout() {
        log.info("Clicking Logout link.");
        clickElement(NAV_LOGOUT);
    }

    /**
     * navigateToHomePage() — opens the base URL directly.
     * Used in @Before hooks or Background steps to land on the home page
     * without relying on the nav bar (useful when session state is unknown).
     */
    public void navigateToHomePage() {
        String url = ConfigReader.getInstance().getUrl();
        log.info("Navigating directly to Home page: {}", url);
        navigateTo(url);
    }
}
