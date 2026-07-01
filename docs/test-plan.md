# Manual Test Plan — Enterprise XML Order Processing System

**Project:** Enterprise XML Order Processing QA Suite  
**Version:** 1.0  
**Date:** 2026-06-30  
**Author:** QA Team  
**Application URL:** http://localhost:8080  

---

## 1. Scope

This test plan covers functional, boundary, negative, security, and database validation testing of the Enterprise XML Order Processing REST API and its web UI.

**In Scope**
- REST API endpoints (`POST /api/orders`, `GET /api/orders/{id}`, `GET /api/orders/{id}/invoice`)
- XSD schema validation logic
- XSLT invoice transformation
- PostgreSQL database record accuracy
- Web UI form submission and order lookup
- XXE injection prevention

**Out of Scope**
- Performance/load testing
- Authentication & authorisation (not yet implemented)
- Browser compatibility beyond Chrome

---

## 2. Test Environment

| Component | Details |
|-----------|---------|
| Application | Spring Boot 3.3.4, Java 21 |
| Database | PostgreSQL 16 via Docker (port 5433) |
| Tools | Postman, curl, pgAdmin/psql |
| Browser | Chrome (latest) |
| OS | macOS |

---

## 3. Test Cases

### 3.1 Valid / Happy Path

| ID | Title | Steps | Expected Result |
|----|-------|-------|-----------------|
| TC-M-001 | Submit single-item valid order | POST valid XML with 1 item to `/api/orders` | 201 Created, `<status>ACCEPTED</status>`, correct totalAmount |
| TC-M-002 | Submit multi-item valid order | POST valid XML with 3+ items | 201 Created, total = sum of (qty × unitPrice) for each item |
| TC-M-003 | Retrieve saved order | GET `/api/orders/{id}` after submitting | 200 OK, XML body matches submitted data |
| TC-M-004 | View HTML invoice | GET `/api/orders/{id}/invoice` | 200 OK, HTML page with styled table showing items and total |
| TC-M-005 | Submit via web UI | Open http://localhost:8080, fill form, click Submit | Success badge shown, invoice link appears |
| TC-M-006 | Look up order via web UI | Switch to Look Up tab, enter valid orderId, click Search | Order XML displayed, invoice link appears |

---

### 3.2 Validation Error Tests

| ID | Title | Steps | Expected Result |
|----|-------|-------|-----------------|
| TC-M-007 | Missing `<orderId>` | POST XML without `<orderId>` | 400 Bad Request, `<validationErrors>` body |
| TC-M-008 | Missing `<customerId>` | POST XML without `<customerId>` | 400, error mentions `customerId` |
| TC-M-009 | Missing `<orderDate>` | POST XML without `<orderDate>` | 400, validation error |
| TC-M-010 | Missing `<items>` block | POST XML with no `<items>` element | 400 |
| TC-M-011 | Empty `<items>` (no item children) | POST XML with `<items></items>` | 400 |
| TC-M-012 | Missing `<sku>` in item | POST XML with item missing `<sku>` | 400 |
| TC-M-013 | Missing `<description>` in item | POST XML with item missing `<description>` | 400 |
| TC-M-014 | Missing `<quantity>` in item | POST XML with item missing `<quantity>` | 400 |
| TC-M-015 | Missing `<unitPrice>` in item | POST XML with item missing `<unitPrice>` | 400 |

---

### 3.3 Data Type Tests

| ID | Title | Steps | Expected Result |
|----|-------|-------|-----------------|
| TC-M-016 | Text in `<quantity>` field | POST XML with `<quantity>TWO</quantity>` | 400, data type error |
| TC-M-017 | Text in `<unitPrice>` field | POST XML with `<unitPrice>nine-ninety-nine</unitPrice>` | 400 |
| TC-M-018 | Invalid date format (dd/MM/yyyy) | POST XML with `<orderDate>30/06/2026</orderDate>` | 400 |
| TC-M-019 | Invalid date format (plain text) | POST XML with `<orderDate>today</orderDate>` | 400 |

---

### 3.4 Boundary Value Tests

| ID | Title | Steps | Expected Result |
|----|-------|-------|-----------------|
| TC-M-020 | Quantity = 1 (minimum) | POST XML with `<quantity>1</quantity>` | 201 Accepted |
| TC-M-021 | Quantity = 10000 (maximum) | POST XML with `<quantity>10000</quantity>` | 201 Accepted |
| TC-M-022 | Quantity = 0 (below minimum) | POST XML with `<quantity>0</quantity>` | 400 |
| TC-M-023 | Quantity = 10001 (above maximum) | POST XML with `<quantity>10001</quantity>` | 400 |
| TC-M-024 | Unit price = 0.00 | POST XML with `<unitPrice>0.00</unitPrice>` | 201 Accepted (free item allowed) |
| TC-M-025 | Unit price negative | POST XML with `<unitPrice>-1.00</unitPrice>` | 400 |
| TC-M-026 | orderId at max length (40 chars) | POST XML with 40-char alphanumeric orderId | 201 Accepted |
| TC-M-027 | orderId at 41 chars (over max) | POST XML with 41-char orderId | 400 |
| TC-M-028 | Description at max length (200 chars) | POST XML with 200-char description | 201 Accepted |
| TC-M-029 | Description at 201 chars (over max) | POST XML with 201-char description | 400 |

