package com.enterprize.orders.xml;

import org.springframework.stereotype.Component;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;

@Component
public class InvoiceXsltTransformer {

    private final Templates invoiceTemplates;

    public InvoiceXsltTransformer(Templates invoiceTemplates) {
        this.invoiceTemplates = invoiceTemplates;
    }

    public String toHtmlInvoice(String purchaseOrderXml, BigDecimal orderTotal) {
        try {
            Transformer transformer = invoiceTemplates.newTransformer();
            transformer.setParameter("orderTotal", orderTotal.setScale(2, java.math.RoundingMode.HALF_UP).toString());

            StringWriter writer = new StringWriter();
            transformer.transform(
                    new StreamSource(new StringReader(purchaseOrderXml)),
                    new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException("Failed to transform purchase order to HTML invoice", e);
        }
    }
}
