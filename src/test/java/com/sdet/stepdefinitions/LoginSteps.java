package com.sdet.stepdefinitions;

import com.sdet.framework.factory.DriverFactory;
import com.sdet.framework.pages.HomePage;
import com.sdet.framework.pages.LoginPage;
import com.sdet.framework.utils.Constants;
import com.sdet.framework.utils.Errors;
import com.sdet.framework.utils.LogManager;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

/**
 * LoginSteps — step definitions that connect login.feature to LoginPage and HomePage.
 *
 * HOW STEP DEFINITION TEXT MUST MATCH THE FEATURE FILE:
 * ------------------------------------------------------
 * Cucumber reads each Given/When/Then/And line in the .feature file and searches
 * every class on the glue path for an annotation whose value is an exact regex
 * match of that text. If no match is found, Cucumber marks the step as
 * "Undefined" and the scenario fails before any code runs.
 *
 * Rules for keeping feature file and step definitions in sync:
 *
 *   1. COPY, DON'T RETYPE — copy the step text directly from the .feature file
 *      into the annotation value. A single character difference (trailing space,
 *      different capitalisation, "Login" vs "login") breaks the match silently.
 *
 *   2. PARAMETERS use {string} in the annotation — Cucumber replaces the
 *      quoted value in the feature file (e.g. "valid@email.com") with a
 *      String argument injected into the method. The quotes are consumed by
 *      Cucumber and must NOT appear in the annotation value.
 *      Feature file:    When the user enters "valid@email.com" as the email address
 *      Annotation:      @When("the user enters {string} as the email address")
 *
 *   3. Given/When/Then/And are interchangeable at the Java level — Cucumber
 *      treats them all the same. @Given, @When, @Then, and @And can all match
 *      any keyword in the feature file. The keyword is for readability only.
 *
 *   4. Background steps are shared — the step defined for the Background
 *      ("the user is on the Login page") is called before every scenario in the
 *      feature file. It must be defined here alongside the scenario steps.
 *
 * PAGE OBJECT INITIALISATION:
 * ----------------------------
 * Page objects are created in the constructor rather than in a @Before hook so
 * PicoContainer can instantiate this class and inject it into other step
 * definition classes if needed. The driver is already initialised by Hooks.java
 * @Before before Cucumber calls the constructor of any step definition class.
 */
public class LoginSteps {

    private static final Logger log = LogManager.getLogger(LoginSteps.class);

    private final LoginPage loginPage;
    private final HomePage  homePage;

    // PicoContainer calls this constructor automatically. By the time it does,
    // Hooks.java @Before has already called DriverFactory.initDriver() so
    // getDriver() is safe to call here.
    public LoginSteps() {
        loginPage = new LoginPage(DriverFactory.getDriver());
        homePage  = new HomePage(DriverFactory.getDriver());
        log.debug("LoginSteps instantiated — LoginPage and HomePage ready.");
    }

    // =========================================================================
    // BACKGROUND STEP
    // Runs before every scenario in login.feature.
    // =========================================================================

    /**
     * Matches: Given the user is on the Login page
     * Opens the login page URL directly. All three scenarios in login.feature
     * start here via the Background block.
     */
    @Given("the user is on the Login page")
    public void theUserIsOnTheLoginPage() {
        log.info("Step: the user is on the Login page");
        Hooks.getExtentTest().info("Navigating to the Login page.");
        loginPage.navigateToLoginPage();
    }

    // =========================================================================
    // SCENARIO: Successful login with valid credentials
    // =========================================================================

    /**
     * Matches: When the user enters a valid email address
     * Types the standard test account email from Constants.
     */
    @When("the user enters a valid email address")
    public void theUserEntersAValidEmailAddress() {
        log.info("Step: the user enters a valid email address");
        Hooks.getExtentTest().info("Entering valid email: " + Constants.VALID_EMAIL);
        loginPage.enterEmail(Constants.VALID_EMAIL);
    }

    /**
     * Matches: And the user enters a valid password
     * Types the standard test account password from Constants.
     */
    @And("the user enters a valid password")
    public void theUserEntersAValidPassword() {
        log.info("Step: the user enters a valid password");
        Hooks.getExtentTest().info("Entering valid password.");
        loginPage.enterPassword(Constants.VALID_PASSWORD);
    }

    /**
     * Matches: And the user clicks the Login button
     * Shared across all three scenarios — defined once, reused everywhere.
     */
    @And("the user clicks the Login button")
    public void theUserClicksTheLoginButton() {
        log.info("Step: the user clicks the Login button");
        Hooks.getExtentTest().info("Clicking the Login button.");
        loginPage.clickLoginButton();
    }

    /**
     * Matches: Then the Home page should be displayed
     * Asserts the site redirected to the home page after login.
     */
    @Then("the Home page should be displayed")
    public void theHomePageShouldBeDisplayed() {
        log.info("Step: the Home page should be displayed");
        Hooks.getExtentTest().info("Asserting that the Home page is displayed.");
        Assert.assertTrue(homePage.isHomePageDisplayed(), Errors.LOGIN_FAILED);
        Hooks.getExtentTest().info("Home page is displayed — assertion passed.");
        log.info("Home page displayed — assertion passed.");
    }

