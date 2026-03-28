package com.sdet.framework.base;

import com.sdet.framework.utils.ConfigReader;
import com.sdet.framework.utils.LogManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

/**
 * BasePage — the parent class for every Page Object in the framework.
 *
 * All page classes extend BasePage and inherit its WebDriver reference,
 * WebDriverWait instance, and the complete set of reusable interaction
 * methods. No page class should interact with the driver directly — all
 * browser actions go through the methods defined here.
 *
 * WHY EXPLICIT WAITS INSTEAD OF IMPLICIT WAITS?
 * -----------------------------------------------
 * Implicit wait is a global setting applied to every findElement call on the
 * driver. It sounds convenient but causes two serious problems:
 *
 *   1. Mixing both wait types produces undefined, non-additive behaviour.
 *      Selenium's own documentation warns against using them together.
 *
 *   2. Implicit wait always waits the full timeout for elements that are
 *      EXPECTED to be absent (e.g. asserting an error message is NOT shown),
 *      making negative assertions slow by design.
 *
 * Explicit waits (WebDriverWait + ExpectedConditions) target a specific
 * element and a specific condition (visible, clickable, present, …) and
 * return the moment that condition is met. This makes tests both faster and
 * more descriptive — the wait itself documents the intent.
 *
 * The framework still sets a short implicit wait in DriverFactory as a
 * safety net for synchronous DOM reads, but all meaningful synchronisation
 * in page classes is done explicitly here.
 */
public class BasePage {

    protected WebDriver driver;
    protected WebDriverWait wait;

    private static final Logger log = LogManager.getLogger(BasePage.class);

    // Directory where screenshots are saved.
    private static final String SCREENSHOT_DIR = "reports/screenshots/";

    // -------------------------------------------------------------------------
    // Constructor — every page object receives the current thread's driver
    // and immediately creates a WebDriverWait using the explicit wait value
    // from config.properties. Both are stored as protected fields so subclasses
    // can access them without re-declaring.
    // -------------------------------------------------------------------------
    public BasePage(WebDriver driver) {
        this.driver = driver;
        int explicitWait = ConfigReader.getInstance().getExplicitWait();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(explicitWait));
        log.debug("BasePage initialised — explicit wait: {} seconds.", explicitWait);
    }

    // -------------------------------------------------------------------------
    // waitForVisibility(locator) — waits until the element located by the given
    // By is present in the DOM AND visible on the page, then returns it.
    // Used internally before read operations (getText, isDisplayed).
    // -------------------------------------------------------------------------
    public WebElement waitForVisibility(By locator) {
        log.debug("Waiting for visibility of element: {}", locator);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // -------------------------------------------------------------------------
    // waitForClickable(locator) — waits until the element is visible AND enabled
    // (not disabled, not obscured). Used internally before click operations to
    // prevent ElementNotInteractableException on elements that are present in
    // the DOM but not yet ready to receive user input.
    // -------------------------------------------------------------------------
    public WebElement waitForClickable(By locator) {
        log.debug("Waiting for element to be clickable: {}", locator);
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    // -------------------------------------------------------------------------
    // clickElement(locator) — waits for the element to be clickable, then clicks.
    // Always uses waitForClickable rather than a raw findElement click so the
    // action is resilient to brief render delays and animation states.
    // -------------------------------------------------------------------------
    public void clickElement(By locator) {
        log.info("Clicking element: {}", locator);
        waitForClickable(locator).click();
        log.debug("Click successful on: {}", locator);
    }

    // -------------------------------------------------------------------------
    // enterText(locator, text) — waits for the field to be clickable (ensuring
    // it is enabled and interactive), clears any pre-filled value, then types
    // the given text. Clearing first prevents leftover values from a previous
    // test polluting the current input.
    // -------------------------------------------------------------------------
    public void enterText(By locator, String text) {
        log.info("Entering text '{}' into element: {}", text, locator);
        WebElement element = waitForClickable(locator);
        element.clear();
        element.sendKeys(text);
        log.debug("Text entry complete on: {}", locator);
    }

    // -------------------------------------------------------------------------
    // getElementText(locator) — waits for the element to be visible (it must be
    // rendered to have meaningful text content), then returns its trimmed text.
    // Trimming removes accidental leading/trailing whitespace that can break
    // assertions.
    // -------------------------------------------------------------------------
    public String getElementText(By locator) {
        log.info("Getting text from element: {}", locator);
        String text = waitForVisibility(locator).getText().trim();
        log.debug("Text retrieved from {}: '{}'", locator, text);
        return text;
    }

    // -------------------------------------------------------------------------
    // isElementDisplayed(locator) — returns true if the element is present in
    // the DOM and visible; false otherwise. Catches NoSuchElementException so
    // callers get a clean boolean rather than an exception for absent elements.
    // Uses a zero-second wait via findElement (implicit wait handles brief delays)
    // because WebDriverWait would pause for the full timeout on a false result.
    // -------------------------------------------------------------------------
    public boolean isElementDisplayed(By locator) {
        log.info("Checking if element is displayed: {}", locator);
        try {
            boolean displayed = driver.findElement(locator).isDisplayed();
            log.debug("Element {} displayed: {}", locator, displayed);
            return displayed;
        } catch (NoSuchElementException e) {
            log.debug("Element not found in DOM — returning false for: {}", locator);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // selectDropdownByVisibleText(locator, text) — waits for the <select>
    // element to be visible, wraps it in a Select helper, then chooses the
    // option whose visible label matches the given text exactly.
    // -------------------------------------------------------------------------
    public void selectDropdownByVisibleText(By locator, String text) {
        log.info("Selecting '{}' from dropdown: {}", text, locator);
        WebElement dropdownElement = waitForVisibility(locator);
        Select select = new Select(dropdownElement);
        select.selectByVisibleText(text);
        log.debug("Dropdown selection '{}' applied on: {}", text, locator);
    }

    // -------------------------------------------------------------------------
    // getPageTitle() — returns the current page title directly from the driver.
    // No explicit wait needed here; the title is available as soon as the page
    // begins loading and is typically used after a navigation action.
    // -------------------------------------------------------------------------
    public String getPageTitle() {
        String title = driver.getTitle();
        log.info("Current page title: '{}'", title);
        return title;
    }

    // -------------------------------------------------------------------------
    // navigateTo(url) — instructs the driver to load the given URL and logs
    // the action. Page objects call this method rather than driver.get() directly
    // so navigation is traceable in the log.
    // -------------------------------------------------------------------------
    public void navigateTo(String url) {
        log.info("Navigating to URL: {}", url);
        driver.get(url);
        log.debug("Navigation complete.");
    }

    // -------------------------------------------------------------------------
    // takeScreenshot(fileName) — captures the full browser viewport as a PNG
    // and saves it to the screenshots directory under reports/.
    // Called from Cucumber hooks on test failure to attach evidence to reports.
    //
    // fileName should be descriptive and unique (e.g. include the scenario name
    // and a timestamp) to prevent older screenshots being overwritten.
    // -------------------------------------------------------------------------
    public String takeScreenshot(String fileName) {
        log.info("Taking screenshot: {}", fileName);
        TakesScreenshot ts = (TakesScreenshot) driver;
        File source = ts.getScreenshotAs(OutputType.FILE);
        String destination = SCREENSHOT_DIR + fileName + ".png";
        try {
            FileUtils.copyFile(source, new File(destination));
            log.info("Screenshot saved to: {}", destination);
        } catch (IOException e) {
            log.error("Failed to save screenshot '{}': {}", destination, e.getMessage(), e);
        }
        return destination;
    }
}
