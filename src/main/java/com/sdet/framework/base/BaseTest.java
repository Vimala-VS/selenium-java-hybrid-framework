package com.sdet.framework.base;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.sdet.framework.factory.DriverFactory;
import com.sdet.framework.utils.ConfigReader;
import com.sdet.framework.utils.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.lang.reflect.Method;

/**
 * BaseTest — the parent class for every TestNG test class in the framework.
 *
 * TESTNG LIFECYCLE ORDER (per suite run):
 * ----------------------------------------
 *
 *   @BeforeSuite   — runs ONCE before any test in the suite starts.
 *                    Used here to initialise ExtentReports so the report
 *                    file is ready to receive entries from all tests.
 *
 *   @BeforeMethod  — runs before EVERY @Test method.
 *                    Used here to launch a fresh browser and create a new
 *                    ExtentTest node for the current test method.
 *
 *        @Test     — the actual test method written in the subclass.
 *                    The subclass accesses the driver via getDriver() and
 *                    logs steps via the protected `extentTest` field.
 *
 *   @AfterMethod   — runs after EVERY @Test method, pass or fail.
 *                    Used here to capture a screenshot on failure, update
 *                    the report status, and quit the browser.
 *
 *   @AfterSuite    — runs ONCE after all tests in the suite have finished.
 *                    Used here to flush ExtentReports, which physically
 *                    writes the HTML file to disk. Without flush(), the
 *                    report file is empty even if tests ran.
 *
 * PARALLEL SAFETY:
 * ----------------
 * ExtentReports itself is thread-safe for concurrent test logging.
 * Each thread gets its own ExtentTest via a ThreadLocal so parallel
 * @Test methods write to their own report node without interfering.
 * DriverFactory also uses ThreadLocal, so each thread drives its own browser.
 */
public class BaseTest {

    private static final Logger log = LogManager.getLogger(BaseTest.class);

    // Shared ExtentReports instance — one per suite, created in @BeforeSuite.
    private static ExtentReports extent;

    // ThreadLocal ExtentTest — each running thread (test method) has its own
    // report node so parallel tests log to separate entries in the HTML report.
    private static final ThreadLocal<ExtentTest> extentTestThreadLocal = new ThreadLocal<>();

    /**
     * Protected accessor so subclasses can log steps directly to the report:
     *   extentTest.log(Status.INFO, "Clicked the Login button.");
     */
    protected ExtentTest extentTest;

    // -------------------------------------------------------------------------
    // @BeforeSuite — initialises ExtentReports once before the suite starts.
    // ExtentSparkReporter builds the HTML report at the configured path.
    // System info (OS, Java version, browser) is embedded in the report header.
    // -------------------------------------------------------------------------
    @BeforeSuite
    public void setUpSuite() {
        log.info("=== @BeforeSuite: Initialising ExtentReports ===");

        ConfigReader config = ConfigReader.getInstance();
        String reportPath = config.getReportPath();

        // ExtentSparkReporter produces the modern HTML report (replaces the old
        // ExtentHtmlReporter). Point it at the path from config.properties.
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setDocumentTitle("AutoExerPom Test Report");
        sparkReporter.config().setReportName("Automation Exercise — Regression Suite");
        sparkReporter.config().setEncoding("UTF-8");

        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        // Embed environment metadata in the report Overview tab.
        extent.setSystemInfo("Environment", config.getProperty("env").toUpperCase());
        extent.setSystemInfo("Browser",     config.getBrowser());
        extent.setSystemInfo("Base URL",    config.getUrl());
        extent.setSystemInfo("OS",          System.getProperty("os.name"));
        extent.setSystemInfo("Java",        System.getProperty("java.version"));

        log.info("ExtentReports initialised — report will be written to: {}", reportPath);
    }

