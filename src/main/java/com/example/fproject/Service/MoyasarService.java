package com.example.fproject.Service;

import com.example.fproject.DTO.OUT.MoyasarPaymentOut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class MoyasarService {

    @Value("${moyasar.api-key}")
    private String secretKey;

    @Value("${moyasar.base-url:https://api.moyasar.com/v1}")
    private String moyasarBaseUrl;

    @Value("${moyasar.publishable-key}")
    private String publishableKey;

    private final RestClient restClient = RestClient.create();

    public MoyasarPaymentOut getPaymentById(String paymentId) {

        Map response = restClient.get()
                .uri(moyasarBaseUrl + "/payments/" + paymentId)
                .headers(headers -> headers.setBasicAuth(secretKey, ""))
                .retrieve()
                .body(Map.class);

        String id = response.get("id").toString();
        String status = response.get("status").toString();
        Integer amount = (Integer) response.get("amount");
        String currency = response.get("currency").toString();

        return new MoyasarPaymentOut(id, status, amount, currency);
    }

    public String getPublishableKey() {
        return publishableKey;
    }
}