package com.sdet.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * RegressionRunner — Cucumber runner that executes all @regression-tagged scenarios.
 *
 * WHY TWO RUNNERS (SanityRunner + RegressionRunner)?
 * ---------------------------------------------------
 * Different stages of the delivery pipeline need different levels of coverage:
 *
 *   SanityRunner  (@sanity)     — fast, ~3-5 scenarios, runs on every commit or
 *                                 before/after a deployment. If sanity fails,
 *                                 there is no point running the full suite.
 *
 *   RegressionRunner (@regression) — full suite, runs nightly or before a
 *                                    release. Takes longer but covers every
 *                                    defined behaviour. @sanity scenarios are
 *                                    also tagged @regression, so they run here
 *                                    too — the full suite includes the smoke tests.
 *
 * Both runners point at the same features directory and glue path. The only
 * difference is the tags value. Adding a new scenario to a .feature file with
 * the correct tag automatically includes it in the right runner — no changes
 * to the runner classes are ever needed.
 *
 * HOW AbstractTestNGCucumberTests WIRES CUCUMBER INTO TESTNG:
 * -----------------------------------------------------------
 * The base class converts each Scenario into a row of a TestNG @DataProvider.
 * TestNG sees a parameterised @Test called feature(), runs it once per scenario,
 * and reports each run individually. This integration means:
 *   - Maven Surefire picks up the runner as a standard TestNG test class.
 *   - The testng.xml suite file can reference this class directly.
 *   - TestNG parallel="methods" or parallel="classes" in testng.xml applies
 *     to scenario execution, enabling parallel Cucumber runs without any
 *     additional configuration.
 *
 * HOW TAGS FILTER WHICH SCENARIOS RUN:
 * ---------------------------------------
 * tags = "@regression" matches any scenario that carries the @regression tag.
 * Because @sanity scenarios in the feature files are also tagged @regression,
 * they are included in this runner. The tag strategy therefore forms a hierarchy:
 *
 *   @sanity ⊂ @regression   (all sanity scenarios are also regression scenarios)
 *
 * Feature-level tags (@login, @product, @checkout) can be combined:
 *   tags = "@regression and @checkout"  → only checkout regression scenarios
 *   tags = "@regression and not @wip"   → skip work-in-progress scenarios
 *
 * WHAT dryRun DOES AND WHEN TO USE IT:
 * ---------------------------------------
 * dryRun = true: Cucumber matches steps to definitions and reports any missing
 * ones with suggested Java snippet code — but nothing actually runs. Useful for:
 *   - Previewing step definition gaps when a new feature file is written.
 *   - Quickly checking that a step rename didn't break bindings.
 *
 * dryRun = false (set here): all matched steps execute as normal.
 *
 * WHAT monochrome DOES TO CONSOLE OUTPUT:
 * -----------------------------------------
 * monochrome = true removes ANSI colour escape sequences from the "pretty"
 * plugin output. The coloured ✓/✗ display works in terminal emulators that
 * support ANSI, but CI systems that capture stdout to a log file often print
 * the raw codes (e.g. "[32m✓[0m"), making the log unreadable. Setting
 * monochrome = true ensures the output is clean plain text everywhere.
 */
@CucumberOptions(
        // features: root path scanned recursively for .feature files.
        features = "src/test/resources/features",

        // glue: package containing all @Before/@After hooks and step definition
        // classes. Cucumber instantiates and scans every class in this package.
        glue = "com.sdet.stepdefinitions",

        // tags: run every scenario tagged @regression.
        // Because @sanity scenarios are also tagged @regression, the full suite
        // includes the smoke scenarios as well.
        tags = "@regression",

        // plugin: output formatters.
        //   pretty  — console step-by-step output.
        //   html    — self-contained HTML report for the full regression run.
        //   json    — machine-readable output consumed by CI report plugins
        //             or Allure framework for enriched dashboards.
        plugin = {
                "pretty",
                "html:reports/cucumber-regression-report.html",
                "json:reports/cucumber-regression.json"
        },

        // monochrome: plain-text console output — safe for all terminals and CI logs.
        monochrome = true,

        // dryRun: false — execute all steps. Flip to true to audit step bindings.
        dryRun = false
)
public class RegressionRunner extends AbstractTestNGCucumberTests {
    // No code needed. AbstractTestNGCucumberTests provides the @Test entry point.
    // @CucumberOptions above supplies all configuration.
}