---

### 3.5 Negative Tests

| ID | Title | Steps | Expected Result |
|----|-------|-------|-----------------|
| TC-M-030 | Duplicate order ID | POST same orderId twice | First: 201; Second: 409 Conflict |
| TC-M-031 | GET non-existent orderId | GET `/api/orders/GHOST-123` | 404 Not Found, XML error body |
| TC-M-032 | GET invoice for non-existent order | GET `/api/orders/GHOST-123/invoice` | 404 Not Found |
| TC-M-033 | Malformed XML (unclosed tag) | POST `<purchaseOrder><orderId>X` | 400, "Malformed XML" error message |
| TC-M-034 | Completely empty body | POST with empty body | 400 |
| TC-M-035 | JSON body instead of XML | POST with JSON `{"orderId":"X"}` | 400 |
| TC-M-036 | orderId with spaces | POST XML with `<orderId>PO 001</orderId>` | 400 (pattern violation) |
| TC-M-037 | orderId with special chars (!@#) | POST XML with `<orderId>PO!@#</orderId>` | 400 (pattern violation) |

---

### 3.6 Database Validation Tests

Run after submitting a successful order. Connect to Postgres and verify:

```sql
-- Verify order saved correctly
SELECT order_id, customer_id, order_date, total_amount
FROM purchase_orders
WHERE order_id = 'YOUR-ORDER-ID';

-- Verify items saved correctly
SELECT oi.sku, oi.description, oi.quantity, oi.unit_price
FROM order_items oi
JOIN purchase_orders po ON oi.order_id = po.order_id
WHERE po.order_id = 'YOUR-ORDER-ID';

-- Verify total matches sum of items
SELECT po.total_amount,
       SUM(oi.quantity * oi.unit_price) AS calculated_total
FROM purchase_orders po
JOIN order_items oi ON oi.order_id = po.order_id
WHERE po.order_id = 'YOUR-ORDER-ID'
GROUP BY po.total_amount;
```

| ID | Check | Pass Criteria |
|----|-------|---------------|
| TC-M-DB-001 | order_id stored correctly | Matches submitted orderId |
| TC-M-DB-002 | customer_id stored correctly | Matches submitted customerId |
| TC-M-DB-003 | order_date stored correctly | Matches submitted orderDate |
| TC-M-DB-004 | total_amount correct | Equals sum of (quantity × unit_price) across all items |
| TC-M-DB-005 | Item count matches | Number of rows in order_items equals items in request |
| TC-M-DB-006 | No orphan items on invalid order | Failed order leaves no rows in order_items or purchase_orders |

---

### 3.7 Security Tests

| ID | Title | Steps | Expected Result |
|----|-------|-------|-----------------|
| TC-M-SEC-001 | XXE injection via DOCTYPE | POST XML with DOCTYPE + entity referencing `/etc/passwd` | 400 (DOCTYPE disallowed) |
| TC-M-SEC-002 | Billion laughs (entity expansion DoS) | POST XML with nested entity expansions | 400 (DOCTYPE disallowed) |

---

### 3.8 Encoding & Special Character Tests

| ID | Title | Steps | Expected Result |
|----|-------|-------|-----------------|
| TC-M-ENC-001 | UTF-8 accented characters in description | POST XML with `Café & Crêpes` in description | 201 Accepted |
| TC-M-ENC-002 | XML entities in description (`&amp;`) | POST XML with `&amp;` in description | 201 Accepted, stored as `&` |
| TC-M-ENC-003 | Unicode characters | POST XML with Japanese characters in description | 201 Accepted |

---

### 3.9 Large Payload Test

| ID | Title | Steps | Expected Result |
|----|-------|-------|-----------------|
| TC-M-LG-001 | Order with 50 line items | POST XML with 50 `<item>` elements | 201 Accepted, all items stored |

---

## 4. Entry / Exit Criteria

**Entry Criteria** — Tests can begin when:
- Docker Postgres container is running on port 5433
- Spring Boot app is running on port 8080
- At least one successful `POST /api/orders` completes from the terminal

**Exit Criteria** — Testing is complete when:
- All TC-M-001 through TC-M-006 pass (happy path)
- At least 90% of validation/negative tests pass
- All DB validation queries return expected results
- Defects are logged in the defect report

---

## 5. Defect Classification

| Severity | Definition |
|----------|------------|
| Critical | App crashes, data loss, or security bypass |
| High | Wrong HTTP status code, data not saved to DB |
| Medium | Incorrect error message, missing validation |
| Low | UI cosmetic issue, minor message wording |
