package com.aeronix.airline_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AirportSearchResponse {
    private Integer airportId;
    private String name;
    private String iataCode;
    private String icaoCode;
    private String city;
    private String country;
    private String timezone;
    private Double latitude;
    private Double longitude;
    private String displayName; // "Delhi (DEL), India"
}