    // -------------------------------------------------------------------------
    // @BeforeMethod — runs before every @Test method.
    // Receives the Method object injected by TestNG so we can read the test
    // method name and use it as the ExtentTest (report node) title.
    //
    // Order of operations:
    //   1. Log the test start.
    //   2. Launch a new browser via DriverFactory (stores driver in ThreadLocal).
    //   3. Create an ExtentTest node for this test method.
    //   4. Assign the node to the protected field so subclasses can log steps.
    // -------------------------------------------------------------------------
    @BeforeMethod
    public void setUp(Method method) {
        String testName = method.getName();
        log.info("=== @BeforeMethod: Starting test '{}' ===", testName);

        // Launch the browser for this test thread.
        DriverFactory.initDriver();
        log.info("WebDriver initialised for test: '{}'", testName);

        // Create a new node in the HTML report for this test method.
        ExtentTest test = extent.createTest(testName);
        extentTestThreadLocal.set(test);

        // Assign to protected field so subclasses can reference it as `extentTest`.
        extentTest = test;
        log.debug("ExtentTest node created for: '{}'", testName);
    }

    // -------------------------------------------------------------------------
    // getDriver() — convenience accessor for subclasses. Delegates to
    // DriverFactory.getDriver() which retrieves this thread's WebDriver from
    // ThreadLocal. Subclasses call this instead of touching DriverFactory directly.
    // -------------------------------------------------------------------------
    protected WebDriver getDriver() {
        return DriverFactory.getDriver();
    }

    // -------------------------------------------------------------------------
    // @AfterMethod — runs after every @Test method regardless of outcome.
    // ITestResult is injected by TestNG and carries the pass/fail/skip status.
    //
    // Order of operations:
    //   1. Retrieve the ExtentTest node for this thread.
    //   2. On FAILURE — take a screenshot, attach it to the report, mark FAIL.
    //   3. On SUCCESS — mark PASS.
    //   4. On SKIP   — mark SKIP.
    //   5. Quit the browser and remove the driver from ThreadLocal.
    // -------------------------------------------------------------------------
    @AfterMethod
    public void tearDown(ITestResult result) {
        String testName = result.getName();
        ExtentTest test = extentTestThreadLocal.get();

        if (result.getStatus() == ITestResult.FAILURE) {
            log.error("Test FAILED: '{}'", testName);

            // Capture a screenshot named after the test method.
            // BasePage.takeScreenshot() saves the file and returns the path.
            BasePage basePage = new BasePage(DriverFactory.getDriver());
            String screenshotPath = basePage.takeScreenshot(testName + "_FAILED");

            // Attach the failure exception message to the report node.
            test.log(Status.FAIL, "Test failed: " + result.getThrowable().getMessage());

            // Attach the screenshot as a clickable link in the report.
            test.addScreenCaptureFromPath(screenshotPath, "Failure Screenshot");
            log.info("Screenshot attached to report for failed test: '{}'", testName);

        } else if (result.getStatus() == ITestResult.SUCCESS) {
            log.info("Test PASSED: '{}'", testName);
            test.log(Status.PASS, "Test passed successfully.");

        } else if (result.getStatus() == ITestResult.SKIP) {
            log.warn("Test SKIPPED: '{}'", testName);
            test.log(Status.SKIP, "Test was skipped: " + result.getThrowable().getMessage());
        }

        // Always quit the browser and clean up ThreadLocal — regardless of status.
        DriverFactory.quitDriver();
        extentTestThreadLocal.remove();
        log.info("=== @AfterMethod: Completed teardown for '{}' ===", testName);
    }

    // -------------------------------------------------------------------------
    // @AfterSuite — runs once after every test in the suite has finished.
    // extent.flush() serialises all in-memory test results to the HTML file.
    // This MUST be called — without it the report file exists but is empty.
    // -------------------------------------------------------------------------
    @AfterSuite
    public void tearDownSuite() {
        log.info("=== @AfterSuite: Flushing ExtentReports to disk ===");
        if (extent != null) {
            extent.flush();
            log.info("ExtentReports flushed. Report written to: {}",
                    ConfigReader.getInstance().getReportPath());
        }
    }
}
