
# Selenium Java Hybrid Automation Framework

![Java](https://img.shields.io/badge/Java-11+-orange?style=flat-square&logo=java)
![Selenium](https://img.shields.io/badge/Selenium-4.x-green?style=flat-square&logo=selenium)
![TestNG](https://img.shields.io/badge/TestNG-7.9-blue?style=flat-square)
![Cucumber](https://img.shields.io/badge/Cucumber-BDD-brightgreen?style=flat-square&logo=cucumber)
![Maven](https://img.shields.io/badge/Maven-Build-red?style=flat-square&logo=apachemaven)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-black?style=flat-square&logo=githubactions)

A production-grade hybrid test automation framework built with **Selenium WebDriver**, **Java**, **TestNG**, and **Cucumber BDD** — targeting [AutomationExercise.com](https://automationexercise.com). Architected from scratch with enterprise patterns: Page Object Model, ThreadLocal parallel execution, data-driven testing, and integrated reporting.

---

## Framework Architecture

```
selenium-java-hybrid-framework/
│
├── src/
│   ├── main/java/com/sdet/framework/
│   │   ├── base/
│   │   │   ├── BasePage.java           # Common WebDriver actions, wait strategies
│   │   │   └── BaseTest.java           # Test lifecycle hooks, setup & teardown
│   │   ├── factory/
│   │   │   └── DriverFactory.java      # ThreadLocal WebDriver — parallel-safe
│   │   ├── pages/
│   │   │   ├── HomePage.java
│   │   │   ├── LoginPage.java
│   │   │   ├── ProductPage.java
│   │   │   └── CheckoutPage.java
│   │   └── utils/
│   │       ├── ConfigReader.java       # Property file reader
│   │       ├── ExcelUtils.java         # Apache POI — data-driven testing
│   │       ├── ExtentReportManager.java
│   │       └── LogUtils.java           # Log4j2 structured logging
│   │
│   ├── main/resources/
│   │   ├── config.properties
│   │   └── log4j2.xml
│   │
│   └── test/
│       ├── java/com/sdet/tests/        # TestNG test classes
│       ├── java/com/sdet/stepdefs/     # Cucumber step definitions
│       └── resources/
│           ├── features/               # Gherkin feature files
│           └── testdata/               # Excel test data files
│
├── testng-suites/
│   ├── regression.xml
│   ├── smoke.xml
│   └── parallel.xml
│
├── reports/                            # ExtentReports HTML output
├── logs/                               # Log4j2 log files
├── .github/workflows/ci.yml            # GitHub Actions CI pipeline
└── pom.xml
```

---

## Key Features

| Feature | Implementation |
|---|---|
| **Page Object Model** | All page interactions encapsulated in dedicated page classes |
| **BDD with Cucumber** | Gherkin feature files with step definitions — business-readable scenarios |
| **Parallel Execution** | ThreadLocal WebDriver via DriverFactory — thread-safe, no session conflicts |
| **Data-Driven Testing** | Apache POI reads test data from Excel — no hardcoded test values |
| **Cross-Browser Support** | Chrome, Firefox, Edge — configured via `config.properties` |
| **HTML Reporting** | ExtentReports with step-level screenshots on failure |
| **Structured Logging** | Log4j2 — execution trace for every test step |
| **CI/CD Pipeline** | GitHub Actions — automated execution on every push and pull request |

---

## Tech Stack

- **Language:** Java 11+
- **Browser Automation:** Selenium WebDriver 4.x
- **Test Framework:** TestNG 7.9
- **BDD Layer:** Cucumber 7.x (Gherkin)
- **Build Tool:** Maven
- **Reporting:** ExtentReports 5.x
- **Logging:** Log4j2
- **Data Handling:** Apache POI (Excel)
- **CI/CD:** GitHub Actions
- **IDE:** VS Code

---

## Getting Started

### Prerequisites

- Java JDK 11+
- Maven 3.8+
- Chrome / Firefox / Edge browser installed

### Clone and Run

```bash
# Clone the repository
git clone https://github.com/Vimala-VS/selenium-java-hybrid-framework.git
cd selenium-java-hybrid-framework

# Install dependencies
mvn clean install -DskipTests

# Run smoke suite
mvn test -Dsurefire.suiteXmlFiles=testng-suites/smoke.xml

# Run full regression suite
mvn test -Dsurefire.suiteXmlFiles=testng-suites/regression.xml

# Run in parallel
mvn test -Dsurefire.suiteXmlFiles=testng-suites/parallel.xml
```

### Configuration

Edit `src/main/resources/config.properties` to switch browsers or environments:

```properties
browser=chrome
baseUrl=https://automationexercise.com
implicitWait=10
explicitWait=20
```

---

## Test Coverage

Automated scenarios cover the following workflows on AutomationExercise.com:

- **Authentication** — Login, logout, invalid credentials, session validation
- **Product Catalog** — Search, filter, product detail validation
- **Shopping Cart** — Add to cart, update quantity, remove items
- **Checkout Flow** — Address entry, order confirmation, payment steps
- **User Registration** — New account creation with field validation

---

## CI/CD Pipeline

GitHub Actions pipeline triggers on every push and pull request to `main`:

```yaml
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
```

Pipeline steps: checkout → Java setup → Maven build → Test execution → Report upload

View pipeline runs: [GitHub Actions](https://github.com/Vimala-VS/selenium-java-hybrid-framework/actions)

---

## Reports & Logs

After execution:
- **HTML Report:** `reports/ExtentReport.html` — open in browser for full test results with screenshots
- **Logs:** `logs/automation.log` — step-by-step execution trace via Log4j2

---

## Design Patterns

**Singleton via ThreadLocal** — DriverFactory uses ThreadLocal to manage WebDriver instances, ensuring each thread gets its own browser session during parallel execution. No shared state, no session conflicts.

**Factory Pattern** — Browser instantiation is centralized in DriverFactory. Adding a new browser requires a single change in one place.

**Page Object Model** — Each page maps to a dedicated class. Locators and actions are encapsulated — tests never interact with raw WebDriver calls.

**BaseTest** — Handles @BeforeMethod and @AfterMethod lifecycle. All test classes extend BaseTest, keeping setup and teardown DRY.

---

## Author

**Vimaladevi Siva**  
Senior SDET | 15+ years QA Automation | FinTech & Payments  
[LinkedIn](https://linkedin.com/in/vimaladevisiva) | testervimala@gmail.com
