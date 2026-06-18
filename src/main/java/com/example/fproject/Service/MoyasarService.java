package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MoyasarService {

    @Value("${moyasar.api-key}")
    private String secretKey;

    @Value("${moyasar.publishable-key}")
    private String publishableKey;

    @Value("${moyasar.base-url:https://api.moyasar.com/v1}")
    private String baseUrl;

    private final RestClient.Builder restClientBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MoyasarService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public String getPublishableKey() {
        if (publishableKey == null || publishableKey.isBlank()) {
            throw new ApiException("Moyasar publishable key is not configured");
        }

        return publishableKey;
    }

    public JsonNode getPayment(String moyasarPaymentId) {
        validateSecretKey();

        if (moyasarPaymentId == null || moyasarPaymentId.isBlank()) {
            throw new ApiException("Moyasar payment id is required");
        }

        try {
            String response = restClientBuilder.build()
                    .get()
                    .uri(baseUrl + "/payments/{id}", moyasarPaymentId)
                    .headers(headers -> headers.setBasicAuth(secretKey, ""))
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                throw new ApiException("Empty response from Moyasar");
            }

            return objectMapper.readTree(response);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Moyasar payment verification failed: " + e.getMessage());
        }
    }

    public String extractPaymentStatus(JsonNode paymentJson) {
        String status = paymentJson.path("status").asText(null);

        if (status == null || status.isBlank()) {
            throw new ApiException("Moyasar response does not contain payment status");
        }

        return status;
    }

    public Long extractAmount(JsonNode paymentJson) {
        JsonNode amountNode = paymentJson.path("amount");

        if (amountNode == null || amountNode.isMissingNode() || amountNode.isNull()) {
            throw new ApiException("Moyasar response does not contain amount");
        }

        return amountNode.asLong();
    }

    public String extractCurrency(JsonNode paymentJson) {
        String currency = paymentJson.path("currency").asText(null);

        if (currency == null || currency.isBlank()) {
            throw new ApiException("Moyasar response does not contain currency");
        }

        return currency;
    }

    private void validateSecretKey() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new ApiException("Moyasar secret key is not configured");
        }
    }
}