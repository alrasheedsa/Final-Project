package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WathqService {

    @Value("${wathq.api-key}")
    private String apiKey;

    public void validateCommercialRegistration(String commercialRegisterNo) {
        try {
            if (commercialRegisterNo == null || commercialRegisterNo.isBlank()) {
                throw new ApiException("Commercial register number is required");
            }

            RestTemplate restTemplate = new RestTemplate();

            String url = "https://api.wathq.sa/sandbox/commercial-registration/status/"
                    + commercialRegisterNo
                    + "?language=ar";

            HttpHeaders headers = new HttpHeaders();
            headers.set("apiKey", apiKey);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ApiException("Wathq API failed with status: " + response.getStatusCode());
            }

            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new ApiException("Empty response from Wathq API");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.getBody());

            if (!json.has("name") || json.get("name").isNull()) {
                throw new ApiException("Wathq response does not contain commercial register status");
            }

            String status = json.get("name").asText();

            System.out.println("CR Status from Wathq: " + status);

            if (!"نشط".equals(status)) {
                throw new ApiException("Commercial Registration is not active: " + status);
            }

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Wathq API failed: " + e.getMessage());
        }
    }
}