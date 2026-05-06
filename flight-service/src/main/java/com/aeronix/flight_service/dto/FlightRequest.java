package com.aeronix.flight_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlightRequest {

    @NotBlank
    private String flightNumber;

    @NotNull
    private Integer airlineId;

    @NotBlank
    private String originAirportCode;

    @NotBlank
    private String destinationAirportCode;

    @NotNull
    private LocalDateTime departureTime;

    @NotNull
    private LocalDateTime arrivalTime;

    private Integer durationMinutes;
    private String aircraftType;

    @NotNull
    private Integer totalSeats;

    @NotNull
    private Double basePrice;

    private Double economyPrice;
    private Double businessPrice;
    private Double firstClassPrice;

    private Integer stops;
}