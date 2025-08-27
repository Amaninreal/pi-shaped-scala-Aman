package com.pishaped.user;

import com.pishaped.event.model.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private static final String TOPIC = "user-events";

    public void sendLoginEvent(String userId) {
        UserEvent event = new UserEvent("USER_LOGIN", userId, System.currentTimeMillis());

        log.info("USER-SERVICE: Sending event for user -> {}", userId);
        kafkaTemplate.send(TOPIC, userId, event);
    }
}