package com.sdet.framework.factory;

import com.sdet.framework.utils.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

/**
 * DriverFactory — creates, stores, and tears down WebDriver instances.
 *
 * WHY ThreadLocal?
 * ----------------
 * When TestNG or Cucumber runs scenarios in parallel, multiple threads execute
 * simultaneously. A plain static WebDriver field would be shared across all
 * threads, causing one thread to accidentally operate the browser opened by
 * another thread — leading to unpredictable failures and race conditions.
 *
 * ThreadLocal<WebDriver> gives every thread its own independent copy of the
 * driver variable. Thread A's driver is completely invisible to Thread B and
 * vice-versa, so parallel scenarios each drive their own browser in isolation.
 *
 * Rule: always call initDriver() at the start of a scenario and quitDriver()
 * at the end so the ThreadLocal slot is cleaned up and no driver leaks occur.
 */
public class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    /**
     * ThreadLocal store — each test thread holds its own WebDriver reference here.
     * Initialised to null so we can detect an uninitialised driver easily.
     */
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    // Prevent instantiation — all methods are accessed statically.
    private DriverFactory() {
    }

    // -------------------------------------------------------------------------
    // initDriver() — resolves the browser type from config, downloads/configures
    // the matching driver binary via WebDriverManager, then stores the created
    // WebDriver in ThreadLocal so the current thread can retrieve it later.
    //
    // If "grid.mode=true" is detected (or any remote flag), a RemoteWebDriver
    // pointing at the Selenium Grid hub is created instead of a local driver.
    // -------------------------------------------------------------------------
    public static void initDriver() {
        ConfigReader config = ConfigReader.getInstance();
        String browser = config.getBrowser().toLowerCase().trim();
        boolean isRemote = isRemoteExecution(config);

        log.info("Initialising WebDriver — browser: '{}', remote: {}", browser, isRemote);

        WebDriver driver;

        if (isRemote) {
            driver = createRemoteDriver(browser, config.getGridUrl());
        } else {
            driver = createLocalDriver(browser);
        }

        // Apply implicit wait globally once, immediately after driver creation.
        int implicitWait = config.getImplicitWait();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        log.debug("Implicit wait set to {} seconds.", implicitWait);

        // Maximise the browser window for consistent element visibility.
        driver.manage().window().maximize();
        log.debug("Browser window maximised.");

        // Store the driver in ThreadLocal — this thread can now retrieve it via
        // getDriver().
        driverThreadLocal.set(driver);
        log.info("WebDriver stored in ThreadLocal for thread: {}", Thread.currentThread().getName());
    }

    // -------------------------------------------------------------------------
    // getDriver() — returns the WebDriver instance belonging to the calling thread.
    // Throws IllegalStateException if initDriver() was never called on this thread,
    // making the root cause clear rather than producing a NullPointerException.
    // -------------------------------------------------------------------------
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver has not been initialised for thread: "
                            + Thread.currentThread().getName()
                            + ". Call DriverFactory.initDriver() before getDriver().");
        }
        return driver;
    }

    // -------------------------------------------------------------------------
    // quitDriver() — shuts down the browser and removes the driver from
    // ThreadLocal. Removal is mandatory: without it the ThreadLocal entry
    // lingers in thread-pool threads, causing driver leaks across test runs.
    // -------------------------------------------------------------------------
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            log.info("Quitting WebDriver for thread: {}", Thread.currentThread().getName());
            driver.quit();
            // Remove the reference so the thread does not hold a stale driver object.
            driverThreadLocal.remove();
            log.debug("WebDriver removed from ThreadLocal.");
        } else {
            log.warn("quitDriver() called but no WebDriver found in ThreadLocal for thread: {}",
                    Thread.currentThread().getName());
        }
    }

    // -------------------------------------------------------------------------
    // createLocalDriver() — uses WebDriverManager to auto-download the correct
    // driver binary, then instantiates the appropriate local WebDriver subclass.
    // -------------------------------------------------------------------------
    private static WebDriver createLocalDriver(String browser) {
        switch (browser) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--remote-allow-origins=*");

                // Read headless flag — set via Maven: -Dheadless=true
                // Automatically true on CI (GitHub Actions sets it in the pipeline)
                boolean isHeadless = Boolean.parseBoolean(
                        System.getProperty("headless", "false"));

                if (isHeadless) {
                    chromeOptions.addArguments("--headless=new");
                    chromeOptions.addArguments("--no-sandbox");
                    chromeOptions.addArguments("--disable-dev-shm-usage");
                    chromeOptions.addArguments("--disable-gpu");
                    chromeOptions.addArguments("--window-size=1920,1080");
                    log.info("Chrome running in HEADLESS mode.");
                }

                log.debug("Creating local ChromeDriver.");
                return new ChromeDriver(chromeOptions);

            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                log.debug("Creating local FirefoxDriver.");
                return new FirefoxDriver(new FirefoxOptions());

            case "edge":
                WebDriverManager.edgedriver().setup();
                log.debug("Creating local EdgeDriver.");
                return new EdgeDriver(new EdgeOptions());

            default:
                throw new IllegalArgumentException(
                        "Unsupported browser: '" + browser + "'. " +
                                "Accepted values in config.properties: chrome, firefox, edge.");
        }
    }

    // -------------------------------------------------------------------------
    // createRemoteDriver() — builds the matching browser Options object and
    // creates a RemoteWebDriver that delegates all commands to the Grid hub.
    // The hub URL is read from config.properties (grid.url).
    // -------------------------------------------------------------------------
    private static WebDriver createRemoteDriver(String browser, String gridUrl) {
        log.info("Creating RemoteWebDriver — hub: {}", gridUrl);
        try {
            URL hubUrl = URI.create(gridUrl).toURL();
            switch (browser) {
                case "chrome":
                    return new RemoteWebDriver(hubUrl, new ChromeOptions());
                case "firefox":
                    return new RemoteWebDriver(hubUrl, new FirefoxOptions());
                case "edge":
                    return new RemoteWebDriver(hubUrl, new EdgeOptions());
                default:
                    throw new IllegalArgumentException(
                            "Unsupported browser for remote execution: '" + browser + "'.");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Selenium Grid URL: '" + gridUrl + "'", e);
        }
    }

    // -------------------------------------------------------------------------
    // isRemoteExecution() — checks whether the framework should create a
    // RemoteWebDriver. Currently driven by the system property "remote=true"
    // so CI pipelines can activate Grid mode without editing config.properties:
    // mvn test -Dremote=true
    // -------------------------------------------------------------------------
    private static boolean isRemoteExecution(ConfigReader config) {
        String remoteFlag = System.getProperty("remote", "false");
        boolean isRemote = Boolean.parseBoolean(remoteFlag);
        log.debug("Remote execution flag (system property 'remote'): {}", isRemote);
        return isRemote;
    }
}
