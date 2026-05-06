package com.aeronix.flight_service.dto;

import com.aeronix.flight_service.entity.Flight;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class RoundTripResponse {
    private List<Flight> outboundFlights;
    private List<Flight> returnFlights;
}