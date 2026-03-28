package com.sdet.stepdefinitions;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.sdet.framework.factory.DriverFactory;
import com.sdet.framework.utils.ConfigReader;
import com.sdet.framework.utils.LogManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Hooks — Cucumber lifecycle manager for the AutoExerPom framework.
 *
 * CUCUMBER @Before / @After vs. TESTNG @BeforeMethod / @AfterMethod:
 * -------------------------------------------------------------------
 * TestNG and Cucumber both have before/after hooks but they serve different
 * runners and must not be mixed.
 *
 * TestNG @BeforeMethod / @AfterMethod (used in BaseTest):
 *   - Driven by TestNG's own engine (XML suite files, test runners).
 *   - Annotated with org.testng.annotations.BeforeMethod.
 *   - TestNG calls them automatically when it discovers @Test methods.
 *   - Used by LoginTest, ProductTest, CheckoutTest (plain TestNG tests).
 *
 * Cucumber @Before / @After (used here):
 *   - Driven by Cucumber's engine, which reads .feature files.
 *   - Annotated with io.cucumber.java.Before / io.cucumber.java.After.
 *   - Cucumber calls them around each Scenario, not each @Test method.
 *   - Used by all Cucumber step definition scenarios.
 *
 * WHY HOOKS REPLACES BaseTest FOR CUCUMBER SCENARIOS:
 * ----------------------------------------------------
 * BaseTest wires its lifecycle to TestNG annotations. When Cucumber runs a
 * Scenario, it does not invoke TestNG's @BeforeMethod — it invokes Cucumber's
 * @Before hook instead. If a Cucumber scenario tried to inherit from BaseTest
 * it would find that initDriver() was never called (because TestNG's
 * @BeforeMethod never fired) and the first getDriver() call would throw
 * IllegalStateException.
 *
 * Hooks.java is the Cucumber-native equivalent of BaseTest. It provides the
 * same browser lifecycle (init → test → quit) and the same reporting lifecycle
 * (create ExtentTest → log result → flush) but wired to Cucumber's event model
 * instead of TestNG's. Step definitions share state with Hooks via PicoContainer
 * dependency injection (cucumber-picocontainer dependency in pom.xml).
 *
 * EXTENTREPORTS MANAGEMENT:
 * --------------------------
 * ExtentReports is initialised as a static field the first time any @Before runs
 * (guarded by a null check) and flushed by a JVM shutdown hook registered at
 * that same moment. This avoids the need for a @BeforeSuite equivalent in
 * Cucumber — there is no direct Cucumber annotation for suite-level setup,
 * but the static initialise-once pattern achieves the same result.
 */
public class Hooks {

    private static final Logger log = LogManager.getLogger(Hooks.class);

    // Shared ExtentReports instance — initialised once for the entire Cucumber run.
    private static ExtentReports extent;

    // ThreadLocal ExtentTest — one report node per running scenario thread.
    private static final ThreadLocal<ExtentTest> extentTestThreadLocal = new ThreadLocal<>();

