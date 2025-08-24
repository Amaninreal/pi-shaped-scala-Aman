package com.pishaped.producer.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;
    @Value("${app.rabbitmq.routingkey}")
    private String routingKey;

    public void publishOrderPlacedEvent(String orderDetails) {
        System.out.println("Producer: Publishing OrderPlaced event: " + orderDetails);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, orderDetails);
    }
}