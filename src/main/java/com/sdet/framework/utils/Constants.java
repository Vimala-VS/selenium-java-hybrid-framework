package com.sdet.framework.utils;

/**
 * Constants — a single source of truth for all fixed values used across the framework.
 *
 * WHY CENTRALISE CONSTANTS?
 * --------------------------
 * When a value like a page title or wait duration is needed in multiple places,
 * the naive approach is to type it wherever it is needed. This creates two risks:
 *
 *   1. TYPO DRIFT — "Automation Exercise" typed ten times across ten files will
 *      eventually be typed wrong in at least one. The test fails, the developer
 *      stares at it wondering why, and the root cause is a missing space. A
 *      constant typed once can only be wrong once, and the compiler finds it.
 *
 *   2. CHANGE AMPLIFICATION — the site updates its page title. Now you must find
 *      every occurrence of the old string in the codebase and update it. With a
 *      constant, you change one line and every reference is fixed automatically.
 *
 * All values here are public static final:
 *   public — accessible from any class in the framework without instantiation.
 *   static — belongs to the class, not to any instance (no new Constants() needed).
 *   final  — value is assigned once at class load and can never be changed at runtime.
 *
 * Usage from any class:
 *   Assert.assertEquals(homePage.getPageTitle(), Constants.HOME_PAGE_TITLE);
 *   driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(Constants.IMPLICIT_WAIT));
 */
public class Constants {

    // Prevent instantiation — this is a static constants holder only.
    private Constants() {}

    // =========================================================================
    // APPLICATION URLS
    // =========================================================================

    // Root URL of the application under test. All page URLs are built from this.
    // Prefer ConfigReader.getUrl() in runtime code; use this in assertions and
    // tests that need a compile-time constant reference.
    public static final String BASE_URL = "https://www.automationexercise.com";

    // =========================================================================
    // PAGE TITLES
    // Used with getPageTitle() assertions to confirm correct page navigation.
    // These must match the <title> tag text exactly — including spacing and case.
    // =========================================================================

    // Title of the Login / Signup page.
    public static final String LOGIN_PAGE_TITLE = "Automation Exercise - Signup / Login";

    // Title of the home page shown after login or on direct navigation.
    public static final String HOME_PAGE_TITLE = "Automation Exercise";

    // Title of the Products catalogue page.
    public static final String PRODUCTS_PAGE_TITLE = "Automation Exercise - All Products";

    // =========================================================================
    // TEST CREDENTIALS
    // These are the default credentials used when no data-driven source is provided.
    // For data-driven login tests use ExcelUtil with EXCEL_LOGIN_SHEET instead.
    // Do NOT use real production credentials here — use a dedicated test account.
    // =========================================================================

    // Email address of the pre-registered test account on automationexercise.com.
    public static final String VALID_EMAIL = "testsdetuser@gmail.com";

    // Password for the pre-registered test account.
    public static final String VALID_PASSWORD = "testuser@1234";

    // =========================================================================
    // TEST DATA
    // Fixed product name used in search, add-to-cart, and checkout scenarios.
    // =========================================================================

    // Name of a product known to exist on the site, used in product flow tests.
    public static final String TEST_PRODUCT_NAME = "Blue Top";

    // =========================================================================
    // WAIT DURATIONS (seconds)
    // Mirror the values in config.properties. Constants are used in tests and
    // utilities that need a compile-time value; ConfigReader is used at runtime
    // so the values can be overridden without recompiling.
    // =========================================================================

    // Applied once to driver.manage().timeouts().implicitlyWait().
    public static final int IMPLICIT_WAIT = 10;

    // Passed into WebDriverWait / FluentWait instances in BasePage.
    public static final int EXPLICIT_WAIT = 20;

    // =========================================================================
    // FILE PATHS
    // =========================================================================

    // Directory where BasePage.takeScreenshot() saves PNG files.
    // Relative to the project root, matching the path used in BaseTest.
    public static final String SCREENSHOT_PATH = "reports/screenshots/";

    // =========================================================================
    // EXCEL SHEET NAMES
    // Sheet names in testdata.xlsx. Centralised here so a sheet rename in the
    // workbook requires only one code change, not a search across all test files.
    //
    // Sheet: LoginData
    //   Columns: email | password | expectedResult
    //   Row 1:   valid@email.com    | Valid@123 | success
    //   Row 2:   invalid@email.com  | wrong     | failure
    //
    // Sheet: ProductData
    //   Columns: productName | expectedPrice
    //   Row 1:   Blue Top    | Rs. 500
    // =========================================================================

    // Sheet name for login data-driven test cases.
    public static final String EXCEL_LOGIN_SHEET = "LoginData";

    // Sheet name for product search and cart data-driven test cases.
    public static final String EXCEL_PRODUCT_SHEET = "ProductData";
}
