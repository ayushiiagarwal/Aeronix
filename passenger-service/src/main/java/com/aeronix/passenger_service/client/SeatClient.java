package com.aeronix.passenger_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatClient {

    private final RestTemplate restTemplate;

    @Value("${seat.service.url}")
    private String seatServiceUrl;

    public void confirmSeat(Integer seatId) {
        try {
            restTemplate.put(
                    seatServiceUrl + "/api/seats/" + seatId + "/confirm", null);
        } catch (Exception e) {
            log.error("Failed to confirm seat {}: {}", seatId, e.getMessage());
        }
    }

    public void releaseSeat(Integer seatId) {
        try {
            restTemplate.put(
                    seatServiceUrl + "/api/seats/" + seatId + "/release", null);
        } catch (Exception e) {
            log.error("Failed to release seat {}: {}", seatId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSeatById(Integer seatId) {
        try {
            return restTemplate.getForObject(
                    seatServiceUrl + "/api/seats/" + seatId, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch seat {}: {}", seatId, e.getMessage());
            return null;
        }
    }
}