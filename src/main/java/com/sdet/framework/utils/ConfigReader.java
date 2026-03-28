package com.sdet.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigReader — Singleton utility that loads config.properties once at startup
 * and exposes every key through typed helper methods.
 *
 * Why Singleton? The Properties file only needs to be read from disk once.
 * A shared instance avoids repeated I/O and guarantees all framework classes
 * see the exact same configuration values throughout a test run.
 */
public class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);

    // The single shared instance — created only on first call to getInstance().
    private static ConfigReader instance;

    // Holds all key-value pairs loaded from config.properties.
    private final Properties properties;

    // Path to the properties file, relative to the project root.
    private static final String CONFIG_FILE = "src/main/resources/config.properties";

    // -------------------------------------------------------------------------
    // Private constructor — prevents direct instantiation from outside.
    // Loads config.properties into the Properties object at construction time.
    // -------------------------------------------------------------------------
    private ConfigReader() {
        properties = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            log.info("config.properties loaded successfully from: {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("Failed to load config.properties from: {}", CONFIG_FILE, e);
            throw new RuntimeException("Could not load config.properties. " +
                    "Ensure the file exists at: " + CONFIG_FILE, e);
        }
    }

    // -------------------------------------------------------------------------
    // getInstance() — returns the single ConfigReader instance, creating it on
    // the first call. Synchronized to be thread-safe under parallel test runs.
    // -------------------------------------------------------------------------
    public static synchronized ConfigReader getInstance() {
        if (instance == null) {
            log.debug("Creating ConfigReader singleton instance.");
            instance = new ConfigReader();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // getProperty(key) — generic lookup for any key in config.properties.
    // Throws RuntimeException if the key is missing so misconfiguration is
    // caught immediately rather than producing a silent null downstream.
    // -------------------------------------------------------------------------
    public String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            log.error("Property '{}' not found in config.properties.", key);
            throw new RuntimeException("Missing required property: '" + key + "' in config.properties.");
        }
        log.debug("Property resolved — key: '{}', value: '{}'", key, value);
        return value.trim();
    }

    // -------------------------------------------------------------------------
    // getBrowser() — returns the target browser name (e.g. "chrome", "firefox").
    // Used by DriverFactory to decide which WebDriver implementation to create.
    // -------------------------------------------------------------------------
    public String getBrowser() {
        return getProperty("browser");
    }

    // -------------------------------------------------------------------------
    // getUrl() — reads the active environment from the "env" key, then resolves
    // the matching "url.<env>" key so callers always get the correct base URL
    // without needing to know which environment is currently active.
    // -------------------------------------------------------------------------
    public String getUrl() {
        String env = getProperty("env");
        log.debug("Active environment: '{}'", env);
        return getProperty("url." + env);
    }

    // -------------------------------------------------------------------------
    // getImplicitWait() — returns the implicit wait duration in seconds as an int.
    // Applied once to driver.manage().timeouts() immediately after driver creation.
    // -------------------------------------------------------------------------
    public int getImplicitWait() {
        return Integer.parseInt(getProperty("implicit.wait"));
    }

    // -------------------------------------------------------------------------
    // getExplicitWait() — returns the explicit wait duration in seconds as an int.
    // Passed into every WebDriverWait / FluentWait instance in the page classes.
    // -------------------------------------------------------------------------
    public int getExplicitWait() {
        return Integer.parseInt(getProperty("explicit.wait"));
    }

    // -------------------------------------------------------------------------
    // getGridUrl() — returns the Selenium Grid hub URL.
    // Used by DriverFactory when the framework is running in remote/grid mode.
    // -------------------------------------------------------------------------
    public String getGridUrl() {
        return getProperty("grid.url");
    }

    // -------------------------------------------------------------------------
    // getReportPath() — returns the relative path where ExtentReports writes
    // the HTML report file after the test run completes.
    // -------------------------------------------------------------------------
    public String getReportPath() {
        return getProperty("report.path");
    }

    // -------------------------------------------------------------------------
    // getTestDataPath() — returns the relative path to the Excel test data file.
    // Used by the ExcelReader utility to locate the workbook via Apache POI.
    // -------------------------------------------------------------------------
    public String getTestDataPath() {
        return getProperty("testdata.path");
    }
}
