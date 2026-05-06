package com.aeronix.airline_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AirportRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Size(min = 3, max = 3)
    private String iataCode;

    @Size(max = 4)
    private String icaoCode;

    @NotBlank
    private String city;

    @NotBlank
    private String country;

    private Double latitude;
    private Double longitude;
    private String timezone;
    private String terminal;
}
