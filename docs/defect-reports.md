# Defect Reports — Enterprise XML Order Processing System

**Project:** Enterprise XML Order Processing QA Suite  
**Version:** 1.0  
**Date:** 2026-06-30  

---

## DR-001 — Native Postgres on Port 5432 Intercepts Docker Connection

| Field | Details |
|-------|---------|
| **ID** | DR-001 |
| **Title** | App connects to native Homebrew Postgres instead of Docker container |
| **Severity** | High |
| **Status** | Fixed |
| **Found by** | Environment smoke test |
| **Date Found** | 2026-06-30 |

**Description**  
On a machine with both Homebrew `postgresql@18` (running on 5432) and the Docker Postgres container (also configured to expose on 5432), the Spring Boot app always connects to the native Postgres. That instance has no `orders` role or database, so startup fails with `FATAL: role "orders" does not exist`.

**Steps to Reproduce**
1. Start Homebrew Postgres (`brew services start postgresql@18`).
2. Run `docker compose up -d` (container exposes port 5432).
3. Start the app with `mvn spring-boot:run`.
4. Observe: `FATAL: role "orders" does not exist`.

**Root Cause**  
macOS routes `localhost:5432` connections to the more specific `127.0.0.1:5432` binding owned by the native Postgres process. Docker's user-space proxy listens on `0.0.0.0:5432` but loses the connection race.

**Fix Applied**  
Changed `docker-compose.yml` port mapping from `5432:5432` → `5433:5432` and updated `application.yml` JDBC URL to `jdbc:postgresql://localhost:5433/orders`.

---

## DR-002 — JAXB Cannot Bind `java.time.LocalDate` Directly

| Field | Details |
|-------|---------|
| **ID** | DR-002 |
| **Title** | `NoSuchMethodError: java.time.LocalDate.<init>()` during JAXB unmarshal |
| **Severity** | Critical |
| **Status** | Fixed |
| **Found by** | JUnit smoke test (`acceptsValidOrderAndPersistsIt`) |
| **Date Found** | 2026-06-30 |

**Description**  
JAXB's `ClassFactory` tries to instantiate bound types via their no-arg constructor. `java.time.LocalDate` has no public no-arg constructor, so the unmarshal step throws `NoSuchMethodException` at runtime even though the code compiles cleanly.

**Steps to Reproduce**
1. Declare a `LocalDate` field in a JAXB-annotated class without an `XmlAdapter`.
2. Send a valid XML purchase order to `POST /api/orders`.
3. Observe: `java.lang.NoSuchMethodError: java.time.LocalDate.<init>()`.

**Root Cause**  
JAXB does not natively support JSR-310 (`java.time`) types. It requires an explicit `XmlAdapter<String, LocalDate>` to handle string-to-date conversion.

**Fix Applied**  
Created `LocalDateAdapter extends XmlAdapter<String, LocalDate>` and annotated both `PurchaseOrderXml.orderDate` and `OrderResponseXml.orderDate` with `@XmlJavaTypeAdapter(LocalDateAdapter.class)`.

---

## DR-003 — XPath 1.0 Cannot Compute Sum of Per-Row Products in XSLT

| Field | Details |
|-------|---------|
| **ID** | DR-003 |
| **Title** | `sum(items/item/(quantity * unitPrice))` is not valid XPath 1.0 |
| **Severity** | Medium |
| **Status** | Fixed |
| **Found by** | Code review / XSLT design |
| **Date Found** | 2026-06-30 |

**Description**  
The initial XSLT invoice template attempted to compute the order total inline using `format-number(sum(items/item/(quantity * unitPrice)), '0.00')`. XPath 1.0 (the version used by Java's built-in `javax.xml.transform`) does not support path expressions that include arithmetic operations inside `sum()`. This would produce `NaN` or throw a transformer error at runtime.

**Steps to Reproduce**
1. Use the XSLT expression `sum(items/item/(quantity * unitPrice))` in an XPath 1.0 context.
2. Apply the transform.
3. Observe: the total renders as `NaN` or the transform fails.

**Root Cause**  
XPath 1.0 `sum()` accepts a node-set and coerces each node to a number. It cannot evaluate `quantity * unitPrice` inline per-node.

**Fix Applied**  
Declared an `<xsl:param name="orderTotal"/>` and passed the pre-computed total from the Java service layer (`entity.getTotalAmount()`) into the transformer via `transformer.setParameter("orderTotal", ...)`. The total is already stored in the database, so no re-computation is needed in the transform.

---

## DR-004 — `JAXBContext.newUnmarshaller()` Method Does Not Exist

| Field | Details |
|-------|---------|
| **ID** | DR-004 |
| **Title** | Compilation error: `cannot find symbol: method newUnmarshaller()` |
| **Severity** | High |
| **Status** | Fixed |
| **Found by** | Maven compile (`mvn compile`) |
| **Date Found** | 2026-06-30 |

**Description**  
`JaxbXmlMapper` called `context.newUnmarshaller()` which does not exist on `jakarta.xml.bind.JAXBContext`. The correct method name is `createUnmarshaller()`.

**Steps to Reproduce**
1. Call `JAXBContext.newInstance(...).newUnmarshaller()` in code.
2. Run `mvn compile`.
3. Observe compilation failure.

**Root Cause**  
Incorrect method name — the JAXB API uses `createUnmarshaller()` (not `newUnmarshaller()`).

**Fix Applied**  
Replaced `context.newUnmarshaller()` with `context.createUnmarshaller()` in `JaxbXmlMapper.java`.

---

## DR-005 — No Existing Defect (Open Template)

| Field | Details |
|-------|---------|
| **ID** | DR-005 |
| **Title** | *(Reserved for future defects)* |
| **Severity** | — |
| **Status** | Open |

**Template for new defects:**

```
| Field    | Details |
|----------|---------|
| ID       | DR-XXX  |
| Title    | Short description |
| Severity | Critical / High / Medium / Low |
| Status   | Open / In Progress / Fixed / Won't Fix |
| Found by | Who/what discovered it |
| Date     | YYYY-MM-DD |

Description: What goes wrong.
Steps to Reproduce: Numbered steps.
Root Cause: Why it happens.
Fix Applied / Suggested Fix: What was or should be done.
```

---

## Known Limitations (Not Defects)

| # | Limitation | Notes |
|---|-----------|-------|
| L-001 | No authentication/authorisation | All endpoints are publicly accessible. Implement Spring Security + JWT for production. |
| L-002 | No rate limiting | A client can flood the API with large payloads. Add rate limiting for production. |
| L-003 | Selenium tests skipped if Chrome unavailable | Tests use `assumeTrue` to skip gracefully rather than fail. |
| L-004 | `open-in-view` JPA warning | Spring's `open-in-view` is enabled by default. Disable in production to avoid lazy-load issues during serialisation. |
