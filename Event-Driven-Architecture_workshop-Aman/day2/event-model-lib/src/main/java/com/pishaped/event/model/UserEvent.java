package com.pishaped.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    // This tells Jackson: "When you create JSON, name this field 'event'"
    @JsonProperty("event")
    private String eventType;

    // This tells Jackson: "When you create JSON, name this field 'user_id'"
    @JsonProperty("user_id")
    private String userId;

    // This field is extra, but we can keep it. It will be named "timestamp"
    private long timestamp;
}