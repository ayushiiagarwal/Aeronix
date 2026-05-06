package com.aeronix.notification_service.dto;

import lombok.Data;

@Data
public class SendNotificationRequest {
    private Integer recipientId;
    private String type;
    private String title;
    private String message;
    private String channel;        // APP, EMAIL, SMS, ALL
    private String bookingId;
    private String email;
    private String phone;
    private String pnrCode;
}