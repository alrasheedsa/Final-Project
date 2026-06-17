package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleMapService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    @Value("${google.maps.geocoding.base-url}")
    private String geocodingBaseUrl;

    @Value("${google.maps.routes.base-url}")
    private String routesBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public double[] extractLocationFromLink(String locationUrl) {
        if (locationUrl == null || locationUrl.isBlank()) {
            throw new ApiException("Location URL is required");
        }

        String finalUrl = expandShortUrl(locationUrl);

        double[] coordinates = extractCoordinatesFromUrl(finalUrl);
        if (coordinates != null) {
            return coordinates;
        }

        String address = extractAddressFromUrl(finalUrl);
        if (address == null || address.isBlank()) {
            throw new ApiException("Could not read location from URL");
        }

        return geocodeAddress(address);
    }

    public double[] geocodeAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new ApiException("Address is required");
        }

        String url = geocodingBaseUrl
                + "?address=" + address.replace(" ", "+")
                + "&key=" + apiKey;

        Map response = restTemplate.getForObject(url, Map.class);
        if (response == null || !"OK".equals(response.get("status"))) {
            throw new ApiException("Failed to geocode address");
        }

        List results = (List) response.get("results");
        if (results == null || results.isEmpty()) {
            throw new ApiException("No geocoding results found");
        }

        Map firstResult = (Map) results.get(0);
        Map geometry = (Map) firstResult.get("geometry");
        Map location = (Map) geometry.get("location");

        Double lat = ((Number) location.get("lat")).doubleValue();
        Double lng = ((Number) location.get("lng")).doubleValue();

        return new double[]{lat, lng};
    }

    public RouteResult calculateRoute(Double originLat, Double originLng, Double destinationLat, Double destinationLng) {
        if (originLat == null || originLng == null || destinationLat == null || destinationLng == null) {
            throw new ApiException("Origin and destination coordinates are required");
        }

        Map<String, Object> body = Map.of(
                "origin", Map.of(
                        "location", Map.of(
                                "latLng", Map.of(
                                        "latitude", originLat,
                                        "longitude", originLng
                                )
                        )
                ),
                "destination", Map.of(
                        "location", Map.of(
                                "latLng", Map.of(
                                        "latitude", destinationLat,
                                        "longitude", destinationLng
                                )
                        )
                ),
                "travelMode", "DRIVE",
                "routingPreference", "TRAFFIC_AWARE"
        );

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask", "routes.duration,routes.distanceMeters");

        org.springframework.http.HttpEntity<Map<String, Object>> request =
                new org.springframework.http.HttpEntity<>(body, headers);

        Map response = restTemplate.postForObject(routesBaseUrl, request, Map.class);
        if (response == null || response.get("routes") == null) {
            throw new ApiException("Failed to calculate route");
        }

        List routes = (List) response.get("routes");
        if (routes.isEmpty()) {
            throw new ApiException("No route found");
        }

        Map route = (Map) routes.get(0);

        Integer distanceMeters = ((Number) route.get("distanceMeters")).intValue();
        String durationText = route.get("duration").toString();

        Double distanceKm = distanceMeters / 1000.0;
        Integer durationMinutes = parseDurationMinutes(durationText);
        String distanceText = String.format("%.2f km", distanceKm);

        return new RouteResult(distanceKm, durationMinutes, distanceText);
    }

    public String buildGoogleMapsDirectionsLink(Double originLat, Double originLng, Double destinationLat, Double destinationLng) {
        if (originLat == null || originLng == null || destinationLat == null || destinationLng == null) {
            throw new ApiException("Origin and destination coordinates are required");
        }

        return "https://www.google.com/maps/dir/?api=1"
                + "&origin=" + originLat + "," + originLng
                + "&destination=" + destinationLat + "," + destinationLng
                + "&travelmode=driving";
    }

    public String buildGoogleMapsLocationLink(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new ApiException("Location coordinates are required");
        }

        return "https://www.google.com/maps/search/?api=1&query="
                + latitude + "," + longitude;
    }

    private String expandShortUrl(String locationUrl) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(locationUrl).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.connect();

            String redirectedUrl = connection.getHeaderField("Location");
            if (redirectedUrl == null) {
                return locationUrl;
            }

            return redirectedUrl;
        } catch (Exception e) {
            return locationUrl;
        }
    }

    private double[] extractCoordinatesFromUrl(String url) {
        try {
            String decodedUrl = URI.create(url).toString();

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(decodedUrl);

            if (matcher.find()) {
                Double lat = Double.parseDouble(matcher.group(1));
                Double lng = Double.parseDouble(matcher.group(2));
                return new double[]{lat, lng};
            }

            pattern = java.util.regex.Pattern.compile("q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            matcher = pattern.matcher(decodedUrl);

            if (matcher.find()) {
                Double lat = Double.parseDouble(matcher.group(1));
                Double lng = Double.parseDouble(matcher.group(2));
                return new double[]{lat, lng};
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractAddressFromUrl(String url) {
        try {
            if (!url.contains("/place/")) {
                return null;
            }

            String addressPart = url.substring(url.indexOf("/place/") + 7);
            if (addressPart.contains("/")) {
                addressPart = addressPart.substring(0, addressPart.indexOf("/"));
            }

            return java.net.URLDecoder.decode(addressPart.replace("+", " "), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseDurationMinutes(String durationText) {
        if (durationText == null || durationText.isBlank()) {
            return 0;
        }

        String secondsText = durationText.replace("s", "");
        Double seconds = Double.parseDouble(secondsText);
        return (int) Math.ceil(seconds / 60.0);
    }

    public record RouteResult(Double distanceKm, Integer durationMinutes, String distanceText) {
    }
}