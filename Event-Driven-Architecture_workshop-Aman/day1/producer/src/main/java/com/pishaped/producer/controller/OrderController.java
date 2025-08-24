package com.pishaped.producer.controller;

import com.pishaped.producer.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody String orderDetails) {
        orderService.publishOrderPlacedEvent(orderDetails);
        return ResponseEntity.ok("Order placed and event published successfully!");
    }
}