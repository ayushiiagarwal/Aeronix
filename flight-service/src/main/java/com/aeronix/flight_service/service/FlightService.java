package com.aeronix.flight_service.service;

import com.aeronix.flight_service.dto.*;
import com.aeronix.flight_service.entity.Flight;

import java.util.List;
import java.util.Optional;

public interface FlightService {
    Flight addFlight(FlightRequest request);
    Optional<Flight> getFlightById(Integer flightId);
    Optional<Flight> getFlightByNumber(String flightNumber);
    List<Flight> searchFlights(FlightSearchRequest request);
    RoundTripResponse searchRoundTrip(RoundTripSearchRequest request);
    Flight updateFlight(Integer flightId, FlightRequest request);
    void updateStatus(Integer flightId, StatusUpdateRequest request);
    void decrementSeats(Integer flightId, int count);
    void incrementSeats(Integer flightId, int count);
    void deleteFlight(Integer flightId);
    List<Flight> getFlightsByAirline(Integer airlineId);
    List<Flight> getFlightsByStatus(String status);
    List<Flight> getAllFlights();
    long countByAirline(Integer airlineId);
}