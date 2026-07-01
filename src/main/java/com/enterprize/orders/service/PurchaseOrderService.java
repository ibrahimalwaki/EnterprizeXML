package com.enterprize.orders.service;

import com.enterprize.orders.domain.OrderItem;
import com.enterprize.orders.domain.PurchaseOrder;
import com.enterprize.orders.repository.PurchaseOrderRepository;
import com.enterprize.orders.xml.InvoiceXsltTransformer;
import com.enterprize.orders.xml.ItemXml;
import com.enterprize.orders.xml.JaxbXmlMapper;
import com.enterprize.orders.xml.OrderResponseXml;
import com.enterprize.orders.xml.PurchaseOrderXml;
import com.enterprize.orders.xml.PurchaseOrderXsdValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderXsdValidator xsdValidator;
    private final JaxbXmlMapper xmlMapper;
    private final PurchaseOrderRepository repository;
    private final InvoiceXsltTransformer invoiceTransformer;

    public PurchaseOrderService(PurchaseOrderXsdValidator xsdValidator,
                                 JaxbXmlMapper xmlMapper,
                                 PurchaseOrderRepository repository,
                                 InvoiceXsltTransformer invoiceTransformer) {
        this.xsdValidator = xsdValidator;
        this.xmlMapper = xmlMapper;
        this.repository = repository;
        this.invoiceTransformer = invoiceTransformer;
    }

    @Transactional
    public OrderResponseXml submitOrder(byte[] rawXml) {
        Document validatedDocument = xsdValidator.parseAndValidate(rawXml);
        PurchaseOrderXml orderXml = xmlMapper.unmarshalPurchaseOrder(validatedDocument);

        if (repository.existsById(orderXml.getOrderId())) {
            throw new DuplicateOrderException(orderXml.getOrderId());
        }

        PurchaseOrder entity = toEntity(orderXml);
        repository.save(entity);

        return new OrderResponseXml(
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getOrderDate(),
                "ACCEPTED",
                entity.getTotalAmount());
    }

    @Transactional(readOnly = true)
    public OrderResponseXml getOrder(String orderId) {
        PurchaseOrder entity = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return new OrderResponseXml(
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getOrderDate(),
                "ACCEPTED",
                entity.getTotalAmount());
    }

    @Transactional(readOnly = true)
    public String getInvoiceHtml(String orderId) {
        PurchaseOrder entity = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        String orderXml = xmlMapper.marshal(toXml(entity));
        return invoiceTransformer.toHtmlInvoice(orderXml, entity.getTotalAmount());
    }

    private PurchaseOrderXml toXml(PurchaseOrder entity) {
        PurchaseOrderXml xml = new PurchaseOrderXml();
        xml.setOrderId(entity.getOrderId());
        xml.setCustomerId(entity.getCustomerId());
        xml.setOrderDate(entity.getOrderDate());

        List<ItemXml> items = new ArrayList<>();
        for (OrderItem item : entity.getItems()) {
            ItemXml itemXml = new ItemXml();
            itemXml.setSku(item.getSku());
            itemXml.setDescription(item.getDescription());
            itemXml.setQuantity(item.getQuantity());
            itemXml.setUnitPrice(item.getUnitPrice());
            items.add(itemXml);
        }
        xml.setItems(items);

        return xml;
    }

    private PurchaseOrder toEntity(PurchaseOrderXml source) {
        PurchaseOrder entity = new PurchaseOrder();
        entity.setOrderId(source.getOrderId());
        entity.setCustomerId(source.getCustomerId());
        entity.setOrderDate(source.getOrderDate());

        BigDecimal total = BigDecimal.ZERO;
        for (ItemXml itemXml : source.getItems()) {
            OrderItem item = new OrderItem();
            item.setSku(itemXml.getSku());
            item.setDescription(itemXml.getDescription());
            item.setQuantity(itemXml.getQuantity());
            item.setUnitPrice(itemXml.getUnitPrice());
            entity.addItem(item);

            total = total.add(itemXml.getUnitPrice().multiply(BigDecimal.valueOf(itemXml.getQuantity())));
        }
        entity.setTotalAmount(total);

        return entity;
    }
}
