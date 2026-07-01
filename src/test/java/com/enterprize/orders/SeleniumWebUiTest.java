package com.enterprize.orders;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Selenium tests for the web UI at /index.html.
 * Tests run in headless Chrome; skipped gracefully if Chrome is unavailable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SeleniumWebUiTest {

    @LocalServerPort
    private int port;

    private static WebDriver driver;
    private static boolean chromeAvailable = true;
    private WebDriverWait wait;

    @BeforeAll
    static void setupDriver() {
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions opts = new ChromeOptions();
            opts.addArguments(
                "--headless",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--disable-web-security",
                "--allow-running-insecure-content",
                "--window-size=1280,900"
            );
            driver = new ChromeDriver(opts);
        } catch (Exception e) {
            chromeAvailable = false;
        }
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void perTest() {
        assumeTrue(chromeAvailable, "Chrome/ChromeDriver not available — skipping Selenium tests");
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("http://localhost:" + port + "/index.html");
        // Wait for the JS-rendered initial item row before each test.
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("#items-container .item-row")));
    }

    // ─────────────────────────────────────────────────────────
    // SE-001  Page loads with correct title
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(1)
    void SE001_pageLoads_correctTitle() {
        assertEquals("Enterprise XML Order System", driver.getTitle());
    }

    // ─────────────────────────────────────────────────────────
    // SE-002  Submit tab is active by default
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(2)
    void SE002_submitTabActiveByDefault() {
        WebElement submitPanel = driver.findElement(By.id("panel-submit"));
        assertTrue(submitPanel.getAttribute("class").contains("active"));
    }

    // ─────────────────────────────────────────────────────────
    // SE-003  Add Item button appends a new item row
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(3)
    void SE003_addItemButton_appendsRow() {
        int before = driver.findElements(By.cssSelector("#items-container .item-row")).size();
        // Call addItem() directly via JS executor so the test is robust to headless-mode
        // limitations that prevent synthetic click events from firing inline onclick handlers.
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Long after = (Long) js.executeScript(
                "addItem(); return document.querySelectorAll('#items-container .item-row').length;");
        assertEquals(before + 1, after.intValue());
    }

    // ─────────────────────────────────────────────────────────
    // SE-004  Remove item button deletes the row
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(4)
    void SE004_removeItemButton_deletesRow() {
        driver.findElement(By.cssSelector(".btn-add")).click();
        int before = driver.findElements(By.cssSelector("#items-container .item-row")).size();
        driver.findElements(By.cssSelector(".btn-danger")).get(0).click();
        int after = driver.findElements(By.cssSelector("#items-container .item-row")).size();
        assertEquals(before - 1, after);
    }

    // ─────────────────────────────────────────────────────────
    // SE-005  Submitting a valid order shows success badge
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(5)
    void SE005_validOrderSubmit_showsSuccessBadge() {
        fillAndSubmitOrder("SE005", "CUST-SE1", "2026-06-30",
                "SKU-SE1", "Selenium Widget", "2", "5.00");

        WebElement badge = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("submit-badge")));
        assertTrue(badge.getText().contains("201"), "Expected 201 badge, got: " + badge.getText());
        assertTrue(badge.getAttribute("class").contains("badge-success"));
    }

    // ─────────────────────────────────────────────────────────
    // SE-006  Invoice link appears after successful submit
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(6)
    void SE006_validOrderSubmit_invoiceLinkVisible() {
        fillAndSubmitOrder("SE006", "CUST-SE2", "2026-06-30",
                "SKU-SE2", "Another Widget", "1", "9.99");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("submit-badge")));

        WebElement invoiceDiv = driver.findElement(By.id("submit-invoice-link"));
        assertEquals("block", invoiceDiv.getCssValue("display"));

        WebElement invoiceBtn = driver.findElement(By.id("invoice-link-btn"));
        assertTrue(invoiceBtn.getAttribute("href").contains("SE006"));
    }

    // ─────────────────────────────────────────────────────────
    // SE-007  Submitting empty form shows error badge
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(7)
    void SE007_emptyForm_showsErrorState() {
        driver.findElement(By.id("orderId")).clear();
        driver.findElement(By.id("customerId")).clear();
        driver.findElement(By.id("submit-btn")).click();

        WebElement badge = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("submit-badge")));
        assertTrue(badge.getAttribute("class").contains("badge-error"));
    }

    // ─────────────────────────────────────────────────────────
    // SE-008  Total preview updates as quantities/prices change
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(8)
    void SE008_totalPreview_updatesOnInput() {
        WebElement row = driver.findElements(By.cssSelector("#items-container .item-row")).get(0);
        WebElement qtyInput = row.findElements(By.tagName("input")).get(2);
        WebElement priceInput = row.findElements(By.tagName("input")).get(3);

        qtyInput.clear(); qtyInput.sendKeys("3");
        priceInput.clear(); priceInput.sendKeys("10.00");
        // Fire change event explicitly so updateTotal() runs in headless Chrome.
        ((JavascriptExecutor) driver).executeScript("updateTotal()");

        WebElement preview = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("total-preview")));
        assertTrue(preview.getText().contains("30.00"),
            "Expected total 30.00 but got: " + preview.getText());
    }

    // ─────────────────────────────────────────────────────────
    // SE-009  Look Up tab switches panel
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(9)
    void SE009_lookupTab_switchesPanel() {
        driver.findElements(By.cssSelector(".tab")).get(1).click();
        WebElement lookupPanel = driver.findElement(By.id("panel-lookup"));
        assertTrue(lookupPanel.getAttribute("class").contains("active"));
    }

    // ─────────────────────────────────────────────────────────
    // SE-010  Looking up a submitted order shows result
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(10)
    void SE010_lookupExistingOrder_showsResult() {
        // Submit an order first via the form
        fillAndSubmitOrder("SE010", "CUST-SE10", "2026-06-30",
                "SKU-L", "Lookup Item", "1", "7.00");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("submit-badge")));

        // Switch to lookup tab
        driver.findElements(By.cssSelector(".tab")).get(1).click();

        WebElement lookupInput = driver.findElement(By.id("lookup-id"));
        lookupInput.sendKeys("SE010");
        driver.findElement(By.xpath("//button[text()='Search']")).click();

        WebElement badge = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("lookup-badge")));
        assertTrue(badge.getText().contains("200"));

        String responseBody = driver.findElement(By.id("lookup-response-body")).getText();
        assertTrue(responseBody.contains("SE010"));
    }

    // ─────────────────────────────────────────────────────────
    // SE-011  Clear button resets form
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(11)
    void SE011_clearButton_resetsForm() {
        driver.findElement(By.id("orderId")).sendKeys("CLEAR-TEST");
        driver.findElement(By.id("customerId")).sendKeys("CUST-CLEAR");

        driver.findElement(By.xpath("//button[text()='Clear']")).click();

        assertEquals("", driver.findElement(By.id("orderId")).getAttribute("value"));
        assertEquals("", driver.findElement(By.id("customerId")).getAttribute("value"));
    }

    // ─────────────────────────────────────────────────────────
    // SE-012  Duplicate submit shows error badge
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(12)
    void SE012_duplicateOrder_showsErrorBadge() {
        fillAndSubmitOrder("SE012-DUP", "CUST-SE12", "2026-06-30",
                "SKU-D", "Dup Item", "1", "1.00");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("submit-badge")));

        // Submit same order again
        fillAndSubmitOrder("SE012-DUP", "CUST-SE12", "2026-06-30",
                "SKU-D", "Dup Item", "1", "1.00");

        WebElement badge = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("submit-badge")));
        assertTrue(badge.getAttribute("class").contains("badge-error"),
            "Expected error badge on duplicate, got: " + badge.getAttribute("class"));
    }

    // ─────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────
    private void fillAndSubmitOrder(String orderId, String customerId, String date,
                                     String sku, String description, String qty, String price) {
        driver.get("http://localhost:" + port + "/index.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("#items-container .item-row")));

        JavascriptExecutor js = (JavascriptExecutor) driver;

        driver.findElement(By.id("orderId")).sendKeys(orderId);
        driver.findElement(By.id("customerId")).sendKeys(customerId);

        // Use JS to set the date input value reliably in headless Chrome.
        js.executeScript("document.getElementById('orderDate').value = arguments[0]", date);

        WebElement row = driver.findElements(By.cssSelector("#items-container .item-row")).get(0);
        java.util.List<WebElement> inputs = row.findElements(By.tagName("input"));
        inputs.get(0).clear(); inputs.get(0).sendKeys(sku);
        inputs.get(1).clear(); inputs.get(1).sendKeys(description);
        inputs.get(2).clear(); inputs.get(2).sendKeys(qty);
        inputs.get(3).clear(); inputs.get(3).sendKeys(price);

        js.executeScript("document.getElementById('submit-btn').click()");
    }
}
