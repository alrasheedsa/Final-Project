package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class WathqService {

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wathq.api-key}")
    private String apiKey;

    @Value("${wathq.base-url:https://api.wathq.sa}")
    private String baseUrl;

    public void validateCommercialRegistration(String commercialRegisterNo) {

        String normalized = validateAndNormalize(commercialRegisterNo);
        JsonNode json = callWathqApi(normalized);
        verifyStatus(json);
        verifyNotExpired(json);
    }


    public String getCompanyName(String commercialRegisterNo) {

        String normalized = validateAndNormalize(commercialRegisterNo);
        JsonNode json = callWathqApi(normalized);

        JsonNode nameNode = json.get("crName");
        if (nameNode == null || nameNode.isNull()) {
            return null;
        }
        return nameNode.asText().trim();
    }

    /** يتحقق من صيغة الرقم ويُرجعه بعد trim */
    private String validateAndNormalize(String commercialRegisterNo) {

        if (commercialRegisterNo == null || commercialRegisterNo.isBlank()) {
            throw new ApiException("Commercial register number is required");
        }

        String normalized = commercialRegisterNo.trim();

        if (!normalized.matches("\\d{10}")) {
            throw new ApiException("Commercial register number must be exactly 10 digits");
        }

        return normalized;
    }

    private void verifyStatus(JsonNode json) {

        JsonNode statusNode = json.get("crStatus");

        if (statusNode == null || statusNode.isNull() || statusNode.asText().isBlank()) {
            throw new ApiException("Wathq did not return a status for this commercial register");
        }

        String crStatus = statusNode.asText().trim();

        if (!"نشط".equals(crStatus)) {
            throw new ApiException(
                    "Commercial registration is not active. Current status: " + crStatus
            );
        }
    }

    private void verifyNotExpired(JsonNode json) {

        JsonNode expiryNode = json.get("expiryDate");

        if (expiryNode == null || expiryNode.isNull() || expiryNode.asText().isBlank()) {
            return;
        }

        try {
            LocalDate expiryDate = LocalDate.parse(expiryNode.asText().trim());

            if (expiryDate.isBefore(LocalDate.now())) {
                throw new ApiException(
                        "Commercial registration has expired on: " + expiryDate
                );
            }

        } catch (DateTimeParseException e) {
            // لو التاريخ بصيغة غير متوقعة نتجاوز هذا التحقق بدل ما نوقف العملية
        }
    }



    private JsonNode callWathqApi(String crNumber) {

        String url = baseUrl + "/commercial-registration/info/" + crNumber + "?language=ar";

        HttpHeaders headers = new HttpHeaders();
        headers.set("apiKey", apiKey);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return parseBody(response.getBody());

        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiException("Commercial register number was not found in Wathq");

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ApiException("Wathq API key is invalid or expired");

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ApiException("Wathq API rate limit exceeded. Please try again later");

        } catch (HttpClientErrorException e) {
            throw new ApiException("Wathq API rejected the request: " + e.getStatusCode());

        } catch (ResourceAccessException e) {
            throw new ApiException("Could not reach Wathq API. Please check your connection");

        } catch (ApiException e) {
            throw e;

        } catch (Exception e) {
            throw new ApiException("Wathq API failed: " + e.getMessage());
        }
    }

    private JsonNode parseBody(String body) {

        if (body == null || body.isBlank()) {
            throw new ApiException("Empty response from Wathq API");
        }

        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new ApiException("Wathq API returned invalid JSON");
        }
    }
}