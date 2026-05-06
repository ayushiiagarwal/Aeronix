package com.aeronix.airline_service.service;

import com.aeronix.airline_service.dto.*;
import com.aeronix.airline_service.entity.Airline;
import com.aeronix.airline_service.entity.Airport;

import java.util.List;
import java.util.Optional;

public interface AirlineService {

    // Airline operations
    Airline createAirline(AirlineRequest request);
    Optional<Airline> getAirlineById(Integer airlineId);
    Optional<Airline> getAirlineByIata(String iataCode);
    List<Airline> getAllAirlines();
    List<Airline> getActiveAirlines();
    Airline updateAirline(Integer airlineId, AirlineRequest request);
    void deactivateAirline(Integer airlineId);
    void activateAirline(Integer airlineId);
    void deleteAirline(Integer airlineId);
    List<Airline> searchAirlines(String query);
    AirlineResponse getAirlineStats(Integer airlineId);

    // Airport operations
    Airport createAirport(AirportRequest request);
    Optional<Airport> getAirportById(Integer airportId);
    Optional<Airport> getAirportByIata(String iataCode);
    List<Airport> searchAirports(String query);
    List<Airport> getAirportsByCity(String city);
    List<Airport> getAirportsByCountry(String country);
    List<Airport> getAllAirports();
    List<Airport> getActiveAirports();
    Airport updateAirport(Integer airportId, AirportRequest request);
    void deactivateAirport(Integer airportId);
    List<AirportSearchResponse> searchAirportsForAutocomplete(String query);

    void activateAirport(Integer airportId);
}