package com.sdet.framework.utils;

import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * ExcelUtil — Apache POI utility for reading test data from .xlsx workbooks.
 *
 * WHY EXCEL INSTEAD OF HARDCODED TEST DATA?
 * ------------------------------------------
 * Hardcoding test data in test classes creates three problems:
 *
 *   1. MAINTENANCE — when data changes (e.g. a password policy update), you
 *      hunt through Java files to find and update every affected literal.
 *      With Excel, the QA lead updates one file and all tests pick up the change
 *      automatically on the next run.
 *
 *   2. SEPARATION OF CONCERNS — test logic (what to do) and test data (what to
 *      use) are different responsibilities. Mixing them in the same Java class
 *      makes both harder to read, review, and maintain.
 *
 *   3. NON-TECHNICAL COLLABORATION — product owners and manual testers can
 *      contribute or verify test data in Excel without touching Java code.
 *      A developer hands off testdata.xlsx; business stakeholders populate rows.
 *
 * HOW Object[][] WORKS WITH TestNG DataProvider:
 * -----------------------------------------------
 * TestNG's @DataProvider annotation expects a method that returns Object[][].
 * The outer array is the list of test runs; the inner array is the list of
 * parameters for one run. TestNG calls the @Test method once per outer row,
 * injecting the inner array values as method arguments in order.
 *
 * Example mapping for LoginData:
 *
 *   Excel sheet "LoginData":
 *   ┌──────────────────────┬───────────┬───────────────┐
 *   │ email                │ password  │ expectedResult│  ← row 0 (header, skipped)
 *   ├──────────────────────┼───────────┼───────────────┤
 *   │ valid@email.com      │ Valid@123 │ success       │  ← row 1 → data[0]
 *   │ invalid@email.com    │ wrong     │ failure       │  ← row 2 → data[1]
 *   └──────────────────────┴───────────┴───────────────┘
 *
 *   getTestData("LoginData") returns:
 *     {
 *       { "valid@email.com",   "Valid@123", "success" },   // data[0]
 *       { "invalid@email.com", "wrong",     "failure" }    // data[1]
 *     }
 *
 *   TestNG DataProvider usage in test class:
 *
 *     @DataProvider(name = "loginData")
 *     public Object[][] loginDataProvider() {
 *         ExcelUtil excel = new ExcelUtil(ConfigReader.getInstance().getTestDataPath());
 *         return excel.getTestData("LoginData");
 *     }
 *
 *     @Test(dataProvider = "loginData")
 *     public void testLogin(String email, String password, String expectedResult) {
 *         // TestNG calls this twice:
 *         //   call 1: email="valid@email.com",   password="Valid@123", expectedResult="success"
 *         //   call 2: email="invalid@email.com", password="wrong",     expectedResult="failure"
 *     }
 *
 * TESTDATA.XLSX SHEET STRUCTURE (create this file manually in Excel or LibreOffice):
 * -----------------------------------------------------------------------------------
 * Sheet name: LoginData
 * ┌──────────────────────┬───────────┬───────────────┐
 * │ email                │ password  │ expectedResult│
 * ├──────────────────────┼───────────┼───────────────┤
 * │ valid@email.com      │ Valid@123 │ success       │
 * │ invalid@email.com    │ wrong     │ failure       │
 * └──────────────────────┴───────────┴───────────────┘
 * Row 0 is the header row and is automatically skipped by getTestData().
 * Add additional sheets for other test modules (e.g. ProductData, UserData).
 */
public class ExcelUtil {

    private static final Logger log = LogManager.getLogger(ExcelUtil.class);

    // The workbook loaded from the .xlsx file — kept open for the lifetime of
    // this ExcelUtil instance to avoid reopening the file on every read call.
    private final Workbook workbook;

    // Path stored for meaningful error messages when a sheet or cell is missing.
    private final String filePath;

    // -------------------------------------------------------------------------
    // Constructor — opens the .xlsx file and loads it into an XSSFWorkbook.
    // Throws RuntimeException immediately if the file does not exist or cannot
    // be read, so misconfiguration surfaces at suite startup rather than mid-run.
    // -------------------------------------------------------------------------
    public ExcelUtil(String filePath) {
        this.filePath = filePath;
        log.info("Opening Excel workbook: {}", filePath);
        try (FileInputStream fis = new FileInputStream(filePath)) {
            workbook = new XSSFWorkbook(fis);
            log.info("Workbook loaded successfully. Sheet count: {}", workbook.getNumberOfSheets());
        } catch (IOException e) {
            log.error("Failed to open Excel file: {}", filePath, e);
            throw new RuntimeException(
                    "Could not open Excel file at: " + filePath +
                    ". Ensure the file exists and is a valid .xlsx workbook.", e);
        }
    }

