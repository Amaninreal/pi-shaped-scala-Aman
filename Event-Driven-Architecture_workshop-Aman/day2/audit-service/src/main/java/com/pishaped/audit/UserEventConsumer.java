package com.pishaped.audit;

import com.pishaped.event.model.UserEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserEventConsumer {

    @KafkaListener(topics = "user-events", groupId = "audit-service-group")
    public void consumeUserEvent(UserEvent event) {
        log.info("AUDIT-SERVICE: Received event. Auditing login for User: {}, Event Type: {}, Timestamp: {}",
                event.getUserId(), event.getEventType(), event.getTimestamp());
    }
}