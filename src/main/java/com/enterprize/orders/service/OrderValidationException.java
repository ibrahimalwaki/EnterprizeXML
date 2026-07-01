package com.enterprize.orders.service;

import java.util.List;

public class OrderValidationException extends RuntimeException {

    private final List<String> errors;

    public OrderValidationException(List<String> errors) {
        super("Purchase order failed validation: " + errors);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
