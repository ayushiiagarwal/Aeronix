package com.aeronix.passenger_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeatAssignRequest {

    @NotNull
    private Integer seatId;

    @NotNull
    private String seatNumber;
}