    /**
     * Matches: And the user should be logged in to their account
     * Asserts the "Logged in as" indicator is visible in the nav bar.
     */
    @And("the user should be logged in to their account")
    public void theUserShouldBeLoggedInToTheirAccount() {
        log.info("Step: the user should be logged in to their account");
        Hooks.getExtentTest().info("Asserting that the user session indicator is visible.");
        Assert.assertTrue(homePage.isUserLoggedIn(), Errors.USER_NOT_LOGGED_IN);
        Hooks.getExtentTest().info("User is logged in — assertion passed.");
        log.info("Logged-in indicator visible — assertion passed.");
    }

    // =========================================================================
    // SCENARIO: Failed login with invalid credentials
    // =========================================================================

    /**
     * Matches: When the user enters an unregistered email address
     * Types an email that has no matching account on the site.
     */
    @When("the user enters an unregistered email address")
    public void theUserEntersAnUnregisteredEmailAddress() {
        log.info("Step: the user enters an unregistered email address");
        Hooks.getExtentTest().info("Entering unregistered email: invalid@email.com");
        loginPage.enterEmail("invalid@email.com");
    }

    /**
     * Matches: And the user enters an incorrect password
     * Types a password that does not match any account.
     */
    @And("the user enters an incorrect password")
    public void theUserEntersAnIncorrectPassword() {
        log.info("Step: the user enters an incorrect password");
        Hooks.getExtentTest().info("Entering incorrect password.");
        loginPage.enterPassword("wrongpassword");
    }

    /**
     * Matches: Then an error message should appear on the Login page
     * Asserts the error paragraph is visible and contains text.
     */
    @Then("an error message should appear on the Login page")
    public void anErrorMessageShouldAppearOnTheLoginPage() {
        log.info("Step: an error message should appear on the Login page");
        Hooks.getExtentTest().info("Asserting that an error message is displayed.");
        String errorMessage = loginPage.getErrorMessage();
        Assert.assertFalse(errorMessage.isEmpty(), Errors.INVALID_LOGIN);
        Hooks.getExtentTest().info("Error message displayed: " + errorMessage);
        log.info("Error message visible: '{}' — assertion passed.", errorMessage);
    }

    /**
     * Matches: And the user should remain on the Login page
     * Asserts the browser did not navigate away — login was correctly rejected.
     */
    @And("the user should remain on the Login page")
    public void theUserShouldRemainOnTheLoginPage() {
        log.info("Step: the user should remain on the Login page");
        Hooks.getExtentTest().info("Asserting that the user is still on the Login page.");
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), Errors.INVALID_LOGIN);
        Hooks.getExtentTest().info("User is still on Login page — assertion passed.");
        log.info("Login page still displayed after rejected credentials — assertion passed.");
    }

    // =========================================================================
    // SCENARIO OUTLINE: Login with multiple user credentials
    // Steps are parameterised — Cucumber injects the Examples table values.
    // =========================================================================

    /**
     * Matches: When the user enters "<email>" as the email address
     * Annotation uses {string} — Cucumber strips the surrounding quotes from
     * the Examples table value and passes the raw string as the email parameter.
     */
    @When("the user enters {string} as the email address")
    public void theUserEntersAsTheEmailAddress(String email) {
        log.info("Step: the user enters '{}' as the email address", email);
        Hooks.getExtentTest().info("Entering email: " + email);
        loginPage.enterEmail(email);
    }

    /**
     * Matches: And the user enters "<password>" as the password
     * Same {string} parameter pattern as the email step above.
     */
    @And("the user enters {string} as the password")
    public void theUserEntersAsThePassword(String password) {
        log.info("Step: the user enters a password.");
        Hooks.getExtentTest().info("Entering password.");
        loginPage.enterPassword(password);
    }

    /**
     * Matches: Then the login result should be "<expectedResult>"
     * Branches on the value from the Examples table:
     *   "success" → asserts home page displayed and user is logged in.
     *   "failure" → asserts error message visible and user stays on login page.
     * Any other value fails with a clear unknown-result message.
     */
    @Then("the login result should be {string}")
    public void theLoginResultShouldBe(String expectedResult) {
        log.info("Step: the login result should be '{}'", expectedResult);
        Hooks.getExtentTest().info("Asserting login result: expected = " + expectedResult);

        if ("success".equalsIgnoreCase(expectedResult)) {
            Assert.assertTrue(homePage.isHomePageDisplayed(), Errors.LOGIN_FAILED);
            Assert.assertTrue(homePage.isUserLoggedIn(), Errors.USER_NOT_LOGGED_IN);
            Hooks.getExtentTest().info("Login succeeded as expected — home page displayed.");
            log.info("Login SUCCESS path verified.");

        } else if ("failure".equalsIgnoreCase(expectedResult)) {
            Assert.assertTrue(loginPage.isLoginPageDisplayed(), Errors.INVALID_LOGIN);
            String error = loginPage.getErrorMessage();
            Assert.assertFalse(error.isEmpty(), Errors.INVALID_LOGIN);
            Hooks.getExtentTest().info("Login rejected as expected — error: " + error);
            log.info("Login FAILURE path verified — error: '{}'", error);

        } else {
            throw new IllegalArgumentException(
                    "Unknown expectedResult value in Examples table: '" + expectedResult +
                    "'. Accepted values: 'success', 'failure'.");
        }
    }
}
