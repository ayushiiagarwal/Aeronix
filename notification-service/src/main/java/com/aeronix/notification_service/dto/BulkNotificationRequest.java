package com.aeronix.notification_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkNotificationRequest {
    private List<Integer> recipientIds;
    private String role;            // PASSENGER, AIRLINE_STAFF, ALL
    private String type;
    private String title;
    private String message;
    private String channel;
    private String bookingId;
}