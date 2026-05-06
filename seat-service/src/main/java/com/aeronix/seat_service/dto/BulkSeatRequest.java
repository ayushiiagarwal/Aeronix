package com.aeronix.seat_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BulkSeatRequest {

    @NotNull
    private Integer flightId;

    private List<SeatRequest> seats;

    // Auto-generate seats from config
    private Integer economyRows;
    private Integer businessRows;
    private Integer firstClassRows;
    private Integer seatsPerRow;   // default 6 (A-F)
}