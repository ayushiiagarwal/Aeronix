package com.aeronix.payment_service.client;

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

    public void confirmBooking(String bookingId, String paymentId) {
        try {
            restTemplate.put(
                    bookingServiceUrl + "/api/bookings/" + bookingId + "/confirm",
                    Map.of("paymentId", paymentId));
        } catch (Exception e) {
            log.error("Failed to confirm booking {}: {}", bookingId, e.getMessage());
            throw new RuntimeException("Failed to confirm booking: " + e.getMessage());
        }
    }

    public void updateBookingStatus(String bookingId, String status) {
        try {
            restTemplate.put(
                    bookingServiceUrl + "/api/bookings/" + bookingId + "/status",
                    Map.of("status", status));
        } catch (Exception e) {
            log.error("Failed to update booking status {}: {}", bookingId, e.getMessage());
        }
    }
}