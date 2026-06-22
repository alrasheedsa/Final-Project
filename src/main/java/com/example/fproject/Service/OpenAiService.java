package com.example.fproject.Service;


import com.example.fproject.Api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.example.fproject.Enum.CampaignType;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        The platform helps store owners identify weak sales opportunities and create smart campaigns.

        Analyze the provided branch sales summary and return JSON only.

        Important rules:
        - Return valid JSON only.
        - Do not wrap the response in markdown.
        - Do not add explanations outside the JSON.
        - Every value must be Arabic text.
        - This analysis is internal and shown to the store owner only.
        - It is allowed to mention ركود، ضعف المبيعات، أوقات ضعيفة، أيام ضعيفة، فائض المنتجات here.
        - Base your answer only on the provided sales data.
        - Do not invent weak days if the data does not clearly show them.
        - If there is no clear weak day, say clearly: لا يوجد يوم ركود واضح من البيانات.
        - If there is a clear slow hour range, write it in HH:mm-HH:mm format, for example: 15:00-17:00.
        - If there are weak weekdays, mention the weekday names clearly in Arabic, for example: الأحد والثلاثاء.

        Required JSON keys:
        {
          "topProducts": "Arabic owner-facing text",
          "lowProducts": "Arabic owner-facing text",
          "peakHours": "Arabic owner-facing text",
          "slowHours": "Arabic owner-facing text",
          "surplusProducts": "Arabic owner-facing text",
          "seasonalPatterns": "Arabic owner-facing text",
          "recommendation": "Arabic owner-facing text",
          "aiSummary": "Arabic owner-facing text"
        }

        What each key means:
        - topProducts: strongest products by sales or quantity.
        - lowProducts: weakest products by sales or quantity.
        - peakHours: strongest hours during the day, preferably in HH:mm-HH:mm format.
        - slowHours: dead hours or weak selling periods during the day, preferably in HH:mm-HH:mm format.
        - surplusProducts: products that may need promotion or clearance.
        - seasonalPatterns: repeated patterns from dates and times, including weak weekdays, weak dates, recurring slow days, or monthly patterns.
        - recommendation: practical recommendation for the store owner.
        - aiSummary: short executive summary of the whole analysis.

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

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();

        String prompt = """
        Generate exactly %s campaign suggestions based on this AI analysis.

        Return JSON only.
        Return a JSON array with exactly %s objects.
        Use Arabic text for title, description, offerText, and suggestedProductName.

        Very important:
        AIAnalysis is internal for the store owner.
        CampaignSuggestion content should be customer-friendly because it may later become the real campaign content.

        Customer-facing text rules:
        - title, description, and offerText must be suitable for customers.                                                                              
        - Never mention internal business problems to customers.
        - Do not use these words in title, description, or offerText:
        ركود، وقت ركود، أوقات الركود، فترات الركود، ضعف المبيعات، مبيعات ضعيفة، تصريف، فائض، منتجات راكدة.
        - Do not include dates or times in title, description, or offerText.
        - Dates and times must appear only in suggestedStartDate, suggestedEndDate, suggestedStartTime, and suggestedEndTime.
        - Use positive marketing wording instead:
        عرض لفترة محدودة، خصم خاص، فرصة مميزة، جرّب الآن، لا تفوّت العرض.
        - The customer should feel it is a special offer, not that the store is trying to fix weak sales.

        campaignType must be only one of these exact values:
        - DIRECT_OFFER
        - QUESTION_BASED

        Current date and time:
        - Today date is %s.
        - Today day of week is %s.
        - Current time is %s.

        Scheduling rules:
        - Use sales data patterns as the primary decision source.
        - Use seasonalPatterns to understand weak days.
        - Use slowHours to understand weak time ranges.
        - Use holiday context as a seasonal factor, not as the only decision.
        - If an upcoming holiday is relevant to the product and the branch is open, you may prioritize the holiday date or the day before it.
        - If sales data shows a clearer weak day, prefer the weak day unless the holiday creates a stronger campaign opportunity.
        - Do not assume every holiday increases visits.
        - If a holiday is not relevant to the product or timing, ignore it and use the sales pattern.
        - If weak weekdays are mentioned, choose a matching upcoming weekday.
        - If a slow-hour time range is mentioned, use that time range.
        - Do not choose a date or time in the past.
        - Do not choose campaign times outside branch opening and closing hours.
        - Do not choose random times.
        - Do not copy sample values blindly.

        Date and time format rules:
        - suggestedStartDate and suggestedEndDate must be real dates in yyyy-MM-dd format.
        - suggestedStartTime and suggestedEndTime must use HH:mm format.
        - suggestedEndDate must be same day or after suggestedStartDate.
        - suggestedEndTime must be after suggestedStartTime.

        Business rules:
        - discountValue must be between 0 and 100.
        - targetCustomersCount must be positive.
        - suggestedProductName must be based on products mentioned in the analysis when possible.
        - Do not return null values.
        - Do not add any explanation outside the JSON.
        
        Campaign content rules:
        - If campaignType is DIRECT_OFFER, create a direct discount or product offer.
        - If campaignType is QUESTION_BASED, make the title and description clearly invite the customer to answer a simple question or participate in a quick interaction.
        - QUESTION_BASED offerText should mention a reward after participation, not only a normal purchase discount.
        - Make the suggestions diverse. Do not repeat the same product in every suggestion unless the analysis only contains one suitable product.

        JSON shape. Use the same keys exactly:
        [
          {
            "title": "عنوان تسويقي مناسب للعميل",
            "description": "وصف تسويقي مناسب للعميل بدون ذكر الركود أو ضعف المبيعات",
            "offerText": "نص العرض الذي يمكن إرساله للعميل",
            "campaignType": "DIRECT_OFFER",
            "suggestedStartDate": "2026-06-23",
            "suggestedEndDate": "2026-06-23",
            "suggestedStartTime": "15:00",
            "suggestedEndTime": "17:00",
            "targetCustomersCount": 100,
            "discountValue": 25,
            "suggestedProductName": "اسم المنتج"
          }
        ]

        AI analysis:
        %s
        """.formatted(
                suggestionCount,
                suggestionCount,
                today,
                todayDayOfWeek,
                now,
                analysisSummary
        );

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
                String title = sanitizeCustomerFacingText(requiredJsonText(suggestionNode, "title"));
                String description = sanitizeCustomerFacingText(jsonText(suggestionNode, "description"));
                String offerText = sanitizeCustomerFacingText(requiredJsonText(suggestionNode, "offerText"));

                String campaignTypeText = requiredJsonText(suggestionNode, "campaignType");
                String startDateText = requiredJsonText(suggestionNode, "suggestedStartDate");
                String endDateText = requiredJsonText(suggestionNode, "suggestedEndDate");
                String startTimeText = requiredJsonText(suggestionNode, "suggestedStartTime");
                String endTimeText = requiredJsonText(suggestionNode, "suggestedEndTime");
                Integer targetCustomersCount = requiredJsonInteger(suggestionNode, "targetCustomersCount");
                Double discountValue = requiredJsonDouble(suggestionNode, "discountValue");
                String suggestedProductName = sanitizeCustomerFacingText(requiredJsonText(suggestionNode, "suggestedProductName"));

                CampaignType campaignType;
                try {
                    campaignType = CampaignType.valueOf(campaignTypeText.trim());
                } catch (Exception e) {
                    throw new ApiException("AI returned invalid campaign type");
                }

                LocalDate aiSuggestedStartDate;
                LocalDate aiSuggestedEndDate;
                LocalTime aiSuggestedStartTime;
                LocalTime aiSuggestedEndTime;

                try {
                    aiSuggestedStartDate = LocalDate.parse(startDateText.trim());
                    aiSuggestedEndDate = LocalDate.parse(endDateText.trim());
                    aiSuggestedStartTime = parseFlexibleTime(startTimeText);
                    aiSuggestedEndTime = parseFlexibleTime(endTimeText);
                } catch (Exception e) {
                    throw new ApiException("AI returned invalid campaign date or time format");
                }

                LocalTime[] resolvedTimeRange = resolveCampaignTimeRange(
                        analysisSummary,
                        aiSuggestedStartTime,
                        aiSuggestedEndTime
                );

                LocalTime suggestedStartTime = resolvedTimeRange[0];
                LocalTime suggestedEndTime = resolvedTimeRange[1];

                LocalDate suggestedStartDate = resolveCampaignStartDate(
                        analysisSummary,
                        aiSuggestedStartDate,
                        suggestedStartTime,
                        today,
                        now
                );

                LocalDate suggestedEndDate = suggestedStartDate;

                title = alignCustomerTextSchedule(title, suggestedStartDate, suggestedStartTime, suggestedEndTime, today);
                description = alignCustomerTextSchedule(description, suggestedStartDate, suggestedStartTime, suggestedEndTime, today);
                offerText = alignCustomerTextSchedule(offerText, suggestedStartDate, suggestedStartTime, suggestedEndTime, today);

                targetCustomersCount = normalizeTargetCustomersCount(campaignType, targetCustomersCount);


                if (aiSuggestedEndDate.isAfter(aiSuggestedStartDate)) {
                    suggestedEndDate = aiSuggestedEndDate;
                }

                if (suggestedEndDate.isBefore(suggestedStartDate)) {
                    suggestedEndDate = suggestedStartDate;
                }

                if (!suggestedEndTime.isAfter(suggestedStartTime)) {
                    throw new ApiException("AI suggested campaign end time must be after start time");
                }

                if (targetCustomersCount == null || targetCustomersCount <= 0) {
                    throw new ApiException("AI suggested target customers count must be positive");
                }

                if (discountValue < 0 || discountValue > 100) {
                    throw new ApiException("AI suggested discount value must be between 0 and 100");
                }

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


    private LocalTime[] resolveCampaignTimeRange(String analysisSummary,
                                                 LocalTime aiSuggestedStartTime,
                                                 LocalTime aiSuggestedEndTime) {

        LocalTime[] slowHoursRange = extractSlowHoursTimeRange(analysisSummary);

        if (slowHoursRange != null && slowHoursRange[1].isAfter(slowHoursRange[0])) {
            return slowHoursRange;
        }

        if (aiSuggestedStartTime != null && aiSuggestedEndTime != null && aiSuggestedEndTime.isAfter(aiSuggestedStartTime)) {
            return new LocalTime[]{aiSuggestedStartTime, aiSuggestedEndTime};
        }

        throw new ApiException("Could not determine a valid campaign time range");
    }

    private LocalTime[] extractSlowHoursTimeRange(String analysisSummary) {
        if (analysisSummary == null || analysisSummary.isBlank()) {
            return null;
        }

        String[] lines = analysisSummary.split("\\R");

        for (String line : lines) {
            String lowerLine = line.toLowerCase();

            if (lowerLine.contains("slow hours")
                    || line.contains("أوقات الركود")
                    || line.contains("وقت الركود")
                    || line.contains("الأوقات الضعيفة")
                    || line.contains("فترات الركود")) {

                LocalTime[] range = extractFirstTimeRangeFromText(line);

                if (range != null) {
                    return range;
                }
            }
        }

        return null;
    }

    private LocalTime[] extractFirstTimeRangeFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile("(\\d{1,2}:\\d{2})\\s*(?:-|–|—|إلى|الى|حتى|to)\\s*(\\d{1,2}:\\d{2})");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            LocalTime start = parseFlexibleTime(matcher.group(1));
            LocalTime end = parseFlexibleTime(matcher.group(2));
            return new LocalTime[]{start, end};
        }

        return null;
    }

    private LocalTime parseFlexibleTime(String text) {
        if (text == null || text.isBlank()) {
            throw new ApiException("Invalid time value");
        }

        String value = text.trim();

        if (value.matches("\\d:\\d{2}")) {
            value = "0" + value;
        }

        return LocalTime.parse(value);
    }

    private LocalDate resolveCampaignStartDate(String analysisSummary,
                                               LocalDate aiSuggestedDate,
                                               LocalTime suggestedStartTime,
                                               LocalDate today,
                                               LocalTime now) {

        List<DayOfWeek> weakDays = extractWeakDaysFromAnalysis(analysisSummary);

        if (!weakDays.isEmpty()) {
            return getNearestUpcomingWeakDate(weakDays, suggestedStartTime, today, now);
        }

        if (aiSuggestedDate.isBefore(today)) {
            return today;
        }

        if (aiSuggestedDate.isEqual(today) && !suggestedStartTime.isAfter(now)) {
            return today.plusDays(1);
        }

        return aiSuggestedDate;
    }

    private List<DayOfWeek> extractWeakDaysFromAnalysis(String analysisSummary) {
        List<DayOfWeek> weakDays = new ArrayList<>();

        if (analysisSummary == null || analysisSummary.isBlank()) {
            return weakDays;
        }

        if (analysisSummary.contains("الأحد") || analysisSummary.contains("الاحد") || analysisSummary.contains("Sunday")) {
            weakDays.add(DayOfWeek.SUNDAY);
        }

        if (analysisSummary.contains("الإثنين") || analysisSummary.contains("الاثنين") || analysisSummary.contains("الأثنين") || analysisSummary.contains("Monday")) {
            weakDays.add(DayOfWeek.MONDAY);
        }

        if (analysisSummary.contains("الثلاثاء") || analysisSummary.contains("Tuesday")) {
            weakDays.add(DayOfWeek.TUESDAY);
        }

        if (analysisSummary.contains("الأربعاء") || analysisSummary.contains("الاربعاء") || analysisSummary.contains("Wednesday")) {
            weakDays.add(DayOfWeek.WEDNESDAY);
        }

        if (analysisSummary.contains("الخميس") || analysisSummary.contains("Thursday")) {
            weakDays.add(DayOfWeek.THURSDAY);
        }

        if (analysisSummary.contains("الجمعة") || analysisSummary.contains("Friday")) {
            weakDays.add(DayOfWeek.FRIDAY);
        }

        if (analysisSummary.contains("السبت") || analysisSummary.contains("Saturday")) {
            weakDays.add(DayOfWeek.SATURDAY);
        }

        return weakDays;
    }

    private LocalDate getNearestUpcomingWeakDate(List<DayOfWeek> weakDays,
                                                 LocalTime suggestedStartTime,
                                                 LocalDate today,
                                                 LocalTime now) {

        for (int i = 0; i <= 21; i++) {
            LocalDate candidateDate = today.plusDays(i);

            if (weakDays.contains(candidateDate.getDayOfWeek())) {
                if (i == 0 && !suggestedStartTime.isAfter(now)) {
                    continue;
                }

                return candidateDate;
            }
        }

        return today.plusDays(1);
    }


    private String sanitizeCustomerFacingText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String cleanText = text;

        cleanText = cleanText.replace("خلال فترات الركود", "خلال فترة العرض المحدودة");
        cleanText = cleanText.replace("خلال أوقات الركود", "خلال فترة العرض المحدودة");
        cleanText = cleanText.replace("خلال وقت الركود", "خلال فترة العرض المحدودة");
        cleanText = cleanText.replace("في فترات الركود", "في فترة العرض المحدودة");
        cleanText = cleanText.replace("في أوقات الركود", "في فترة العرض المحدودة");
        cleanText = cleanText.replace("في وقت الركود", "في فترة العرض المحدودة");

        cleanText = cleanText.replace("فترات الركود", "فترة العرض المحدودة");
        cleanText = cleanText.replace("أوقات الركود", "فترة العرض المحدودة");
        cleanText = cleanText.replace("وقت الركود", "فترة العرض المحدودة");
        cleanText = cleanText.replace("الركود", "العرض");

        cleanText = cleanText.replace("ضعف المبيعات", "الفترة المحدودة");
        cleanText = cleanText.replace("مبيعات ضعيفة", "فترة محدودة");
        cleanText = cleanText.replace("المبيعات الضعيفة", "الفترة المحدودة");

        cleanText = cleanText.replace("تصريف", "تجربة");
        cleanText = cleanText.replace("الفائض", "المميز");
        cleanText = cleanText.replace("فائض", "مميز");
        cleanText = cleanText.replace("منتجات راكدة", "منتجات مختارة");

        return cleanText.trim();
    }

    private String alignCustomerTextSchedule(String text,
                                             LocalDate suggestedStartDate,
                                             LocalTime suggestedStartTime,
                                             LocalTime suggestedEndTime,
                                             LocalDate today) {

        if (text == null || text.isBlank()) {
            return text;
        }

        String cleanText = sanitizeCustomerFacingText(text);

        cleanText = cleanText.replaceAll(
                "\\s*(?:من|بين)?\\s*\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:-|–|—|إلى|الى|حتى|to)\\s*\\d{1,2}:\\d{2}(?::\\d{2})?",
                ""
        );

        cleanText = cleanText.replace("اليوم فقط", "لفترة محدودة");
        cleanText = cleanText.replace("اليوم", "لفترة محدودة");
        cleanText = cleanText.replace("غدًا", "لفترة محدودة");
        cleanText = cleanText.replace("غدا", "لفترة محدودة");
        cleanText = cleanText.replace("بكرة", "لفترة محدودة");

        cleanText = cleanText.replaceAll("\\s{2,}", " ");
        cleanText = cleanText.replace(" ،", "،");
        cleanText = cleanText.replace(" !", "!");
        cleanText = cleanText.replace(" .", ".");
        cleanText = cleanText.replace("،!", "!");
        cleanText = cleanText.replace("،.", ".");

        return cleanText.trim();
    }

    private Integer normalizeTargetCustomersCount(CampaignType campaignType, Integer aiTargetCustomersCount) {
        int min;
        int max;
        int defaultValue;

        if (campaignType == CampaignType.QUESTION_BASED) {
            min = 40;
            max = 80;
            defaultValue = 50;
        } else {
            min = 80;
            max = 150;
            defaultValue = 100;
        }

        if (aiTargetCustomersCount == null || aiTargetCustomersCount <= 0) {
            return defaultValue;
        }

        if (aiTargetCustomersCount < min) {
            return min;
        }

        if (aiTargetCustomersCount > max) {
            return max;
        }

        return aiTargetCustomersCount;
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
                You are a business analyst for a retail platform.
                Analyze the following monthly report data and write an executive summary in English for the store owner.

                Rules:
                - Use only the provided data, do not invent numbers or information.
                - Write a clear paragraph of 3 to 5 sentences.
                - Mention sales performance, strongest and weakest products, and peak/slow hours.
                - End with one or two practical recommendations to improve sales.
                - Return plain English text only, no JSON and no Markdown.

                Store: %s
                Branch: %s
                Month: %s
                Year: %s
                Total Sales (SAR): %.2f
                Total Quantity Sold: %s
                Top Products: %s
                Low Products: %s
                Products Suggested for Promotion: %s
                Peak Hour: %s
                Slow Hour: %s
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

    public String generateMonthlyReportComparisonSummary(
            String storeName,
            String branchName,
            // بيانات الشهر الحالي
            Integer currentMonth,
            Integer currentYear,
            Double  currentTotalSales,
            Integer currentTotalQuantity,
            String  currentTopProducts,
            String  currentLowProducts,
            String  currentSurplusProducts,
            String  currentPeakHours,
            String  currentSlowHours,
            // بيانات الشهر السابق
            Integer previousMonth,
            Integer previousYear,
            Double  previousTotalSales,
            Integer previousTotalQuantity,
            String  previousTopProducts,
            String  previousLowProducts
    ) {
        validateApiKey();
        validateText(storeName,  "Store name is required");
        validateText(branchName, "Branch name is required");

        double salesChangePercent = previousTotalSales > 0
                ? ((currentTotalSales - previousTotalSales) / previousTotalSales) * 100
                : 0;

        String salesTrend = salesChangePercent >= 0
                ? String.format("increased by %.1f%%", salesChangePercent)
                : String.format("decreased by %.1f%%", Math.abs(salesChangePercent));

        String prompt = """
                You are a business analyst for a retail platform.
                Compare the performance of the following two months and write an executive summary in English for the store owner.

                Rules:
                - Use only the provided data, do not invent numbers.
                - Write a paragraph of 4 to 6 sentences.
                - Compare sales, quantities, and products between the two months.
                - Clarify whether performance improved or declined and why.
                - End with one or two practical recommendations for next month.
                - Return plain English text only, no JSON and no Markdown.

                Store: %s
                Branch: %s

                === Current Month: %s/%s ===
                Total Sales: %.2f SAR
                Total Quantity: %d units
                Top Products: %s
                Low Products: %s
                Products Suggested for Promotion: %s
                Peak Hour: %s
                Slow Hour: %s

                === Previous Month: %s/%s ===
                Total Sales: %.2f SAR
                Total Quantity: %d units
                Top Products: %s
                Low Products: %s

                === Quick Comparison ===
                Sales %s (from %.2f to %.2f SAR)
                """.formatted(
                storeName, branchName,
                currentMonth, currentYear,
                currentTotalSales, currentTotalQuantity,
                currentTopProducts, currentLowProducts,
                currentSurplusProducts, currentPeakHours, currentSlowHours,
                previousMonth, previousYear,
                previousTotalSales, previousTotalQuantity,
                previousTopProducts, previousLowProducts,
                salesTrend, previousTotalSales, currentTotalSales
        );

        String summary = extractAssistantContent(sendPrompt(prompt)).trim();
        validateText(summary, "OpenAI returned an empty comparison summary");
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