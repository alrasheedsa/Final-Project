package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.UltraMsgWebhookIn;
import com.example.fproject.Service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @PostMapping("/webhook")
    public ResponseEntity<?> receiveWebhook(@RequestBody UltraMsgWebhookIn webhookIn) {
        whatsAppService.receiveWebhook(webhookIn);
        return ResponseEntity.status(200).body(new ApiResponse("WhatsApp webhook received successfully"));
    }
}
