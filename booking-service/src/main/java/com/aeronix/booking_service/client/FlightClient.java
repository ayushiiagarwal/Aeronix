package com.aeronix.booking_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlightClient {

    private final RestTemplate restTemplate;

    @Value("${flight.service.url}")
    private String flightServiceUrl;

    public void decrementSeats(Integer flightId, int count) {
        try {
            restTemplate.put(
                    flightServiceUrl + "/api/flights/" + flightId +
                            "/seats/decrement?count=" + count, null);
        } catch (Exception e) {
            log.error("Failed to decrement seats for flight {}: {}", flightId, e.getMessage());
        }
    }

    public void incrementSeats(Integer flightId, int count) {
        try {
            restTemplate.put(
                    flightServiceUrl + "/api/flights/" + flightId +
                            "/seats/increment?count=" + count, null);
        } catch (Exception e) {
            log.error("Failed to increment seats for flight {}: {}", flightId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getFlightById(Integer flightId) {
        try {
            return restTemplate.getForObject(
                    flightServiceUrl + "/api/flights/" + flightId, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch flight {}: {}", flightId, e.getMessage());
            return null;
        }
    }
}