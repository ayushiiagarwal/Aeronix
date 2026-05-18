package com.aeronix.airline_service.service;

import com.aeronix.airline_service.dto.*;
import com.aeronix.airline_service.entity.*;
import com.aeronix.airline_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirlineServiceImpl implements AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;
    private final RestTemplate restTemplate;

    @Value("${flight.service.url:http://localhost:8086}")
    private String flightServiceUrl;

    // ── Airline CRUD ─────────────────────────────────────────

    @Override
    @Transactional
    public Airline createAirline(AirlineRequest request) {
        if (airlineRepository.existsByIataCode(request.getIataCode().toUpperCase())) {
            throw new RuntimeException(
                    "Airline with IATA code already exists: " + request.getIataCode());
        }

        Airline airline = Airline.builder()
                .name(request.getName())
                .iataCode(request.getIataCode().toUpperCase())
                .icaoCode(request.getIcaoCode() != null
                        ? request.getIcaoCode().toUpperCase() : null)
                .logoUrl(request.getLogoUrl())
                .country(request.getCountry())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .website(request.getWebsite())
                .description(request.getDescription())
                .isActive(true)
                .build();

        return airlineRepository.save(airline);
    }

    @Override
    public Optional<Airline> getAirlineById(Integer airlineId) {
        return airlineRepository.findById(airlineId);
    }

    @Override
    public Optional<Airline> getAirlineByIata(String iataCode) {
        return airlineRepository.findByIataCode(iataCode.toUpperCase());
    }

    @Override
    public List<Airline> getAllAirlines() {
        return airlineRepository.findAll();
    }

    @Override
    public List<Airline> getActiveAirlines() {
        return airlineRepository.findByIsActive(true);
    }

    @Override
    @Transactional
    public Airline updateAirline(Integer airlineId, AirlineRequest request) {
        Airline airline = airlineRepository.findById(airlineId)
                .orElseThrow(() -> new RuntimeException(
                        "Airline not found: " + airlineId));

        if (request.getName() != null)
            airline.setName(request.getName());
        if (request.getLogoUrl() != null)
            airline.setLogoUrl(request.getLogoUrl());
        if (request.getCountry() != null)
            airline.setCountry(request.getCountry());
        if (request.getContactEmail() != null)
            airline.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null)
            airline.setContactPhone(request.getContactPhone());
        if (request.getWebsite() != null)
            airline.setWebsite(request.getWebsite());
        if (request.getDescription() != null)
            airline.setDescription(request.getDescription());
        if (request.getIcaoCode() != null)
            airline.setIcaoCode(request.getIcaoCode().toUpperCase());

        airline.setUpdatedAt(LocalDateTime.now());
        return airlineRepository.save(airline);
    }

    @Override
    @Transactional
    public void deactivateAirline(Integer airlineId) {
        Airline airline = airlineRepository.findById(airlineId)
                .orElseThrow(() -> new RuntimeException(
                        "Airline not found: " + airlineId));
        airline.setActive(false);
        airline.setUpdatedAt(LocalDateTime.now());
        airlineRepository.save(airline);
        log.info("Airline deactivated: {} ({})", airline.getName(), airline.getIataCode());
    }

    @Override
    @Transactional
    public void activateAirline(Integer airlineId) {
        Airline airline = airlineRepository.findById(airlineId)
                .orElseThrow(() -> new RuntimeException(
                        "Airline not found: " + airlineId));
        airline.setActive(true);
        airline.setUpdatedAt(LocalDateTime.now());
        airlineRepository.save(airline);
    }

    @Override
    @Transactional
    public void deleteAirline(Integer airlineId) {
        airlineRepository.deleteById(airlineId);
    }

    @Override
    public List<Airline> searchAirlines(String query) {
        return airlineRepository.searchAirlines(query);
    }

    @Override
    public AirlineResponse getAirlineStats(Integer airlineId) {
        Airline airline = airlineRepository.findById(airlineId)
                .orElseThrow(() -> new RuntimeException(
                        "Airline not found: " + airlineId));

        long flightCount = 0;
        try {
            Map<?, ?> resp = restTemplate.getForObject(
                    flightServiceUrl + "/api/flights/airline/" +
                            airlineId + "/count", Map.class);
            if (resp != null && resp.get("count") != null) {
                flightCount = ((Number) resp.get("count")).longValue();
            }
        } catch (Exception e) {
            log.warn("Could not fetch flight count for airline {}: {}",
                    airlineId, e.getMessage());
        }

        return AirlineResponse.builder()
                .airlineId(airlineId)
                .name(airline.getName())
                .iataCode(airline.getIataCode())
                .totalFlights(flightCount)
                .isActive(airline.isActive())
                .build();
    }

    // ── Airport CRUD ─────────────────────────────────────────

    @Override
    @Transactional
    public Airport createAirport(AirportRequest request) {
        if (airportRepository.existsByIataCode(request.getIataCode().toUpperCase())) {
            throw new RuntimeException(
                    "Airport with IATA code already exists: " + request.getIataCode());
        }

        Airport airport = Airport.builder()
                .name(request.getName())
                .iataCode(request.getIataCode().toUpperCase())
                .icaoCode(request.getIcaoCode() != null
                        ? request.getIcaoCode().toUpperCase() : null)
                .city(request.getCity())
                .country(request.getCountry())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timezone(request.getTimezone())
                .terminal(request.getTerminal())
                .isActive(true)
                .build();

        return airportRepository.save(airport);
    }

    @Override
    public Optional<Airport> getAirportById(Integer airportId) {
        return airportRepository.findById(airportId);
    }

    @Override
    public Optional<Airport> getAirportByIata(String iataCode) {
        return airportRepository.findByIataCode(iataCode.toUpperCase());
    }

    @Override
    public List<Airport> searchAirports(String query) {
        return airportRepository.searchAirports(query);
    }

    @Override
    public List<Airport> getAirportsByCity(String city) {
        return airportRepository.findByCityContaining(city);
    }

    @Override
    public List<Airport> getAirportsByCountry(String country) {
        return airportRepository.findActiveByCountry(country);
    }

    @Override
    public List<Airport> getAllAirports() {
        return airportRepository.findAll();
    }

    @Override
    public List<Airport> getActiveAirports() {
        return airportRepository.findByIsActive(true);
    }

    @Override
    @Transactional
    public Airport updateAirport(Integer airportId, AirportRequest request) {
        Airport airport = airportRepository.findById(airportId)
                .orElseThrow(() -> new RuntimeException(
                        "Airport not found: " + airportId));

        if (request.getName() != null)
            airport.setName(request.getName());
        if (request.getCity() != null)
            airport.setCity(request.getCity());
        if (request.getCountry() != null)
            airport.setCountry(request.getCountry());
        if (request.getLatitude() != null)
            airport.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)
            airport.setLongitude(request.getLongitude());
        if (request.getTimezone() != null)
            airport.setTimezone(request.getTimezone());
        if (request.getTerminal() != null)
            airport.setTerminal(request.getTerminal());
        if (request.getIcaoCode() != null)
            airport.setIcaoCode(request.getIcaoCode().toUpperCase());

        airport.setUpdatedAt(LocalDateTime.now());
        return airportRepository.save(airport);
    }

    @Override
    @Transactional
    public void deactivateAirport(Integer airportId) {
        Airport airport = airportRepository.findById(airportId)
                .orElseThrow(() -> new RuntimeException(
                        "Airport not found: " + airportId));
        airport.setActive(false);
        airport.setUpdatedAt(LocalDateTime.now());
        airportRepository.save(airport);
    }

    public void activateAirport(Integer airportId) {
        Airport airport = airportRepository.findById(airportId)
                .orElseThrow(() -> new RuntimeException("Airport not found: " + airportId));
        airport.setActive(true);  // or setIsActive(true) — match whatever deactivateAirport uses
        airportRepository.save(airport);
    }

    @Override
    public List<AirportSearchResponse> searchAirportsForAutocomplete(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        List<Airport> airports = airportRepository.searchAirports(query.trim());

        return airports.stream()
                .filter(Airport::isActive)
                .limit(10)
                .map(a -> AirportSearchResponse.builder()
                        .airportId(a.getAirportId())
                        .name(a.getName())
                        .iataCode(a.getIataCode())
                        .icaoCode(a.getIcaoCode())
                        .city(a.getCity())
                        .country(a.getCountry())
                        .timezone(a.getTimezone())
                        .latitude(a.getLatitude())
                        .longitude(a.getLongitude())
                        .displayName(buildDisplayName(a))
                        .build())
                .collect(Collectors.toList());
    }

    // ── Helpers ──────────────────────────────────────────────

    private String buildDisplayName(Airport airport) {
        return airport.getName() + " (" + airport.getIataCode() + "), " +
                airport.getCity() + ", " + airport.getCountry();
    }
}
