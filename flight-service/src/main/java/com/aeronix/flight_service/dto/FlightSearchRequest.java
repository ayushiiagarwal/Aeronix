package com.aeronix.flight_service.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FlightSearchRequest {
    private String origin;
    private String destination;
    private LocalDate departureDate;
    private int passengers = 1;

    // Filters
    private Double minPrice;
    private Double maxPrice;
    private Integer airlineId;
    private String seatClass;     // ECONOMY, BUSINESS, FIRST
    private Integer maxStops;
    private String sortBy;        // price, duration, departure
}