package com.enterprize.orders.controller;

import com.enterprize.orders.service.PurchaseOrderService;
import com.enterprize.orders.xml.JaxbXmlMapper;
import com.enterprize.orders.xml.OrderResponseXml;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class PurchaseOrderController {

    private final PurchaseOrderService orderService;
    private final JaxbXmlMapper xmlMapper;

    public PurchaseOrderController(PurchaseOrderService orderService, JaxbXmlMapper xmlMapper) {
        this.orderService = orderService;
        this.xmlMapper = xmlMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> submitOrder(@RequestBody byte[] rawXml) {
        OrderResponseXml response = orderService.submitOrder(rawXml);
        return ResponseEntity.status(HttpStatus.CREATED).body(xmlMapper.marshal(response));
    }

    @GetMapping(value = "/{orderId}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getOrder(@PathVariable String orderId) {
        OrderResponseXml response = orderService.getOrder(orderId);
        return ResponseEntity.ok(xmlMapper.marshal(response));
    }

    @GetMapping(value = "/{orderId}/invoice", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getInvoice(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getInvoiceHtml(orderId));
    }
}
