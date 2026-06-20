package com.example.fproject.Service;


import com.example.fproject.Api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.example.fproject.Enum.CampaignType;

import java.time.LocalDate;
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

    public List<CampaignSuggestionResult> generateCampaignSuggestionsFromAIAnalysis(String analysisSummary, Integer suggestionRound, Integer suggestionCount) {
        validateApiKey();
        validateText(analysisSummary, "AI analysis summary is required");

        if (suggestionCount == null || suggestionCount < 1) {
            throw new ApiException("Suggestion count must be positive");
        }

        String prompt = """
            Generate exactly %s campaign suggestions based on this AI analysis.

            Return JSON only.
            Return a JSON array with exactly %s objects.
            Use Arabic text for title, description, offerText, and suggestedProductName.
            campaignType must be only DIRECT_OFFER or QUESTION_BASED.
            suggestedStartDate and suggestedEndDate must use yyyy-MM-dd format.
            suggestedStartTime and suggestedEndTime must use HH:mm format.
            Branch opening and closing times are included in the AI analysis.
            All suggested campaign times must be inside branch working hours.
            suggestedStartTime must be same as or after branch opening time.
            suggestedEndTime must be same as or before branch closing time.
            Never suggest campaigns before opening time or after closing time.
            suggestedStartDate must not be in the past.
            suggestedEndDate must be same day or after suggestedStartDate.
            discountValue must be between 0 and 100.
            targetCustomersCount must be positive.

            JSON shape:
            [
              {
                "title": "Arabic text",
                "description": "Arabic text",
                "offerText": "Arabic text",
                "campaignType": "DIRECT_OFFER",
                "suggestedStartDate": "2026-06-20",
                "suggestedEndDate": "2026-06-20",
                "suggestedStartTime": "15:00",
                "suggestedEndTime": "17:00",
                "targetCustomersCount": 100,
                "discountValue": 25,
                "suggestedProductName": "Arabic product name"
              }
            ]

            AI analysis:
            %s
            """.formatted(suggestionCount, suggestionCount, analysisSummary);

        String response = sendPrompt(prompt);
        String content = extractAssistantContent(response);
        String cleanContent = cleanJsonContent(content);

        try {
            JsonNode jsonNode = objectMapper.readTree(cleanContent);

            if (!jsonNode.isArray() || jsonNode.size() != suggestionCount) {
                throw new ApiException("AI must return exactly " + suggestionCount + " campaign suggestions");
            }

            List<CampaignSuggestionResult> results = new ArrayList<>();

            for (JsonNode suggestionNode : jsonNode) {
                String title = requiredJsonText(suggestionNode, "title");
                String description = jsonText(suggestionNode, "description");
                String offerText = requiredJsonText(suggestionNode, "offerText");
                String campaignTypeText = requiredJsonText(suggestionNode, "campaignType");
                String startDateText = requiredJsonText(suggestionNode, "suggestedStartDate");
                String endDateText = requiredJsonText(suggestionNode, "suggestedEndDate");
                String startTimeText = requiredJsonText(suggestionNode, "suggestedStartTime");
                String endTimeText = requiredJsonText(suggestionNode, "suggestedEndTime");
                Integer targetCustomersCount = requiredJsonInteger(suggestionNode, "targetCustomersCount");
                Double discountValue = requiredJsonDouble(suggestionNode, "discountValue");
                String suggestedProductName = requiredJsonText(suggestionNode, "suggestedProductName");

                CampaignType campaignType = CampaignType.valueOf(campaignTypeText);
                LocalDate suggestedStartDate = LocalDate.parse(startDateText);
                LocalDate suggestedEndDate = LocalDate.parse(endDateText);
                LocalTime suggestedStartTime = LocalTime.parse(startTimeText);
                LocalTime suggestedEndTime = LocalTime.parse(endTimeText);

                results.add(new CampaignSuggestionResult(
                        title,
                        description,
                        offerText,
                        campaignType,
                        suggestedStartDate,
                        suggestedEndDate,
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

    public BranchRadiusAIResult recommendBranchRadius(
            String branchName,
            Double branchLatitude,
            Double branchLongitude,
            Integer currentCampaignRadiusMeters,
            Integer customersWithin500,
            Integer customersWithin1500,
            Integer customersWithin3000,
            Integer customersWithin5000,
            Integer customersWithin7000,
            Integer customersWithin10000,
            Integer customersWithin20000,
            Integer customersWithin40000
    ) {
        validateApiKey();

        String prompt = """
            You are an AI location and marketing analyst for a smart retail campaign platform.

            Your task:
            Recommend the best campaign radius in meters for a store branch.

            Return JSON only.
            Do not use markdown.
            Do not add any explanation outside JSON.

            Rules:
            - recommendedRadiusMeters must be one of these exact values only:
              500, 1500, 3000, 5000, 7000, 10000, 20000, 40000
            - Use Arabic text for reason.
            - Choose the radius based on customer density and campaign relevance.
            - Do not always choose the biggest radius.
            - If many customers are close to the branch, choose a smaller radius.
            - If few customers are close, choose a wider radius.
            - The result must be practical for a local retail campaign.

            Required JSON shape:
            {
              "recommendedRadiusMeters": 3000,
              "reason": "Arabic reason"
            }

            Branch data:
            Branch name: %s
            Branch latitude: %s
            Branch longitude: %s
            Current campaign radius meters: %s

            Customer counts by radius:
            Within 500 meters: %s
            Within 1500 meters: %s
            Within 3000 meters: %s
            Within 5000 meters: %s
            Within 7000 meters: %s
            Within 10000 meters: %s
            Within 20000 meters: %s
            Within 40000 meters: %s
            """.formatted(
                branchName,
                branchLatitude,
                branchLongitude,
                currentCampaignRadiusMeters,
                customersWithin500,
                customersWithin1500,
                customersWithin3000,
                customersWithin5000,
                customersWithin7000,
                customersWithin10000,
                customersWithin20000,
                customersWithin40000
        );

        String response = sendPrompt(prompt);
        String content = extractAssistantContent(response);
        String cleanContent = cleanJsonContent(content);

        try {
            JsonNode jsonNode = objectMapper.readTree(cleanContent);

            Integer recommendedRadiusMeters =
                    requiredJsonInteger(jsonNode, "recommendedRadiusMeters");

            String reason =
                    requiredJsonText(jsonNode, "reason");

            if (!isAllowedRadius(recommendedRadiusMeters)) {
                throw new ApiException("AI returned invalid recommended radius");
            }

            return new BranchRadiusAIResult(recommendedRadiusMeters, reason);

        } catch (ApiException e) {
            throw e;

        } catch (Exception e) {
            throw new ApiException("Failed to parse AI branch radius response");
        }
    }

    public String generateMonthlyReportSummary(
            String storeName,
            String branchName,
            Integer month,
            Integer year,
            Double totalSales,
            Integer totalQuantity,
            String topProducts,
            String lowProducts,
            String surplusProducts,
            String peakHours,
            String slowHours
    ) {
        validateApiKey();
        validateText(storeName, "Store name is required");
        validateText(branchName, "Branch name is required");

        String prompt = """
                أنت محلل أعمال لمنصة تجزئة سعودية.
                حلل بيانات التقرير الشهري التالية واكتب ملخصا تنفيذيا باللغة العربية لصاحب المتجر.

                القواعد:
                - اعتمد فقط على البيانات المقدمة، ولا تخترع أرقاما أو معلومات.
                - اكتب فقرة واضحة من 3 إلى 5 جمل.
                - اذكر أداء المبيعات، المنتجات الأقوى والأضعف، وساعات الذروة والركود.
                - اختم بتوصية عملية واحدة أو اثنتين لتحسين المبيعات.
                - أعد النص العربي فقط، بدون JSON وبدون Markdown.

                المتجر: %s
                الفرع: %s
                الشهر: %s
                السنة: %s
                إجمالي المبيعات بالريال: %.2f
                إجمالي الكمية المباعة: %s
                أعلى المنتجات: %s
                أقل المنتجات: %s
                المنتجات المقترحة للترويج: %s
                ساعة الذروة: %s
                ساعة الركود: %s
                """.formatted(
                storeName,
                branchName,
                month,
                year,
                totalSales,
                totalQuantity,
                topProducts,
                lowProducts,
                surplusProducts,
                peakHours,
                slowHours
        );

        String summary = extractAssistantContent(sendPrompt(prompt)).trim();
        validateText(summary, "OpenAI returned an empty monthly report summary");
        return summary;
    }

    private boolean isAllowedRadius(Integer radiusMeters) {

        return radiusMeters != null && (
                radiusMeters.equals(500)
                        || radiusMeters.equals(1500)
                        || radiusMeters.equals(3000)
                        || radiusMeters.equals(5000)
                        || radiusMeters.equals(7000)
                        || radiusMeters.equals(10000)
                        || radiusMeters.equals(20000)
                        || radiusMeters.equals(40000)
        );
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
            LocalDate suggestedStartDate,
            LocalDate suggestedEndDate,
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

    public record BranchRadiusAIResult(
            Integer recommendedRadiusMeters,
            String reason
    ) {
    }

}
