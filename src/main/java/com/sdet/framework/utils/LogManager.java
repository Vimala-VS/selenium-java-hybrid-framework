package com.sdet.framework.utils;

import org.apache.logging.log4j.Logger;

/**
 * LogManager — a thin wrapper around Log4j2 that gives every framework class
 * a consistent, single-line way to obtain a Logger.
 *
 * WHY a wrapper?
 * --------------
 * Rather than each class importing and calling org.apache.logging.log4j.LogManager
 * directly, all classes go through this utility. If the underlying logging library
 * ever changes (e.g. from Log4j2 to SLF4J), only this file needs to be updated —
 * every other class stays untouched.
 *
 * NAMING NOTE:
 * ------------
 * This class is intentionally named LogManager to provide a clean, memorable API
 * (LogManager.getLogger(...)). It lives in com.sdet.framework.utils, so it does
 * not conflict with org.apache.logging.log4j.LogManager as long as this package
 * is used for imports inside the framework. If both are ever needed in the same
 * file, use the fully-qualified name for the Log4j2 one.
 *
 * EXAMPLE USAGE — LoginPage:
 * --------------------------
 *
 *   import com.sdet.framework.utils.LogManager;
 *   import org.apache.logging.log4j.Logger;
 *
 *   public class LoginPage {
 *
 *       private static final Logger log = LogManager.getLogger(LoginPage.class);
 *
 *       public void enterEmail(String email) {
 *           log.info("Entering email: {}", email);
 *           emailField.sendKeys(email);
 *       }
 *
 *       public void clickLogin() {
 *           log.info("Clicking the Login button.");
 *           loginButton.click();
 *       }
 *
 *       public void loginWithInvalidCredentials(String email, String password) {
 *           log.warn("Attempting login with invalid credentials — email: {}", email);
 *           enterEmail(email);
 *           enterPassword(password);
 *           clickLogin();
 *       }
 *   }
 */
public class LogManager {

    // Prevent instantiation — this class is a static utility only.
    private LogManager() {}

    /**
     * getLogger(clazz) — returns a Log4j2 Logger named after the given class.
     *
     * The class name becomes the logger name, which Log4j2 uses to apply the
     * correct log level and appender rules from log4j2.xml. Passing the actual
     * Class object (rather than a string) means the logger name automatically
     * tracks refactoring/renames without any manual update.
     *
     * @param clazz  the class requesting a logger (e.g. LoginPage.class)
     * @return       a Logger instance scoped to that class
     *
     * Usage:
     *   private static final Logger log = LogManager.getLogger(MyClass.class);
     */
    public static Logger getLogger(Class<?> clazz) {
        return org.apache.logging.log4j.LogManager.getLogger(clazz);
    }
}