    // -------------------------------------------------------------------------
    // initExtentReports() — creates the ExtentReports instance and registers a
    // JVM shutdown hook to flush it. Called once from the first @Before that
    // runs. Subsequent @Before calls skip initialisation (extent != null check).
    //
    // Shutdown hook guarantees the HTML file is written even if the Cucumber
    // runner exits without an explicit flush call — useful when the suite is
    // interrupted mid-run by a CI timeout or Ctrl+C.
    // -------------------------------------------------------------------------
    private static synchronized void initExtentReports() {
        if (extent != null) {
            return; // Already initialised by a previous scenario.
        }

        ConfigReader config = ConfigReader.getInstance();
        String reportPath = config.getReportPath();

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setDocumentTitle("AutoExerPom Cucumber Report");
        sparkReporter.config().setReportName("Automation Exercise — Cucumber Regression");
        sparkReporter.config().setEncoding("UTF-8");

        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        extent.setSystemInfo("Environment", config.getProperty("env").toUpperCase());
        extent.setSystemInfo("Browser",     config.getBrowser());
        extent.setSystemInfo("Base URL",    config.getUrl());
        extent.setSystemInfo("OS",          System.getProperty("os.name"));
        extent.setSystemInfo("Java",        System.getProperty("java.version"));

        // Flush the report when the JVM exits so no explicit @AfterSuite is needed.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook: flushing ExtentReports to disk.");
            extent.flush();
            log.info("ExtentReports flushed — report written to: {}", reportPath);
        }));

        log.info("ExtentReports initialised — report path: {}", reportPath);
    }

    // -------------------------------------------------------------------------
    // @Before — runs before every Cucumber Scenario.
    //
    // Order of operations:
    //   1. Ensure ExtentReports is initialised (idempotent — safe to call every time).
    //   2. Launch a browser via DriverFactory and store it in ThreadLocal.
    //   3. Create an ExtentTest node named after the scenario.
    //
    // The Scenario object injected by Cucumber carries the scenario name, tags,
    // and (after execution) the pass/fail status — making it the Cucumber
    // equivalent of TestNG's ITestResult.
    // -------------------------------------------------------------------------
    @Before
    public void beforeScenario(Scenario scenario) {
        initExtentReports();

        String scenarioName = scenario.getName();
        log.info(">>> SCENARIO STARTING: '{}' | Tags: {}", scenarioName, scenario.getSourceTagNames());

        // Launch the browser for this scenario's thread.
        DriverFactory.initDriver();
        log.info("WebDriver initialised for scenario: '{}'", scenarioName);

        // Create a report node for this scenario.
        ExtentTest extentTest = extent.createTest(scenarioName);
        extentTestThreadLocal.set(extentTest);
        extentTest.info("Scenario started: " + scenarioName);
        extentTest.assignCategory(scenario.getSourceTagNames().toArray(new String[0]));
    }

    // -------------------------------------------------------------------------
    // @After — runs after every Cucumber Scenario regardless of outcome.
    //
    // The Scenario object's isFailed() method reflects the final status after
    // all steps have executed. Cucumber sets this automatically — no try/catch
    // is needed in the step definitions.
    //
    // Order of operations:
    //   1. On failure: capture a screenshot as bytes, attach to both the
    //      Cucumber HTML report (scenario.attach) and ExtentReports.
    //   2. Update the ExtentTest node with pass or fail status.
    //   3. Quit the browser and clean up ThreadLocal.
    // -------------------------------------------------------------------------
    @After
    public void afterScenario(Scenario scenario) {
        String scenarioName = scenario.getName();
        ExtentTest extentTest = extentTestThreadLocal.get();

        if (scenario.isFailed()) {
            log.error("SCENARIO FAILED: '{}'", scenarioName);

            // Capture screenshot as a byte array.
            // Using OutputType.BYTES allows attaching directly to both reporting
            // systems without writing a file to disk first.
            try {
                WebDriver driver = DriverFactory.getDriver();
                byte[] screenshot = ((TakesScreenshot) driver)
                        .getScreenshotAs(OutputType.BYTES);

                // Attach to the Cucumber HTML report — visible in the scenario's
                // step output when the report is opened in a browser.
                scenario.attach(screenshot, "image/png", scenarioName + "_FAILED");
                log.info("Screenshot attached to Cucumber report for failed scenario: '{}'", scenarioName);

                // Attach to ExtentReports as a Base64-encoded inline image.
                extentTest.addScreenCaptureFromBase64String(
                        java.util.Base64.getEncoder().encodeToString(screenshot),
                        "Failure Screenshot");

            } catch (Exception e) {
                log.error("Could not capture screenshot for scenario '{}': {}", scenarioName, e.getMessage(), e);
            }

            extentTest.log(Status.FAIL, "Scenario FAILED: " + scenarioName);

        } else {
            log.info("SCENARIO PASSED: '{}'", scenarioName);
            extentTest.log(Status.PASS, "Scenario PASSED: " + scenarioName);
        }

        // Always quit the browser and remove from ThreadLocal — regardless of status.
        DriverFactory.quitDriver();
        extentTestThreadLocal.remove();
        log.info("<<< SCENARIO COMPLETE: '{}' | Status: {}",
                scenarioName, scenario.isFailed() ? "FAILED" : "PASSED");
    }

    /**
     * getExtentTest() — allows step definition classes to retrieve the current
     * thread's ExtentTest node and log individual steps to the report.
     *
     * Usage in a step definition class:
     *   Hooks.getExtentTest().info("User clicked the login button.");
     */
    public static ExtentTest getExtentTest() {
        return extentTestThreadLocal.get();
    }
}
