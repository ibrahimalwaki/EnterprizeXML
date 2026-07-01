package com.enterprize.orders.xml;

import com.enterprize.orders.service.OrderValidationException;
import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PurchaseOrderXsdValidator {

    private final Schema schema;

    public PurchaseOrderXsdValidator(Schema purchaseOrderSchema) {
        this.schema = purchaseOrderSchema;
    }

    public Document parseAndValidate(byte[] xmlBytes) {
        Document document = parseSecurely(xmlBytes);

        List<String> errors = new ArrayList<>();
        Validator validator = schema.newValidator();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) {
                // Warnings do not fail validation.
            }

            @Override
            public void error(SAXParseException exception) {
                errors.add(describe(exception));
            }

            @Override
            public void fatalError(SAXParseException exception) {
                errors.add(describe(exception));
            }

            private String describe(SAXParseException e) {
                return "Line " + e.getLineNumber() + ": " + e.getMessage();
            }
        });

        try {
            validator.validate(new DOMSource(document));
        } catch (SAXException | IOException e) {
            errors.add("Schema validation failed: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new OrderValidationException(errors);
        }

        return document;
    }

    private Document parseSecurely(byte[] xmlBytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Harden against XXE / entity expansion attacks on untrusted input.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            try (InputStream in = new ByteArrayInputStream(xmlBytes)) {
                return builder.parse(in);
            }
        } catch (ParserConfigurationException | IOException e) {
            throw new OrderValidationException(List.of("Unable to parse XML: " + e.getMessage()));
        } catch (SAXException e) {
            throw new OrderValidationException(List.of("Malformed XML: " + e.getMessage()));
        }
    }
}