    // -------------------------------------------------------------------------
    // isSheetExists(sheetName) — returns true if the workbook contains a sheet
    // with the given name. Use this as a guard before calling getRowCount() or
    // getCellData() to produce a clear error rather than a NullPointerException.
    // -------------------------------------------------------------------------
    public boolean isSheetExists(String sheetName) {
        boolean exists = workbook.getSheet(sheetName) != null;
        log.debug("Sheet '{}' exists: {}", sheetName, exists);
        return exists;
    }

    // -------------------------------------------------------------------------
    // getRowCount(sheetName) — returns the number of populated rows in the
    // sheet, including the header row. Subtract 1 to get the data row count.
    //
    // Uses getLastRowNum() + 1 because getLastRowNum() is zero-indexed and
    // returns -1 for an empty sheet.
    // -------------------------------------------------------------------------
    public int getRowCount(String sheetName) {
        Sheet sheet = getSheet(sheetName);
        int rowCount = sheet.getLastRowNum() + 1;
        log.debug("Row count in sheet '{}': {} (includes header)", sheetName, rowCount);
        return rowCount;
    }

    // -------------------------------------------------------------------------
    // getCellData(sheetName, row, col) — returns the string value of a single
    // cell. Row and col are zero-indexed.
    //
    // All cell types are converted to String using DataFormatter so numeric
    // cells (e.g. a card number stored as a number) are not returned in
    // scientific notation ("1.23457E+15") the way cell.getNumericCellValue()
    // would produce.
    //
    // Returns an empty string for blank cells rather than null, so callers
    // never need null checks on the return value.
    // -------------------------------------------------------------------------
    public String getCellData(String sheetName, int row, int col) {
        Sheet sheet = getSheet(sheetName);
        Row excelRow = sheet.getRow(row);

        if (excelRow == null) {
            log.warn("Row {} is null in sheet '{}' — returning empty string.", row, sheetName);
            return "";
        }

        Cell cell = excelRow.getCell(col);
        if (cell == null) {
            log.warn("Cell [{},{}] is null in sheet '{}' — returning empty string.", row, col, sheetName);
            return "";
        }

        // DataFormatter renders the cell exactly as Excel displays it,
        // regardless of the underlying cell type (String, Numeric, Boolean, Formula).
        DataFormatter formatter = new DataFormatter();
        String value = formatter.formatCellValue(cell).trim();
        log.debug("Cell [{},{}] in sheet '{}': '{}'", row, col, sheetName, value);
        return value;
    }

    // -------------------------------------------------------------------------
    // getTestData(sheetName) — reads all data rows (skipping row 0 header) and
    // returns them as Object[][] for use with a TestNG @DataProvider.
    //
    // The number of columns is determined from the header row (row 0) so the
    // array dimensions are derived automatically from the sheet structure.
    // Adding a new column to the Excel sheet is the only change needed to pass
    // an additional parameter to the @Test method — no Java changes required.
    // -------------------------------------------------------------------------
    public Object[][] getTestData(String sheetName) {
        log.info("Loading test data from sheet: '{}'", sheetName);

        int totalRows = getRowCount(sheetName);
        int dataRows  = totalRows - 1;          // exclude header row

        if (dataRows <= 0) {
            log.warn("Sheet '{}' has no data rows. Returning empty Object[][].", sheetName);
            return new Object[0][0];
        }

        // Determine column count from the header row.
        Sheet sheet = getSheet(sheetName);
        int totalCols = sheet.getRow(0).getLastCellNum();

        log.debug("Sheet '{}' — data rows: {}, columns: {}", sheetName, dataRows, totalCols);

        Object[][] data = new Object[dataRows][totalCols];

        // Start at row index 1 to skip the header row.
        for (int rowIndex = 1; rowIndex <= dataRows; rowIndex++) {
            for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                data[rowIndex - 1][colIndex] = getCellData(sheetName, rowIndex, colIndex);
            }
        }

        log.info("Test data loaded from sheet '{}' — {} test case(s) found.", sheetName, dataRows);
        return data;
    }

    // -------------------------------------------------------------------------
    // getSheet(sheetName) — internal helper that retrieves the Sheet object or
    // throws a descriptive RuntimeException if the sheet does not exist.
    // Called by every public method that needs a sheet reference, keeping the
    // null-check in one place rather than repeated across multiple methods.
    // -------------------------------------------------------------------------
    private Sheet getSheet(String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            String msg = String.format(
                    "Sheet '%s' not found in workbook: %s. " +
                    "Available sheets: %s",
                    sheetName, filePath, getAvailableSheetNames());
            log.error(msg);
            throw new RuntimeException(msg);
        }
        return sheet;
    }

    // -------------------------------------------------------------------------
    // getAvailableSheetNames() — builds a comma-separated list of all sheet
    // names in the workbook. Used in error messages so the developer can see
    // exactly what sheets are present when a name mismatch occurs.
    // -------------------------------------------------------------------------
    private String getAvailableSheetNames() {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (i > 0) names.append(", ");
            names.append(workbook.getSheetName(i));
        }
        return names.toString();
    }
}
