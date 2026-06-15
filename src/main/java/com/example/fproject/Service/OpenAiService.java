package com.example.fproject.Service;


import com.example.fproject.Api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final RestClient.Builder restClientBuilder;

    public OpenAiService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public String analyzeSalesData(String salesData) {
        validateApiKey();
        validateText(salesData, "Sales data is required");
        return sendPrompt("Analyze this sales data and return peak hours, idle hours, and recommendations: " + salesData);
    }

    public String generateCampaignSuggestion(String analysis) {
        validateApiKey();
        validateText(analysis, "Analysis is required");
        return sendPrompt("Generate a campaign suggestion based on this analysis: " + analysis);
    }

    public String generateCampaignQuestion(String campaignDetails) {
        validateApiKey();
        validateText(campaignDetails, "Campaign details are required");
        return sendPrompt("Generate one smart customer question for this campaign: " + campaignDetails);
    }

    private String sendPrompt(String prompt) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        return restClientBuilder.build()
                .post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(String.class);
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException("OpenAI API key is not configured");
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
    }
}
