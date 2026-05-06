package com.aeronix.seat_service.dto;

import com.aeronix.seat_service.entity.Seat;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SeatMapResponse {
    private Integer flightId;
    private Map<String, List<Seat>> seatsByClass; // ECONOMY, BUSINESS, FIRST
    private int totalAvailable;
    private int totalSeats;
}