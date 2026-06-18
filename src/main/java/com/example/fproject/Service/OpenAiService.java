package com.example.fproject.Service;


import com.example.fproject.Api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.example.fproject.Enum.CampaignType;
import java.time.LocalTime;
import java.util.ArrayList;

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
            You are an AI business analyst for a smart retail platform.
            The platform helps stores use dead hours to create smart campaigns.

            Analyze the provided branch sales summary and return JSON only.

            Important rules:
            - Return valid JSON only.
            - Do not wrap the response in markdown.
            - Do not add explanations outside the JSON.
            - Every value must be a short Arabic text.
            - Base your answer only on the provided sales data.
            - If a value is unclear, write a reasonable Arabic explanation based on the data.
            - Focus on dead hours, weak products, high-performing products, and campaign opportunities.

            Required JSON keys:
            {
              "topProducts": "Arabic text",
              "lowProducts": "Arabic text",
              "peakHours": "Arabic text",
              "slowHours": "Arabic text",
              "surplusProducts": "Arabic text",
              "seasonalPatterns": "Arabic text",
              "recommendation": "Arabic text",
              "aiSummary": "Arabic text"
            }

            What each key means:
            - topProducts: products with strongest sales performance.
            - lowProducts: products with weakest sales performance.
            - peakHours: hours with strongest sales or revenue.
            - slowHours: dead hours or weak selling periods.
            - surplusProducts: products that may need promotion or clearance.
            - seasonalPatterns: repeated pattern noticed from the month or hours.
            - recommendation: one strategic recommendation for the store owner.
            - aiSummary: short summary of the whole analysis.

            Sales summary:
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

    public List<CampaignSuggestionResult> generateCampaignSuggestionsFromAIAnalysis(String analysisSummary, Integer suggestionRound) {
        validateApiKey();
        validateText(analysisSummary, "AI analysis summary is required");

        String prompt = """
            Generate exactly 3 campaign suggestions based on this AI analysis.

            Return JSON only.
            Return a JSON array with exactly 3 objects.
            Use Arabic text for title, description, offerText, and suggestedProductName.
            campaignType must be only DIRECT_OFFER or QUESTION_BASED.
            suggestedStartTime and suggestedEndTime must use HH:mm format.
            discountValue must be between 0 and 100.
            targetCustomersCount must be positive.

            JSON shape:
            [
              {
                "title": "Arabic text",
                "description": "Arabic text",
                "offerText": "Arabic text",
                "campaignType": "DIRECT_OFFER",
                "suggestedStartTime": "15:00",
                "suggestedEndTime": "17:00",
                "targetCustomersCount": 100,
                "discountValue": 25,
                "suggestedProductName": "Arabic product name"
              }
            ]

            AI analysis:
            """ + analysisSummary;

        String response = sendPrompt(prompt);
        String content = extractAssistantContent(response);
        String cleanContent = cleanJsonContent(content);

        try {
            JsonNode jsonNode = objectMapper.readTree(cleanContent);

            if (!jsonNode.isArray() || jsonNode.size() != 3) {
                throw new ApiException("AI must return exactly 3 campaign suggestions");
            }

            List<CampaignSuggestionResult> results = new ArrayList<>();

            for (JsonNode suggestionNode : jsonNode) {
                String title = requiredJsonText(suggestionNode, "title");
                String description = jsonText(suggestionNode, "description");
                String offerText = requiredJsonText(suggestionNode, "offerText");
                String campaignTypeText = requiredJsonText(suggestionNode, "campaignType");
                String startTimeText = requiredJsonText(suggestionNode, "suggestedStartTime");
                String endTimeText = requiredJsonText(suggestionNode, "suggestedEndTime");
                Integer targetCustomersCount = requiredJsonInteger(suggestionNode, "targetCustomersCount");
                Double discountValue = requiredJsonDouble(suggestionNode, "discountValue");
                String suggestedProductName = requiredJsonText(suggestionNode, "suggestedProductName");

                CampaignType campaignType = CampaignType.valueOf(campaignTypeText);
                LocalTime suggestedStartTime = LocalTime.parse(startTimeText);
                LocalTime suggestedEndTime = LocalTime.parse(endTimeText);

                results.add(new CampaignSuggestionResult(
                        title,
                        description,
                        offerText,
                        campaignType,
                        suggestedStartTime,
                        suggestedEndTime,
                        targetCustomersCount,
                        discountValue,
                        suggestedProductName,
                        suggestionRound
                ));
            }

            return results;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to parse AI campaign suggestions response");
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

    private Integer requiredJsonInteger(JsonNode jsonNode, String fieldName) {
        JsonNode value = jsonNode.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            throw new ApiException("AI response field is missing: " + fieldName);
        }

        return value.asInt();
    }

    private Double requiredJsonDouble(JsonNode jsonNode, String fieldName) {
        JsonNode value = jsonNode.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            throw new ApiException("AI response field is missing: " + fieldName);
        }

        return value.asDouble();
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

    public record CampaignSuggestionResult(
            String title,
            String description,
            String offerText,
            CampaignType campaignType,
            LocalTime suggestedStartTime,
            LocalTime suggestedEndTime,
            Integer targetCustomersCount,
            Double discountValue,
            String suggestedProductName,
            Integer suggestionRound
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
