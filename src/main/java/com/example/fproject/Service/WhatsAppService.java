package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.number}")
    private String fromNumber;

    public String sendMessage(String phone, String messageBody) {
        validateConfiguration();
        validateText(phone, "Phone is required");
        validateText(messageBody, "Message is required");
        Twilio.init(accountSid, authToken);
        Message.creator(
                new PhoneNumber("whatsapp:" + phone),
                new PhoneNumber("whatsapp:" + fromNumber),
                messageBody
        ).create();
        return "WhatsApp message has been sent";
    }

    public String sendQrCode(String phone, String qrCode) {
        validateText(phone, "Phone is required");
        validateText(qrCode, "QR code is required");
        return sendMessage(phone, qrCode);
    }

    public String receiveWebhook(String payload) {
        validateText(payload, "Webhook payload is required");
        return "WhatsApp webhook has been received";
    }

    private void validateConfiguration() {
        if (accountSid == null || accountSid.isBlank()) {
            throw new ApiException("Twilio account SID is not configured");
        }
        if (authToken == null || authToken.isBlank()) {
            throw new ApiException("Twilio auth token is not configured");
        }
        if (fromNumber == null || fromNumber.isBlank()) {
            throw new ApiException("Twilio WhatsApp number is not configured");
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
    }
}
