package com.enterprize.orders;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * REST Assured API test suite covering valid, invalid, boundary, negative,
 * and encoding scenarios as defined in the QA test plan.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RestAssuredOrderTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
    }

    // ─────────────────────────────────────────────────────────
    // TC-001  Valid order — happy path
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(1)
    void TC001_validOrder_returns201AndAccepted() {
        given()
            .contentType(ContentType.XML)
            .body(validOrder("TC001", "CUST-1", "2026-06-30",
                    item("SKU-A", "Widget", 2, "5.00")))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .contentType(ContentType.XML)
            .body("orderConfirmation.status", equalTo("ACCEPTED"))
            .body("orderConfirmation.orderId", equalTo("TC001"))
            .body("orderConfirmation.totalAmount", equalTo("10.00"));
    }

    // ─────────────────────────────────────────────────────────
    // TC-002  Retrieve order just submitted
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(2)
    void TC002_getSubmittedOrder_returns200() {
        given()
            .contentType(ContentType.XML)
            .body(validOrder("TC002", "CUST-2", "2026-01-15",
                    item("SKU-B", "Gadget", 1, "99.99")))
        .when()
            .post("/api/orders");

        given()
        .when()
            .get("/api/orders/TC002")
        .then()
            .statusCode(200)
            .body("orderConfirmation.customerId", equalTo("CUST-2"))
            .body("orderConfirmation.totalAmount", equalTo("99.99"));
    }

    // ─────────────────────────────────────────────────────────
    // TC-003  HTML invoice endpoint
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(3)
    void TC003_invoiceEndpoint_returnsHtml() {
        given()
            .contentType(ContentType.XML)
            .body(validOrder("TC003", "CUST-3", "2026-03-10",
                    item("SKU-C", "Doohickey", 3, "4.00")))
        .when()
            .post("/api/orders");

        given()
        .when()
            .get("/api/orders/TC003/invoice")
        .then()
            .statusCode(200)
            .contentType("text/html")
            .body(containsString("Invoice for Order TC003"))
            .body(containsString("12.00"));
    }

    // ─────────────────────────────────────────────────────────
    // TC-004  Multiple line items — total computed correctly
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(4)
    void TC004_multipleItems_totalIsCorrect() {
        String items = item("SKU-1", "Alpha", 2, "10.00")
                     + item("SKU-2", "Beta",  3, "5.00")
                     + item("SKU-3", "Gamma", 1, "2.50");

        given()
            .contentType(ContentType.XML)
            .body(validOrder("TC004", "CUST-4", "2026-06-01", items))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .body("orderConfirmation.totalAmount", equalTo("37.50")); // 20+15+2.50
    }

    // ─────────────────────────────────────────────────────────
    // TC-005  Duplicate order ID → 409
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(5)
    void TC005_duplicateOrderId_returns409() {
        String body = validOrder("TC005-DUP", "CUST-5", "2026-06-30",
                item("SKU-Z", "Thing", 1, "1.00"));

        given().contentType(ContentType.XML).body(body).post("/api/orders");

        given()
            .contentType(ContentType.XML)
            .body(body)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(409)
            .body("validationErrors.error", containsString("TC005-DUP"));
    }

    // ─────────────────────────────────────────────────────────
    // TC-006  Missing <customerId> → 400 with field-level error
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(6)
    void TC006_missingCustomerId_returns400WithDetail() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <purchaseOrder>
                <orderId>TC006</orderId>
                <orderDate>2026-06-30</orderDate>
                <items><item>
                    <sku>SKU-1</sku><description>X</description>
                    <quantity>1</quantity><unitPrice>1.00</unitPrice>
                </item></items>
            </purchaseOrder>""";

        given()
            .contentType(ContentType.XML)
            .body(xml)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400)
            .body("validationErrors.error[0]", containsString("customerId"));
    }

    // ─────────────────────────────────────────────────────────
    // TC-007  Missing <items> block → 400
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(7)
    void TC007_missingItems_returns400() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <purchaseOrder>
                <orderId>TC007</orderId>
                <customerId>CUST-7</customerId>
                <orderDate>2026-06-30</orderDate>
            </purchaseOrder>""";

        given()
            .contentType(ContentType.XML)
            .body(xml)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400)
            .body("validationErrors.error[0]", notNullValue());
    }

    // ─────────────────────────────────────────────────────────
    // TC-008  Missing <orderId> → 400
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(8)
    void TC008_missingOrderId_returns400() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <purchaseOrder>
                <customerId>CUST-8</customerId>
                <orderDate>2026-06-30</orderDate>
                <items><item>
                    <sku>SKU-1</sku><description>X</description>
                    <quantity>1</quantity><unitPrice>1.00</unitPrice>
                </item></items>
            </purchaseOrder>""";

        given()
            .contentType(ContentType.XML)
            .body(xml)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    // ─────────────────────────────────────────────────────────
    // TC-009  Invalid date format → 400
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(9)
    void TC009_invalidDateFormat_returns400() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <purchaseOrder>
                <orderId>TC009</orderId>
                <customerId>CUST-9</customerId>
                <orderDate>30/06/2026</orderDate>
                <items><item>
                    <sku>SKU-1</sku><description>X</description>
                    <quantity>1</quantity><unitPrice>1.00</unitPrice>
                </item></items>
            </purchaseOrder>""";

        given()
            .contentType(ContentType.XML)
            .body(xml)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    // ─────────────────────────────────────────────────────────
    // TC-010  Text in quantity field (wrong data type) → 400
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(10)
    void TC010_textInQuantity_returns400() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <purchaseOrder>
                <orderId>TC010</orderId>
                <customerId>CUST-10</customerId>
                <orderDate>2026-06-30</orderDate>
                <items><item>
                    <sku>SKU-1</sku><description>Widget</description>
                    <quantity>TWO</quantity><unitPrice>1.00</unitPrice>
                </item></items>
            </purchaseOrder>""";

        given()
            .contentType(ContentType.XML)
            .body(xml)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    // ─────────────────────────────────────────────────────────
    // TC-011  Negative unit price → 400
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(11)
    void TC011_negativePriceRejected_returns400() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <purchaseOrder>
                <orderId>TC011</orderId>
                <customerId>CUST-11</customerId>
                <orderDate>2026-06-30</orderDate>
                <items><item>
                    <sku>SKU-1</sku><description>Negative</description>
                    <quantity>1</quantity><unitPrice>-5.00</unitPrice>
                </item></items>
            </purchaseOrder>""";

        given()
            .contentType(ContentType.XML)
            .body(xml)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    // ─────────────────────────────────────────────────────────
    // TC-012  Boundary — quantity = 1 (minimum allowed)
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(12)
    void TC012_boundary_minimumQuantity1_accepted() {
        given()
            .contentType(ContentType.XML)
            .body(validOrder("TC012", "CUST-12", "2026-06-30",
                    item("SKU-1", "Min Qty", 1, "1.00")))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);
    }

    // ─────────────────────────────────────────────────────────
    // TC-013  Boundary — quantity = 10000 (maximum allowed)
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(13)
    void TC013_boundary_maximumQuantity10000_accepted() {
        given()
            .contentType(ContentType.XML)
            .body(validOrder("TC013", "CUST-13", "2026-06-30",
                    item("SKU-1", "Max Qty", 10000, "0.01")))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);
    }

    // ─────────────────────────────────────────────────────────
    // TC-014  Boundary — quantity = 10001 (exceeds maximum) → 400
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(14)
    void TC014_boundary_quantity10001_rejected() {
        given()
            .contentType(ContentType.XML)
            .body(validOrder("TC014", "CUST-14", "2026-06-30",
                    item("SKU-1", "Over Max", 10001, "1.00")))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    // ─────────────────────────────────────────────────────────
    // TC-015  Boundary — quantity = 0 (below minimum) → 400
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(15)
    void TC015_boundary_quantity0_rejected() {
        given()
            .contentType(ContentType.XML)
            .body(validOrder("TC015", "CUST-15", "2026-06-30",
                    item("SKU-1", "Zero Qty", 0, "1.00")))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    // ─────────────────────────────────────────────────────────
    // TC-016  Malformed XML (unclosed tag) → 400
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(16)
    void TC016_malformedXml_returns400() {
        given()
            .contentType(ContentType.XML)
            .body("<purchaseOrder><orderId>TC016</orderId>")
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400)
            .body("validationErrors.error[0]", containsString("Malformed XML"));
    }

    // ─────────────────────────────────────────────────────────
    // TC-017  Empty body → 400
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(17)
    void TC017_emptyBody_returns400() {
        given()
            .contentType(ContentType.XML)
            .body("")
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    // ─────────────────────────────────────────────────────────
    // TC-018  Special characters in description
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(18)
    void TC018_specialCharactersInDescription_accepted() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <purchaseOrder>
                <orderId>TC018</orderId>
                <customerId>CUST-18</customerId>
                <orderDate>2026-06-30</orderDate>
                <items><item>
                    <sku>SKU-SPEC</sku>
                    <description>Caf&#233; &amp; Co. Item #1</description>
                    <quantity>1</quantity><unitPrice>3.50</unitPrice>
                </item></items>
            </purchaseOrder>""";

        given()
            .contentType(ContentType.XML)
            .body(xml)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);
    }

    // ─────────────────────────────────────────────────────────
    // TC-019  XXE injection attempt → 400 (blocked)
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(19)
    void TC019_xxeInjectionBlocked_returns400() {
        String xxeXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <purchaseOrder>
                <orderId>&xxe;</orderId>
                <customerId>CUST-19</customerId>
                <orderDate>2026-06-30</orderDate>
                <items><item>
                    <sku>SKU-1</sku><description>X</description>
                    <quantity>1</quantity><unitPrice>1.00</unitPrice>
                </item></items>
            </purchaseOrder>""";

        given()
            .contentType(ContentType.XML)
            .body(xxeXml)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    // ─────────────────────────────────────────────────────────
    // TC-020  Non-existent order ID → 404
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(20)
    void TC020_unknownOrderId_returns404() {
        given()
        .when()
            .get("/api/orders/DOES-NOT-EXIST")
        .then()
            .statusCode(404)
            .body("validationErrors.error[0]", containsString("DOES-NOT-EXIST"));
    }

    // ─────────────────────────────────────────────────────────
    // TC-021  Invoice for non-existent order → 404
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(21)
    void TC021_invoiceForUnknownOrder_returns404() {
        given()
        .when()
            .get("/api/orders/NO-SUCH-ORDER/invoice")
        .then()
            .statusCode(404);
    }

    // ─────────────────────────────────────────────────────────
    // TC-022  Large order — 50 line items
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(22)
    void TC022_largeOrder50Items_accepted() {
        StringBuilder items = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            items.append(item("SKU-" + i, "Product number " + i, i, "1.00"));
        }

        given()
            .contentType(ContentType.XML)
            .body(validOrder("TC022", "CUST-22", "2026-06-30", items.toString()))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);
    }

    // ─────────────────────────────────────────────────────────
    // TC-023  orderId with special pattern characters → 400
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(23)
    void TC023_invalidOrderIdPattern_returns400() {
        given()
            .contentType(ContentType.XML)
            .body(validOrder("INVALID ORDER ID!", "CUST-23", "2026-06-30",
                    item("SKU-1", "Widget", 1, "1.00")))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    // ─────────────────────────────────────────────────────────
    // TC-024  Database record matches submitted order (data integrity)
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(24)
    void TC024_databaseRecordMatchesSubmittedOrder() {
        String orderId = "TC024";
        String customerId = "CUST-24";
        String date = "2026-05-15";

        given()
            .contentType(ContentType.XML)
            .body(validOrder(orderId, customerId, date,
                    item("SKU-DB", "DB Test Item", 7, "3.00")))
        .when()
            .post("/api/orders");

        given()
        .when()
            .get("/api/orders/" + orderId)
        .then()
            .statusCode(200)
            .body("orderConfirmation.orderId", equalTo(orderId))
            .body("orderConfirmation.customerId", equalTo(customerId))
            .body("orderConfirmation.orderDate", equalTo(date))
            .body("orderConfirmation.totalAmount", equalTo("21.00")); // 7 * 3.00
    }

    // ─────────────────────────────────────────────────────────
    // TC-025  Zero unit price (boundary — 0.00 is allowed by XSD)
    // ─────────────────────────────────────────────────────────
    @Test
    @Order(25)
    void TC025_boundary_zeroPriceAllowed() {
        given()
            .contentType(ContentType.XML)
            .body(validOrder("TC025", "CUST-25", "2026-06-30",
                    item("SKU-FREE", "Free Item", 1, "0.00")))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .body("orderConfirmation.totalAmount", equalTo("0.00"));
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────
    private String validOrder(String orderId, String customerId, String date, String itemsXml) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <purchaseOrder>
                <orderId>%s</orderId>
                <customerId>%s</customerId>
                <orderDate>%s</orderDate>
                <items>%s</items>
            </purchaseOrder>""".formatted(orderId, customerId, date, itemsXml);
    }

    private String item(String sku, String description, int qty, String price) {
        return """
            <item>
                <sku>%s</sku><description>%s</description>
                <quantity>%d</quantity><unitPrice>%s</unitPrice>
            </item>""".formatted(sku, description, qty, price);
    }
}
