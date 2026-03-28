package com.sdet.tests;

import com.sdet.framework.base.BaseTest;
import com.sdet.framework.pages.HomePage;
import com.sdet.framework.pages.LoginPage;
import com.sdet.framework.utils.ConfigReader;
import com.sdet.framework.utils.Constants;
import com.sdet.framework.utils.Errors;
import com.sdet.framework.utils.ExcelUtil;
import com.sdet.framework.utils.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * LoginTest — end-to-end tests for the Login and Signup page.
 *
 * WHY THESE THREE TESTS?
 * ----------------------
 * Login is the gateway to the entire application. If it breaks, every other
 * authenticated scenario breaks with it. These tests cover three distinct
 * risk areas:
 *
 *   testValidLogin()
 *     The happy path. Proves that a registered user with correct credentials
 *     can reach the home page and the session is established. Tagged "sanity"
 *     so it runs as part of the quick smoke check before a full regression.
 *
 *   testInvalidLogin()
 *     The negative path. Proves that the application rejects bad credentials
 *     AND displays an error message. Without this test, a regression that
 *     accidentally accepts any credentials would pass the happy-path test.
 *
 *   testLoginWithMultipleUsers()
 *     The data-driven path. Reads multiple credential rows from Excel and runs
 *     the login action for each, asserting success or failure based on the
 *     expectedResult column. This catches boundary conditions (e.g. empty
 *     password, email with unusual characters) without duplicating test code.
 *
 * TEST LIFECYCLE (from BaseTest):
 * --------------------------------
 *   @BeforeSuite  → ExtentReports initialised
 *   @BeforeMethod → browser launched, ExtentTest node created
 *   @BeforeMethod (this class) → LoginPage and HomePage instantiated
 *   @Test         → scenario executes
 *   @AfterMethod  → pass/fail logged, screenshot on failure, browser quit
 *   @AfterSuite   → HTML report written to disk
 *
 * NOTE: BaseTest's @BeforeMethod runs before this class's @BeforeMethod
 * because TestNG calls parent class hooks first. The driver is therefore
 * already initialised when setUp() here runs.
 */
public class LoginTest extends BaseTest {

    private static final Logger log = LogManager.getLogger(LoginTest.class);

    private LoginPage loginPage;
    private HomePage homePage;

