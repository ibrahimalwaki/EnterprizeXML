package com.enterprize.orders.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.xml.XMLConstants;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

@Configuration
public class XsltConfig {

    @Bean
    public Templates invoiceTemplates(
            @Value("${app.xslt.invoice}") Resource xsltResource) throws TransformerConfigurationException, IOException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        // Harden against XXE / external resource access when compiling the stylesheet.
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        try (var in = xsltResource.getInputStream()) {
            return transformerFactory.newTemplates(new StreamSource(in));
        }
    }
}
