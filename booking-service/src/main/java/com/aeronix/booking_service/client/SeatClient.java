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
public class SeatClient {

    private final RestTemplate restTemplate;

    @Value("${seat.service.url}")
    private String seatServiceUrl;

    public void holdSeat(Integer seatId, String userId, Integer flightId) {
        try {
            Map<String, Object> body = Map.of(
                    "userId", userId,
                    "flightId", flightId
            );
            restTemplate.put(
                    seatServiceUrl + "/api/seats/" + seatId + "/hold", body);
        } catch (Exception e) {
            log.error("Failed to hold seat {}: {}", seatId, e.getMessage());
            throw new RuntimeException("Failed to hold seat " + seatId + ": " + e.getMessage());
        }
    }

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

    public void releaseUserHolds(Integer flightId, String userId) {
        try {
            restTemplate.put(
                    seatServiceUrl + "/api/seats/flight/" + flightId +
                            "/release-user?userId=" + userId, null);
        } catch (Exception e) {
            log.error("Failed to release user holds: {}", e.getMessage());
        }
    }
}