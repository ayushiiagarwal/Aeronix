package com.aeronix.flight_service.dto;

import lombok.Data;

@Data
public class StatusUpdateRequest {
    private String status;
    private String reason;
    private Integer delayMinutes;
}