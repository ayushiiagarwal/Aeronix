package com.aeronix.airline_service.controller;

import com.aeronix.airline_service.dto.*;
import com.aeronix.airline_service.entity.*;
import com.aeronix.airline_service.service.AirlineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    // ── Public ───────────────────────────────────────────────

    @GetMapping("/api/airlines")
    public ResponseEntity<List<Airline>> getAllAirlines() {
        return ResponseEntity.ok(airlineService.getActiveAirlines());
    }

    @GetMapping("/api/airlines/all")
    public ResponseEntity<List<Airline>> getAllIncludingInactive(
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(airlineService.getAllAirlines());
    }

    @GetMapping("/api/airlines/{airlineId}")
    public ResponseEntity<Airline> getById(@PathVariable Integer airlineId) {
        return airlineService.getAirlineById(airlineId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/airlines/iata/{iataCode}")
    public ResponseEntity<Airline> getByIata(@PathVariable String iataCode) {
        return airlineService.getAirlineByIata(iataCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/airlines/search")
    public ResponseEntity<List<Airline>> searchAirlines(
            @RequestParam String query) {
        return ResponseEntity.ok(airlineService.searchAirlines(query));
    }

    @GetMapping("/api/airlines/{airlineId}/stats")
    public ResponseEntity<AirlineResponse> getStats(
            @PathVariable Integer airlineId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok(airlineService.getAirlineStats(airlineId));
    }

    // ── Admin Only ───────────────────────────────────────────

    @PostMapping("/api/airlines")
    public ResponseEntity<Airline> createAirline(
            @Valid @RequestBody AirlineRequest request,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(airlineService.createAirline(request));
    }

    @PutMapping("/api/airlines/{airlineId}")
    public ResponseEntity<Airline> updateAirline(
            @PathVariable Integer airlineId,
            @RequestBody AirlineRequest request,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(airlineService.updateAirline(airlineId, request));
    }

    @PutMapping("/api/airlines/{airlineId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivate(
            @PathVariable Integer airlineId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        airlineService.deactivateAirline(airlineId);
        return ResponseEntity.ok(Map.of("message", "Airline deactivated"));
    }

    @PutMapping("/api/airlines/{airlineId}/activate")
    public ResponseEntity<Map<String, String>> activate(
            @PathVariable Integer airlineId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        airlineService.activateAirline(airlineId);
        return ResponseEntity.ok(Map.of("message", "Airline activated"));
    }

    @DeleteMapping("/api/airlines/{airlineId}")
    public ResponseEntity<Map<String, String>> deleteAirline(
            @PathVariable Integer airlineId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        airlineService.deleteAirline(airlineId);
        return ResponseEntity.ok(Map.of("message", "Airline deleted"));
    }

    // ── Public ───────────────────────────────────────────────

    @GetMapping("/api/airports")
    public ResponseEntity<List<Airport>> getAllAirports() {
        return ResponseEntity.ok(airlineService.getActiveAirports());
    }

    @GetMapping("/api/airports/all")
    public ResponseEntity<List<Airport>> getAllAirportsIncludingInactive(
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(airlineService.getAllAirports());
    }

    @GetMapping("/api/airports/{airportId}")
    public ResponseEntity<Airport> getAirportById(
            @PathVariable Integer airportId) {
        return airlineService.getAirportById(airportId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/airports/iata/{iataCode}")
    public ResponseEntity<Airport> getAirportByIata(
            @PathVariable String iataCode) {
        return airlineService.getAirportByIata(iataCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Autocomplete — powers the flight search form
    @GetMapping("/api/airports/search")
    public ResponseEntity<List<AirportSearchResponse>> searchAirports(
            @RequestParam String query) {
        return ResponseEntity.ok(
                airlineService.searchAirportsForAutocomplete(query));
    }

    @GetMapping("/api/airports/search/raw")
    public ResponseEntity<List<Airport>> searchAirportsRaw(
            @RequestParam String query) {
        return ResponseEntity.ok(airlineService.searchAirports(query));
    }

    @GetMapping("/api/airports/city/{city}")
    public ResponseEntity<List<Airport>> getByCity(@PathVariable String city) {
        return ResponseEntity.ok(airlineService.getAirportsByCity(city));
    }

    @GetMapping("/api/airports/country/{country}")
    public ResponseEntity<List<Airport>> getByCountry(
            @PathVariable String country) {
        return ResponseEntity.ok(airlineService.getAirportsByCountry(country));
    }

    // ── Admin Only ───────────────────────────────────────────

    @PostMapping("/api/airports")
    public ResponseEntity<Airport> createAirport(
            @Valid @RequestBody AirportRequest request,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(airlineService.createAirport(request));
    }

    @PutMapping("/api/airports/{airportId}")
    public ResponseEntity<Airport> updateAirport(
            @PathVariable Integer airportId,
            @RequestBody AirportRequest request,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(airlineService.updateAirport(airportId, request));
    }

    @PutMapping("/api/airports/{airportId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateAirport(
            @PathVariable Integer airportId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        airlineService.deactivateAirport(airportId);
        return ResponseEntity.ok(Map.of("message", "Airport deactivated"));
    }

    @PutMapping("/api/airports/{airportId}/activate")
    public ResponseEntity<Map<String, String>> activateAirport(
            @PathVariable Integer airportId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        airlineService.activateAirport(airportId);
        return ResponseEntity.ok(Map.of("message", "Airport activated"));
    }
}