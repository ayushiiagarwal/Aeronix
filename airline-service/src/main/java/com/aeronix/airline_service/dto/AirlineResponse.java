package com.aeronix.airline_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AirlineResponse {
    private Integer airlineId;
    private String name;
    private String iataCode;
    private long totalFlights;
    private boolean isActive;
}