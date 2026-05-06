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
public class BookingClient {

    private final RestTemplate restTemplate;

    @Value("${booking.service.url}")
    private String bookingServiceUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getBookingById(String bookingId) {
        try {
            return restTemplate.getForObject(
                    bookingServiceUrl + "/api/bookings/" + bookingId, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch booking {}: {}", bookingId, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getBookingByPnr(String pnrCode) {
        try {
            return restTemplate.getForObject(
                    bookingServiceUrl + "/api/bookings/pnr/" + pnrCode, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch booking by PNR {}: {}", pnrCode, e.getMessage());
            return null;
        }
    }
}