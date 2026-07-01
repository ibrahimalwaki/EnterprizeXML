package com.enterprize.orders.controller;

import com.enterprize.orders.service.DuplicateOrderException;
import com.enterprize.orders.service.OrderNotFoundException;
import com.enterprize.orders.service.OrderValidationException;
import com.enterprize.orders.xml.JaxbXmlMapper;
import com.enterprize.orders.xml.ValidationErrorXml;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final JaxbXmlMapper xmlMapper;

    public GlobalExceptionHandler(JaxbXmlMapper xmlMapper) {
        this.xmlMapper = xmlMapper;
    }

    @ExceptionHandler(OrderValidationException.class)
    public ResponseEntity<String> handleValidation(OrderValidationException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, e.getErrors());
    }

    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<String> handleDuplicate(DuplicateOrderException e) {
        return errorResponse(HttpStatus.CONFLICT, List.of(e.getMessage()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<String> handleNotFound(OrderNotFoundException e) {
        return errorResponse(HttpStatus.NOT_FOUND, List.of(e.getMessage()));
    }

    private ResponseEntity<String> errorResponse(HttpStatus status, List<String> errors) {
        ValidationErrorXml body = new ValidationErrorXml(errors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_XML)
                .body(xmlMapper.marshal(body));
    }
}
