package com.enterprize.orders.xml;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "purchaseOrder")
@XmlAccessorType(XmlAccessType.FIELD)
public class PurchaseOrderXml {

    @XmlElement(required = true)
    private String orderId;

    @XmlElement(required = true)
    private String customerId;

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate orderDate;

    @XmlElementWrapper(name = "items")
    @XmlElement(name = "item")
    private List<ItemXml> items = new ArrayList<>();

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public List<ItemXml> getItems() {
        return items;
    }

    public void setItems(List<ItemXml> items) {
        this.items = items;
    }
}
