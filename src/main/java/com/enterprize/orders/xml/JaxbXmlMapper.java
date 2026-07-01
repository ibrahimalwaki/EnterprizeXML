package com.enterprize.orders.xml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.io.StringWriter;

@Component
public class JaxbXmlMapper {

    public PurchaseOrderXml unmarshalPurchaseOrder(Document document) {
        try {
            JAXBContext context = JAXBContext.newInstance(PurchaseOrderXml.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (PurchaseOrderXml) unmarshaller.unmarshal(document);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to bind validated XML to PurchaseOrderXml", e);
        }
    }

    public String marshal(Object xmlObject) {
        try {
            JAXBContext context = JAXBContext.newInstance(xmlObject.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            marshaller.marshal(xmlObject, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to marshal " + xmlObject.getClass().getSimpleName() + " to XML", e);
        }
    }
}
