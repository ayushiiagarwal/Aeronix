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
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    public void sendPaymentSuccess(Integer userId, String bookingId,
                                   String pnrCode, Double amount) {
        try {
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications/send",
                    Map.of(
                            "recipientId", userId,
                            "bookingId",   bookingId,
                            "type",        "BOOKING_CONFIRMED",
                            "title",       "Payment Successful",
                            "message",     "Payment of INR " + amount +
                                    " received. PNR: " + pnrCode
                    ), Void.class);
        } catch (Exception e) {
            log.warn("Payment success notification failed: {}", e.getMessage());
        }
    }

    public void sendPaymentFailed(Integer userId, String bookingId, Double amount) {
        try {
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications/send",
                    Map.of(
                            "recipientId", userId,
                            "bookingId",   bookingId,
                            "type",        "PAYMENT_FAILED",
                            "title",       "Payment Failed",
                            "message",     "Payment of INR " + amount +
                                    " failed. Please retry."
                    ), Void.class);
        } catch (Exception e) {
            log.warn("Payment failed notification failed: {}", e.getMessage());
        }
    }

    public void sendRefundInitiated(Integer userId, String bookingId,
                                    Double refundAmount) {
        try {
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications/send",
                    Map.of(
                            "recipientId", userId,
                            "bookingId",   bookingId,
                            "type",        "CANCELLATION",
                            "title",       "Refund Initiated",
                            "message",     "Refund of INR " + refundAmount +
                                    " initiated. Credit in 5-7 working days."
                    ), Void.class);
        } catch (Exception e) {
            log.warn("Refund notification failed: {}", e.getMessage());
        }
    }
}