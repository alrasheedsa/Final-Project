package com.example.fproject.DTO.IN;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UltraMsgWebhookIn {

    @JsonProperty("event_type")
    private String eventType;

    private String instanceId;

    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {

        private String id;

        private String from;

        private String to;

        private String body;

        private String type;

        private Boolean fromMe;
    }
}
