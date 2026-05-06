package com.aeronix.seat_service.dto;

import lombok.Data;

@Data
public class HoldRequest {
    private String userId;
    private Integer flightId;
}