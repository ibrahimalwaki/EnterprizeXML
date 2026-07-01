package com.enterprize.orders.service;

public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException(String orderId) {
        super("An order with orderId '" + orderId + "' already exists");
    }
}
