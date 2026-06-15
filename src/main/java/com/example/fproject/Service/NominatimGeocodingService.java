package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NominatimGeocodingService {

    @Value("${nominatim.base-url}")
    private String baseUrl;

    private final RestClient.Builder restClientBuilder;

    public NominatimGeocodingService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public String geocodeAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new ApiException("Address is required");
        }
        return restClientBuilder.build()
                .get()
                .uri(baseUrl + "/search?q={address}&format=json&limit=1", address)
                .header("User-Agent", "F-Project")
                .retrieve()
                .body(String.class);
    }

    public String reverseGeocode(Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        return restClientBuilder.build()
                .get()
                .uri(baseUrl + "/reverse?lat={latitude}&lon={longitude}&format=json", latitude, longitude)
                .header("User-Agent", "F-Project")
                .retrieve()
                .body(String.class);
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new ApiException("Latitude and longitude are required");
        }
    }
}
