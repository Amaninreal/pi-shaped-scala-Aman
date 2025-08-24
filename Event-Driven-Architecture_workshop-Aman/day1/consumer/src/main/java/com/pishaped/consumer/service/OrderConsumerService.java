package com.pishaped.consumer.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumerService {
    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void processOrderPlacedEvent(String orderDetails) {
        System.out.println("Consumer: Received OrderPlaced event: " + orderDetails);
        System.out.println("Consumer: Simulating order fulfillment for: " + orderDetails);
        System.out.println("Consumer: Order fulfillment confirmed.");
        System.out.println("---------------------------------------------------------");
    }
}