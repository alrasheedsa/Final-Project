package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WathqService {

    @Value("${wathq.api-key:}")
    private String apiKey;

    @Value("${wathq.client-secret:}")
    private String clientSecret;

    @Value("${wathq.base-url}")
    private String baseUrl;

    private final RestClient.Builder restClientBuilder;

    public WathqService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public String verifyCommercialRegister(String commercialRegisterNo) {
        validateApiKey();
        if (commercialRegisterNo == null || commercialRegisterNo.isBlank()) {
            throw new ApiException("Commercial register number is required");
        }
        return restClientBuilder.build()
                .get()
                .uri(baseUrl + "/commercial-registration/info/{commercialRegisterNo}", commercialRegisterNo)
                .header("apiKey", apiKey)
                .header("clientSecret", clientSecret)
                .retrieve()
                .body(String.class);
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException("Wathq API key is not configured");
        }
    }
}
