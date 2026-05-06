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
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    public void sendBookingConfirmation(Integer userId, String bookingId,
                                        String pnrCode, String email, String phone,
                                        String flightNumber, String origin,
                                        String destination, String departureTime) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("recipientId",   userId);
            body.put("bookingId",     bookingId);
            body.put("pnrCode",       pnrCode);
            body.put("email",         email);
            body.put("phone",         phone);
            body.put("flightNumber",  flightNumber);
            body.put("origin",        origin);
            body.put("destination",   destination);
            body.put("departureTime", departureTime);
            body.put("type",          "BOOKING_CONFIRMED");
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications/booking-confirmation",
                    body, Void.class);
        } catch (Exception e) {
            log.warn("Notification dispatch failed (non-critical): {}", e.getMessage());
        }
    }

    public void sendCancellationNotification(Integer userId, String bookingId,
                                             String pnrCode, String email) {
        try {
            Map<String, Object> body = Map.of(
                    "recipientId", userId,
                    "bookingId",   bookingId,
                    "pnrCode",     pnrCode,
                    "email",       email,
                    "type",        "CANCELLATION"
            );
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications/send",
                    body, Void.class);
        } catch (Exception e) {
            log.warn("Cancellation notification failed (non-critical): {}", e.getMessage());
        }
    }

    public void sendCheckinReminder(Integer userId, String bookingId, String email) {
        try {
            Map<String, Object> body = Map.of(
                    "recipientId", userId,
                    "bookingId",   bookingId,
                    "email",       email,
                    "type",        "CHECKIN_REMINDER"
            );
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications/send",
                    body, Void.class);
        } catch (Exception e) {
            log.warn("Check-in reminder failed (non-critical): {}", e.getMessage());
        }
    }
}