    // -------------------------------------------------------------------------
    // @BeforeMethod — instantiates page objects using the driver that BaseTest
    // already initialised. Page objects are re-created for every test method
    // because BaseTest quits and restarts the browser between tests.
    //
    // Uses alwaysRun = true so this method runs even for tests in groups that
    // are not explicitly included — prevents a NullPointerException if a test
    // is run individually outside the full suite context.
    // -------------------------------------------------------------------------
    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        log.debug("Initialising LoginPage and HomePage page objects.");
        loginPage = new LoginPage(getDriver());
        homePage  = new HomePage(getDriver());
    }

    // =========================================================================
    // TESTS
    // =========================================================================

    /**
     * testValidLogin — verifies that a user with valid credentials can log in
     * and is returned to the home page with an active session.
     *
     * Sanity group: runs in the quick smoke suite before regression to give
     * fast feedback on whether the login flow is fundamentally broken.
     *
     * Steps:
     *   1. Open the login page.
     *   2. Enter the valid test account credentials from Constants.
     *   3. Click the Login button.
     *   4. Assert the home page is displayed.
     *   5. Assert the "Logged in as" indicator is visible.
     */
    @Test(groups = {"sanity", "regression"}, description = "Valid user can log in and reach the home page")
    public void testValidLogin() {
        log.info("=== TEST: testValidLogin ===");

        extentTest.info("Navigating to the Login page.");
        loginPage.navigateToLoginPage();

        extentTest.info("Entering valid email: " + Constants.VALID_EMAIL);
        loginPage.enterEmail(Constants.VALID_EMAIL);

        extentTest.info("Entering valid password.");
        loginPage.enterPassword(Constants.VALID_PASSWORD);

        extentTest.info("Clicking the Login button.");
        loginPage.clickLoginButton();

        extentTest.info("Asserting that the Home page is displayed after login.");
        Assert.assertTrue(homePage.isHomePageDisplayed(), Errors.LOGIN_FAILED);
        log.info("Home page displayed after valid login — assertion passed.");

        extentTest.info("Asserting that the user session indicator is visible.");
        Assert.assertTrue(homePage.isUserLoggedIn(), Errors.USER_NOT_LOGGED_IN);
        log.info("User is logged in — assertion passed.");

        extentTest.pass("testValidLogin passed — user successfully logged in.");
    }

    /**
     * testInvalidLogin — verifies that the application rejects invalid credentials
     * and displays a meaningful error message rather than silently failing or
     * redirecting to the home page.
     *
     * Steps:
     *   1. Open the login page.
     *   2. Enter a non-existent email and wrong password.
     *   3. Click the Login button.
     *   4. Assert an error message is displayed on the login page.
     *   5. Assert the user is NOT logged in (still on login page).
     */
    @Test(groups = {"regression"}, description = "Invalid credentials show an error message")
    public void testInvalidLogin() {
        log.info("=== TEST: testInvalidLogin ===");

        extentTest.info("Navigating to the Login page.");
        loginPage.navigateToLoginPage();

        extentTest.info("Entering invalid email: invalid@email.com");
        loginPage.enterEmail("invalid@email.com");

        extentTest.info("Entering incorrect password.");
        loginPage.enterPassword("wrongpassword");

        extentTest.info("Clicking the Login button.");
        loginPage.clickLoginButton();

        extentTest.info("Asserting that an error message is displayed.");
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), Errors.INVALID_LOGIN);
        log.info("Login page still displayed after invalid credentials — expected.");

        String errorMessage = loginPage.getErrorMessage();
        extentTest.info("Error message displayed: " + errorMessage);
        Assert.assertFalse(errorMessage.isEmpty(), Errors.INVALID_LOGIN);
        log.info("Error message visible: '{}' — assertion passed.", errorMessage);

        extentTest.pass("testInvalidLogin passed — error message displayed for bad credentials.");
    }

    /**
     * testLoginWithMultipleUsers — data-driven test that reads credential rows
     * from the LoginData sheet of testdata.xlsx and runs the login flow for each.
     *
     * WHY DATA-DRIVEN FOR LOGIN?
     * The same login action must be verified for multiple accounts:
     *   - A valid registered user           → expects "success" → home page shown
     *   - An unregistered email             → expects "failure" → error shown
     * Rather than writing a separate test method for each case, one parameterised
     * method handles all of them, and new cases are added by appending rows to Excel.
     *
     * Parameters injected by the DataProvider:
     *   email          — the email to enter
     *   password       — the password to enter
     *   expectedResult — "success" or "failure" (case-insensitive)
     */
    @Test(dataProvider = "loginData", groups = {"regression"},
          description = "Data-driven login test covering success and failure scenarios")
    public void testLoginWithMultipleUsers(String email, String password, String expectedResult) {
        log.info("=== TEST: testLoginWithMultipleUsers — email: '{}', expected: '{}' ===",
                email, expectedResult);

        extentTest.info("Data-driven login test — email: " + email + ", expected: " + expectedResult);

        extentTest.info("Navigating to the Login page.");
        loginPage.navigateToLoginPage();

        extentTest.info("Entering email: " + email);
        loginPage.enterEmail(email);

        extentTest.info("Entering password.");
        loginPage.enterPassword(password);

        extentTest.info("Clicking the Login button.");
        loginPage.clickLoginButton();

        if ("success".equalsIgnoreCase(expectedResult)) {
            extentTest.info("Expected result is SUCCESS — asserting home page is displayed.");
            Assert.assertTrue(homePage.isHomePageDisplayed(), Errors.LOGIN_FAILED);
            Assert.assertTrue(homePage.isUserLoggedIn(), Errors.USER_NOT_LOGGED_IN);
            log.info("Login SUCCESS path verified for: '{}'", email);
            extentTest.pass("Login succeeded as expected for: " + email);

        } else {
            extentTest.info("Expected result is FAILURE — asserting error message is displayed.");
            Assert.assertTrue(loginPage.isLoginPageDisplayed(), Errors.INVALID_LOGIN);
            String error = loginPage.getErrorMessage();
            Assert.assertFalse(error.isEmpty(), Errors.INVALID_LOGIN);
            log.info("Login FAILURE path verified for: '{}' — error: '{}'", email, error);
            extentTest.pass("Login rejected as expected for: " + email + " | Error: " + error);
        }
    }

    // =========================================================================
    // DATA PROVIDER
    // =========================================================================

    /**
     * loginData — TestNG DataProvider that reads the LoginData sheet from
     * testdata.xlsx and returns each row as a test iteration.
     *
     * HOW IT CONNECTS TO testLoginWithMultipleUsers:
     *   TestNG sees dataProvider = "loginData" on the @Test annotation and calls
     *   this method first to get the Object[][]. It then calls
     *   testLoginWithMultipleUsers() once per row, mapping:
     *     column 0 → String email
     *     column 1 → String password
     *     column 2 → String expectedResult
     *
     * The DataProvider is defined in this class so it is immediately visible
     * alongside the test that uses it. For shared providers used by multiple
     * test classes, move them to a dedicated DataProviders utility class and
     * reference them with dataProviderClass = DataProviders.class.
     */
    @DataProvider(name = "loginData")
    public Object[][] loginData() {
        log.info("Loading login test data from Excel sheet: '{}'", Constants.EXCEL_LOGIN_SHEET);
        ExcelUtil excel = new ExcelUtil(ConfigReader.getInstance().getTestDataPath());
        Object[][] data = excel.getTestData(Constants.EXCEL_LOGIN_SHEET);
        log.info("Login data loaded — {} test case(s) found.", data.length);
        return data;
    }
}
