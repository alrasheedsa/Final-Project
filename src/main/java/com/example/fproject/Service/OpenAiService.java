package com.example.fproject.Service;


import com.example.fproject.Api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public AIAnalysisResult analyzeSalesDataForAIAnalysis(String salesData) {
        validateApiKey();
        validateText(salesData, "Sales data is required");

        String prompt = """
                Analyze this branch sales Excel data and return JSON only.
                Required keys:
                topProducts, lowProducts, peakHours, slowHours, surplusProducts, seasonalPatterns, recommendation, aiSummary.
                Keep every value as a short Arabic text.
                Sales data:
                """ + salesData;

        String response = sendPrompt(prompt);
        String content = extractAssistantContent(response);
        String cleanContent = cleanJsonContent(content);

        try {
            JsonNode jsonNode = objectMapper.readTree(cleanContent);

            return new AIAnalysisResult(
                    requiredJsonText(jsonNode, "topProducts"),
                    requiredJsonText(jsonNode, "lowProducts"),
                    requiredJsonText(jsonNode, "peakHours"),
                    requiredJsonText(jsonNode, "slowHours"),
                    requiredJsonText(jsonNode, "surplusProducts"),
                    jsonText(jsonNode, "seasonalPatterns"),
                    requiredJsonText(jsonNode, "recommendation"),
                    requiredJsonText(jsonNode, "aiSummary")
            );
        } catch (Exception e) {
            throw new ApiException("Failed to parse AI analysis response");
        }
    }

    public String generateCampaignSuggestion(String analysis) {
        validateApiKey();
        validateText(analysis, "Analysis is required");
        return extractAssistantContent(sendPrompt("Generate a campaign suggestion based on this analysis: " + analysis));
    }

    public AiQuestionResult generateAiQuestion() {
        validateApiKey();

        String prompt = """
                Generate one easy multiple-choice question for a WhatsApp campaign.
                Rules:
                - The question must be about Saudi Arabia only.
                - It can be about Saudi national culture, heritage, geography, history, or public national information.
                - It must be easy and suitable for all ages.
                - Return exactly three options.
                - The correct option must be only A, B, or C.
                - Return JSON only with these keys:
                  questionText, optionA, optionB, optionC, correctOption.
                """;

        String response = sendPrompt(prompt);
        String content = extractAssistantContent(response);
        String cleanContent = cleanJsonContent(content);

        try {
            JsonNode jsonNode = objectMapper.readTree(cleanContent);

            return new AiQuestionResult(
                    requiredJsonText(jsonNode, "questionText"),
                    requiredJsonText(jsonNode, "optionA"),
                    requiredJsonText(jsonNode, "optionB"),
                    requiredJsonText(jsonNode, "optionC"),
                    requiredJsonText(jsonNode, "correctOption")
            );
        } catch (Exception e) {
            throw new ApiException("Failed to parse AI question response");
        }
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

    private String extractAssistantContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new ApiException("Failed to read OpenAI response");
        }
    }

    private String cleanJsonContent(String content) {
        String cleanContent = content.trim();

        if (cleanContent.startsWith("```json")) {
            cleanContent = cleanContent.substring(7);
        }

        if (cleanContent.startsWith("```")) {
            cleanContent = cleanContent.substring(3);
        }

        if (cleanContent.endsWith("```")) {
            cleanContent = cleanContent.substring(0, cleanContent.length() - 3);
        }

        return cleanContent.trim();
    }

    private String requiredJsonText(JsonNode jsonNode, String fieldName) {
        String value = jsonText(jsonNode, fieldName);

        if (value == null || value.isBlank()) {
            throw new ApiException("AI response field is missing: " + fieldName);
        }

        return value;
    }

    private String jsonText(JsonNode jsonNode, String fieldName) {
        JsonNode value = jsonNode.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        return value.asText();
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

    public record AIAnalysisResult(
            String topProducts,
            String lowProducts,
            String peakHours,
            String slowHours,
            String surplusProducts,
            String seasonalPatterns,
            String recommendation,
            String aiSummary
    ) {
    }

    public record AiQuestionResult(
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String correctOption
    ) {
    }
}
