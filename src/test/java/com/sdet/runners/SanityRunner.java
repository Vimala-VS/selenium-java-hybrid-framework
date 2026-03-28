package com.sdet.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * SanityRunner — Cucumber runner that executes only @sanity-tagged scenarios.
 *
 * WHY EXTEND AbstractTestNGCucumberTests?
 * ----------------------------------------
 * Cucumber needs a test runner to drive its engine. Two options exist:
 *   - JUnit runner (CucumberOptions + @RunWith(Cucumber.class))
 *   - TestNG runner (CucumberOptions + extends AbstractTestNGCucumberTests)
 *
 * This framework uses TestNG, so AbstractTestNGCucumberTests is the correct
 * base class. It provides a TestNG @Test method called feature() that
 * discovers feature files, maps them through the glue path, and runs each
 * matched Scenario as a separate TestNG data-driven test case. This means:
 *   - Scenarios appear individually in TestNG reports and IDE runners.
 *   - TestNG parallel execution settings in testng.xml apply to scenarios.
 *   - TestNG listeners (ExtentReports, Surefire) receive per-scenario events.
 *
 * If you extend nothing and just use @CucumberOptions, nothing runs — the
 * annotation is metadata only; it does not create a test entry point.
 *
 * HOW TAGS FILTER WHICH SCENARIOS RUN:
 * ---------------------------------------
 * Every scenario and feature in a .feature file can carry one or more tags
 * (e.g. @sanity, @regression). The tags option in @CucumberOptions is a
 * boolean expression evaluated against those tags:
 *
 *   tags = "@sanity"              → run scenarios tagged @sanity only
 *   tags = "@regression"          → run scenarios tagged @regression only
 *   tags = "@sanity or @smoke"    → run either tag
 *   tags = "@regression and not @wip" → regression but skip work-in-progress
 *
 * SanityRunner uses tags = "@sanity" to run only the quick happy-path
 * smoke checks — typically executed before a deployment or after a build
 * to confirm the application is fundamentally functional.
 *
 * WHAT dryRun DOES AND WHEN TO USE IT:
 * ---------------------------------------
 * dryRun = true tells Cucumber to parse all feature files and attempt to
 * match every step to a step definition — but NOT execute any step code.
 * Cucumber prints a list of every unmapped step and suggests snippet code
 * for each one.
 *
 * Use dryRun = true:
 *   - After writing a new feature file to instantly see which steps need
 *     step definition methods before writing any Java code.
 *   - After renaming step text to find broken bindings across the suite.
 *
 * Use dryRun = false (default, set here):
 *   - For all actual test runs — steps execute normally.
 *
 * WHAT monochrome DOES TO CONSOLE OUTPUT:
 * -----------------------------------------
 * monochrome = true strips ANSI colour codes from the console output.
 * Without it, terminals that do not support ANSI (older CI systems, Windows
 * CMD) print raw escape sequences like "[32mPassed[0m" instead of "Passed".
 * With it, the output is plain text readable in any terminal or log file.
 * Set to true for CI pipelines; either value works in colour-capable terminals.
 */
@CucumberOptions(
        // features: root directory (or specific paths) where Cucumber searches
        // recursively for .feature files. All .feature files under this path
        // are candidates — tag filtering then determines which scenarios run.
        features = "src/test/resources/features",

        // glue: one or more packages where Cucumber looks for step definition
        // classes (@Given/@When/@Then methods) and hook classes (@Before/@After).
        // Every class in the package is scanned — no explicit registration needed.
        glue = "com.sdet.stepdefinitions",

        // tags: boolean expression selecting which scenarios to include.
        // Only scenarios tagged @sanity will be picked up by this runner.
        tags = "@sanity",

        // plugin: list of output formatters. Each entry is "format:destination".
        //   pretty  — human-readable step-by-step output to the console.
        //   html    — self-contained HTML report at the given path.
        //   json    — machine-readable JSON for third-party report tools
        //             (e.g. Allure, Jenkins Cucumber plugin).
        plugin = {
                "pretty",
                "html:reports/cucumber-sanity-report.html",
                "json:reports/cucumber-sanity.json"
        },

        // monochrome: strips ANSI colour codes from console output so the
        // Cucumber step log is readable in plain-text CI build logs.
        monochrome = true,

        // dryRun: false → steps execute normally.
        // Flip to true temporarily to check for undefined steps without running tests.
        dryRun = false
)
public class SanityRunner extends AbstractTestNGCucumberTests {
    // No code needed. AbstractTestNGCucumberTests provides the @Test entry point.
    // @CucumberOptions above supplies all configuration.
}
