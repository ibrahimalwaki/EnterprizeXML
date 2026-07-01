package com.enterprize.orders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PurchaseOrderApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID_ORDER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <purchaseOrder>
                <orderId>PO-1001</orderId>
                <customerId>CUST-1</customerId>
                <orderDate>2026-06-30</orderDate>
                <items>
                    <item>
                        <sku>SKU-1</sku>
                        <description>Widget</description>
                        <quantity>3</quantity>
                        <unitPrice>9.99</unitPrice>
                    </item>
                </items>
            </purchaseOrder>
            """;

    private static final String MISSING_CUSTOMER_ID = """
            <?xml version="1.0" encoding="UTF-8"?>
            <purchaseOrder>
                <orderId>PO-1002</orderId>
                <orderDate>2026-06-30</orderDate>
                <items>
                    <item>
                        <sku>SKU-1</sku>
                        <description>Widget</description>
                        <quantity>3</quantity>
                        <unitPrice>9.99</unitPrice>
                    </item>
                </items>
            </purchaseOrder>
            """;

    private static final String MALFORMED_XML = "<purchaseOrder><orderId>PO-1003</orderId>";

    @Test
    void acceptsValidOrderAndPersistsIt() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<status>ACCEPTED</status>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<totalAmount>29.97</totalAmount>")));

        mockMvc.perform(get("/api/orders/PO-1001"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<customerId>CUST-1</customerId>")));
    }

    @Test
    void rejectsDuplicateOrderId() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(VALID_ORDER.replace("PO-1001", "PO-2001")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(VALID_ORDER.replace("PO-1001", "PO-2001")))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsMissingRequiredElement() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(MISSING_CUSTOMER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<validationErrors>")));
    }

    @Test
    void rejectsMalformedXml() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(MALFORMED_XML))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        mockMvc.perform(get("/api/orders/DOES-NOT-EXIST"))
                .andExpect(status().isNotFound());
    }

    @Test
    void generatesHtmlInvoiceForExistingOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(VALID_ORDER.replace("PO-1001", "PO-3001")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders/PO-3001/invoice"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invoice for Order PO-3001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("29.97")));
    }
}
