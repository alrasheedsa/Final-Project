package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class MoyasarService {

    @Value("${moyasar.api-key:}")
    private String apiKey;

    @Value("${moyasar.base-url}")
    private String baseUrl;


    private final RestClient.Builder restClientBuilder;

    public MoyasarService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public String createPayment(Double amount, String description) {
        validateApiKey();
        if (amount == null || amount <= 0) {
            throw new ApiException("Payment amount must be greater than zero");
        }
        if (description == null || description.isBlank()) {
            throw new ApiException("Payment description is required");
        }
        Map<String, Object> request = Map.of(
                "amount", Math.round(amount * 100),
                "currency", "SAR",
                "description", description
        );
        return restClientBuilder.build()
                .post()
                .uri(baseUrl + "/payments")
                .headers(headers -> headers.setBasicAuth(apiKey, ""))
                .body(request)
                .retrieve()
                .body(String.class);
    }

    public String getPaymentStatus(String transactionId) {
        validateApiKey();
        if (transactionId == null || transactionId.isBlank()) {
            throw new ApiException("Transaction id is required");
        }
        return restClientBuilder.build()
                .get()
                .uri(baseUrl + "/payments/{transactionId}", transactionId)
                .headers(headers -> headers.setBasicAuth(apiKey, ""))
                .retrieve()
                .body(String.class);
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException("Moyasar API key is not configured");
        }
    }
}
