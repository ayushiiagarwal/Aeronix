package com.aeronix.booking_service.dto;

import lombok.Data;

@Data
public class CancelRequest {
    private String reason;
}