package com.aeronix.seat_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeatRequest {

    @NotNull
    private Integer flightId;

    @NotBlank
    private String seatNumber;

    @NotBlank
    private String seatClass;   // ECONOMY, BUSINESS, FIRST

    @NotNull
    private Integer seatRow;

    @NotBlank
    private String seatColumn;

    private boolean isWindow;
    private boolean isAisle;
    private boolean hasExtraLegroom;
    private Double priceMultiplier;
}