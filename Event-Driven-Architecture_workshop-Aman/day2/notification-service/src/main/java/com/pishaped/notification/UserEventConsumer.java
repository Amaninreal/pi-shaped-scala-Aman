package com.pishaped.notification;

import com.pishaped.event.model.UserEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserEventConsumer {

    @KafkaListener(topics = "user-events", groupId = "notification-service-group")
    public void consumeUserEvent(UserEvent event) {
        // Business logic for this service: Send a "notification".
        log.info("NOTIFICATION-SERVICE: Received event. Sending push notification to User: {}",
                event.getUserId());
    }
}