package com.enterprize.orders.service;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("No order found with orderId '" + orderId + "'");
    }
}
