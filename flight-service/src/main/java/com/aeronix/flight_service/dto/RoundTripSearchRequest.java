package com.aeronix.flight_service.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RoundTripSearchRequest {
    private String origin;
    private String destination;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private int passengers = 1;

    // Filters
    private Double minPrice;
    private Double maxPrice;
    private Integer airlineId;
    private String seatClass;
    private Integer maxStops;
}