package com.example.fproject.DTO.IN;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppWebhookIn {

    private String from;

    private String body;

    private String messageSid;
}
