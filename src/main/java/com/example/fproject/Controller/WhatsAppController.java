package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.WhatsAppWebhookIn;
import com.example.fproject.Service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> receiveWebhook(@RequestParam("From") String from,
                                            @RequestParam("Body") String body,
                                            @RequestParam(value = "MessageSid", required = false) String messageSid) {
        WhatsAppWebhookIn webhookIn = new WhatsAppWebhookIn(from, body, messageSid);
        whatsAppService.receiveWebhook(webhookIn);
        return ResponseEntity.status(200).body(new ApiResponse("WhatsApp webhook received successfully"));
    }
}
