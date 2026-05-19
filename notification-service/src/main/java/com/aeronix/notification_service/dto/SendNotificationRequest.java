package com.aeronix.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendNotificationRequest {
    @NotNull(message = "Recipient ID is required")
    private Integer recipientId;

    @NotBlank(message = "Notification type is required")
    @Pattern(regexp = "BOOKING_CONFIRMED|BOOKING_CANCELLED|PAYMENT_SUCCESS|PAYMENT_FAILED|REFUND_INITIATED|CHECKIN_REMINDER|FLIGHT_DELAY|GATE_CHANGE|BROADCAST",
            message = "Invalid notification type")
    private String type;
    private String title;
    private String message;

    @Pattern(regexp = "APP|EMAIL|SMS|ALL", message = "channel must be APP, EMAIL, SMS, or ALL")
    private String channel;        // APP, EMAIL, SMS, ALL
    private String bookingId;

    @Email(message = "Invalid email address")
    private String email;
    private String phone;
    private String pnrCode;
}