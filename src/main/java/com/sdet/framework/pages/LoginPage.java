package com.sdet.framework.pages;

import com.sdet.framework.base.BasePage;
import com.sdet.framework.utils.ConfigReader;
import com.sdet.framework.utils.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * LoginPage — Page Object for https://www.automationexercise.com/login
 *
 * PAGE OBJECT MODEL (POM) PATTERN:
 * ----------------------------------
 * POM separates two concerns that should never be mixed:
 *
 *   1. HOW to interact with a page  →  lives here (locators + action methods)
 *   2. WHAT to verify in a scenario →  lives in step definitions / test classes
 *
 * Without POM, both concerns are scattered across test files. When the UI
 * changes (e.g. the login button gets a new ID), you hunt through dozens of
 * test files to fix the same broken locator. With POM, you fix it in one place
 * — this file — and every scenario that uses LoginPage is instantly repaired.
 *
 * WHY LOCATORS ARE PRIVATE:
 * --------------------------
 * Locators are declared private so they can ONLY be used through the public
 * action methods of this class. If a step definition could access a By directly,
 * it would bypass the wait logic in BasePage and break the encapsulation POM
 * relies on. Private locators enforce the rule: "tell the page what to do,
 * don't ask it for its internals."
 *
 * WHY data-qa ATTRIBUTES:
 * ------------------------
 * automationexercise.com ships elements with dedicated data-qa attributes
 * (e.g. data-qa="login-email"). These are the most stable locator strategy
 * because they are added specifically for automation and are not affected by
 * CSS refactoring, class renames, or layout changes.
 */
public class LoginPage extends BasePage {

    private static final Logger log = LogManager.getLogger(LoginPage.class);

    // Login page URL path — appended to the base URL from config.properties.
    private static final String LOGIN_PATH = "/login";

    // =========================================================================
    // LOCATORS — private, static, final.
    //
    // static  : locators are class-level constants, not per-instance fields.
    //           They never change so there is no reason to re-create them.
    // final   : prevents accidental reassignment elsewhere in this class.
    // private : enforces access only through this class's action methods.
    // =========================================================================

    // --- Login section -------------------------------------------------------

    // Email input on the login form (left-hand side of the split page).
    private static final By EMAIL_INPUT       = By.cssSelector("[data-qa='login-email']");

    // Password input on the login form.
    private static final By PASSWORD_INPUT    = By.cssSelector("[data-qa='login-password']");

    // "Login" submit button below the login form.
    private static final By LOGIN_BUTTON      = By.cssSelector("[data-qa='login-button']");

    // Error paragraph shown when credentials are invalid:
    // "Your email or password is incorrect!"
    private static final By ERROR_MESSAGE     = By.xpath("//p[contains(@style,'color: red')]");

    // --- Signup section ------------------------------------------------------

    // Name input on the new-user signup form (right-hand side of the split page).
    private static final By SIGNUP_NAME_INPUT  = By.cssSelector("[data-qa='signup-name']");

    // Email input on the signup form (separate from the login email field).
    private static final By SIGNUP_EMAIL_INPUT = By.cssSelector("[data-qa='signup-email']");

    // "Signup" submit button below the signup form.
    private static final By SIGNUP_BUTTON      = By.cssSelector("[data-qa='signup-button']");

    // Heading used to confirm the login page is displayed.
    // Both "Login to your account" and "New User Signup!" headings live here.
    private static final By LOGIN_HEADING      = By.xpath("//h2[text()='Login to your account']");

    // =========================================================================
    // CONSTRUCTOR
    // Passes the WebDriver up to BasePage which initialises the WebDriverWait.
    // No other setup is needed — all locators are static constants.
    // =========================================================================
    public LoginPage(WebDriver driver) {
        super(driver);
        log.debug("LoginPage instantiated.");
    }

    // =========================================================================
    // ACTION METHODS — public, one responsibility each.
    //
    // Rules:
    //   - Never call driver.findElement() directly. Always use BasePage methods.
    //   - Never assert or verify here. Assertions belong in step definitions.
    //   - Every method logs what it is doing at INFO level.
    // =========================================================================

    /**
     * navigateToLoginPage() — builds the full login URL from config and opens it.
     * Combines the environment base URL with the /login path so the method works
     * across QA and Staging without hardcoding either URL.
     */
    public void navigateToLoginPage() {
        String url = ConfigReader.getInstance().getUrl() + LOGIN_PATH;
        log.info("Navigating to Login page: {}", url);
        navigateTo(url);
    }

    /**
     * enterEmail(email) — types the user's email into the login email field.
     * BasePage.enterText() waits for the field to be interactive then clears
     * it before typing to prevent leftover values from a previous attempt.
     */
    public void enterEmail(String email) {
        log.info("Entering login email: {}", email);
        enterText(EMAIL_INPUT, email);
    }

    /**
     * enterPassword(password) — types the user's password into the password field.
     */
    public void enterPassword(String password) {
        log.info("Entering login password.");
        enterText(PASSWORD_INPUT, password);
    }

    /**
     * clickLoginButton() — waits for the Login button to be clickable then clicks it.
     * After this call the page will either redirect (success) or show an error message.
     */
    public void clickLoginButton() {
        log.info("Clicking the Login button.");
        clickElement(LOGIN_BUTTON);
    }

    /**
     * getErrorMessage() — retrieves the visible error text shown when login fails.
     * Used by step definitions to assert the correct error wording is displayed.
     */
    public String getErrorMessage() {
        log.info("Reading login error message.");
        return getElementText(ERROR_MESSAGE);
    }

    /**
     * isLoginPageDisplayed() — returns true when the "Login to your account"
     * heading is visible, confirming the browser is on the login page.
     * Used in step definitions to assert correct navigation.
     */
    public boolean isLoginPageDisplayed() {
        log.info("Checking if Login page is displayed.");
        return isElementDisplayed(LOGIN_HEADING);
    }

    /**
     * enterSignupName(name) — types the new user's name into the signup name field
     * on the right-hand side of the login page.
     */
    public void enterSignupName(String name) {
        log.info("Entering signup name: {}", name);
        enterText(SIGNUP_NAME_INPUT, name);
    }

    /**
     * enterSignupEmail(email) — types the new user's email into the signup email field.
     * This field is separate from the login email field even though they appear
     * on the same page.
     */
    public void enterSignupEmail(String email) {
        log.info("Entering signup email: {}", email);
        enterText(SIGNUP_EMAIL_INPUT, email);
    }

    /**
     * clickSignupButton() — submits the signup form.
     * On success the browser navigates to the account registration details page.
     */
    public void clickSignupButton() {
        log.info("Clicking the Signup button.");
        clickElement(SIGNUP_BUTTON);
    }
}
