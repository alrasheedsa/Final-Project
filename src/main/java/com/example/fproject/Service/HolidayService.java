package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HolidayService {

    @Value("${holiday.api.base-url:https://date.nager.at/api/v3}")
    private String holidayApiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Map<String, Object>> getPublicHolidays(Integer year, String countryCode) {
        if (year == null) {
            throw new ApiException("Year is required");
        }

        if (countryCode == null || countryCode.isBlank()) {
            throw new ApiException("Country code is required");
        }

        String url = holidayApiBaseUrl
                + "/PublicHolidays/"
                + year
                + "/"
                + countryCode.trim().toUpperCase();

        Object[] response = restTemplate.getForObject(url, Object[].class);

        if (response == null) {
            throw new ApiException("Failed to fetch public holidays");
        }

        List<Map<String, Object>> holidays = new ArrayList<>();

        for (Object item : response) {
            Map<String, Object> holiday = (Map<String, Object>) item;
            holidays.add(holiday);
        }

        return holidays;
    }

    public Map<String, Object> checkHoliday(String dateText, String countryCode) {
        if (dateText == null || dateText.isBlank()) {
            throw new ApiException("Date is required");
        }

        LocalDate date = LocalDate.parse(dateText);

        List<Map<String, Object>> holidays =
                getPublicHolidays(date.getYear(), countryCode);

        for (Map<String, Object> holiday : holidays) {
            Object holidayDate = holiday.get("date");

            if (holidayDate != null && holidayDate.toString().equals(date.toString())) {
                Map<String, Object> result = new HashMap<>();
                result.put("date", date.toString());
                result.put("isHoliday", true);
                result.put("holiday", holiday);
                return result;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", date.toString());
        result.put("isHoliday", false);
        result.put("holiday", null);

        return result;
    }

    public String getSaudiHolidayContextForAI() {
        try {
            Integer currentYear = LocalDate.now().getYear();
            List<Map<String, Object>> holidays = getPublicHolidays(currentYear, "SA");

            StringBuilder context = new StringBuilder();
            context.append("Saudi public holidays for ").append(currentYear).append(":\n");

            for (Map<String, Object> holiday : holidays) {
                context.append("- Date: ")
                        .append(holiday.get("date"))
                        .append(", Local name: ")
                        .append(holiday.get("localName"))
                        .append(", English name: ")
                        .append(holiday.get("name"))
                        .append("\n");
            }

            context.append("Use public holidays as a business signal when suggesting campaign timing and offer strength.\n");

            return context.toString();

        } catch (Exception e) {
            return "Holiday API is currently unavailable. Continue campaign suggestion without holiday data.";
        }
    }
}
