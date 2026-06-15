package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OSRMRouteService {

    @Value("${osrm.base-url}")
    private String baseUrl;

    private final RestClient.Builder restClientBuilder;

    public OSRMRouteService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public String getRoute(Double startLatitude, Double startLongitude, Double endLatitude, Double endLongitude) {
        validateCoordinates(startLatitude, startLongitude, endLatitude, endLongitude);
        return restClientBuilder.build()
                .get()
                .uri(baseUrl + "/route/v1/driving/{startLongitude},{startLatitude};{endLongitude},{endLatitude}?overview=false",
                        startLongitude, startLatitude, endLongitude, endLatitude)
                .retrieve()
                .body(String.class);
    }

    public Double calculateDistanceKm(Double startLatitude, Double startLongitude, Double endLatitude, Double endLongitude) {
        validateCoordinates(startLatitude, startLongitude, endLatitude, endLongitude);
        return 0.0;
    }

    public Integer calculateDurationMinutes(Double startLatitude, Double startLongitude, Double endLatitude, Double endLongitude) {
        validateCoordinates(startLatitude, startLongitude, endLatitude, endLongitude);
        return 0;
    }

    public String buildDistanceText(Double distanceKm, Integer durationMinutes) {
        if (distanceKm == null || durationMinutes == null) {
            throw new ApiException("Distance and duration are required");
        }
        return distanceKm + " km / " + durationMinutes + " minutes";
    }

    private void validateCoordinates(Double startLatitude, Double startLongitude, Double endLatitude, Double endLongitude) {
        if (startLatitude == null || startLongitude == null || endLatitude == null || endLongitude == null) {
            throw new ApiException("Route coordinates are required");
        }
    }
}
