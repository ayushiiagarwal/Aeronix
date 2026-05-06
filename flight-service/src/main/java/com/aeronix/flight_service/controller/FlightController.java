package com.aeronix.flight_service.controller;

import com.aeronix.flight_service.dto.*;
import com.aeronix.flight_service.entity.Flight;
import com.aeronix.flight_service.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    // ── Public / Guest ───────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<List<Flight>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String date,
            @RequestParam(defaultValue = "1") int passengers,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer airlineId,
            @RequestParam(required = false) String seatClass,
            @RequestParam(required = false) Integer maxStops,
            @RequestParam(required = false) String sortBy) {

        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin(origin);
        req.setDestination(destination);
        req.setDepartureDate(parseDate(date));
        req.setPassengers(passengers);
        req.setMinPrice(minPrice);
        req.setMaxPrice(maxPrice);
        req.setAirlineId(airlineId);
        req.setSeatClass(seatClass);
        req.setMaxStops(maxStops);
        req.setSortBy(sortBy);

        return ResponseEntity.ok(flightService.searchFlights(req));
    }

    @GetMapping("/round-trip")
    public ResponseEntity<RoundTripResponse> searchRoundTrip(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String departureDate,
            @RequestParam String returnDate,
            @RequestParam(defaultValue = "1") int passengers,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer airlineId,
            @RequestParam(required = false) String seatClass,
            @RequestParam(required = false) Integer maxStops) {

        RoundTripSearchRequest req = new RoundTripSearchRequest();
        req.setOrigin(origin);
        req.setDestination(destination);
        req.setDepartureDate(parseDate(departureDate));
        req.setReturnDate(parseDate(returnDate));
        req.setPassengers(passengers);
        req.setMinPrice(minPrice);
        req.setMaxPrice(maxPrice);
        req.setAirlineId(airlineId);
        req.setSeatClass(seatClass);
        req.setMaxStops(maxStops);

        return ResponseEntity.ok(flightService.searchRoundTrip(req));
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        }
    }

    @GetMapping("/{flightId}")
    public ResponseEntity<Flight> getById(@PathVariable Integer flightId) {
        return flightService.getFlightById(flightId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{flightNumber}")
    public ResponseEntity<Flight> getByNumber(@PathVariable String flightNumber) {
        return flightService.getFlightByNumber(flightNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Flight>> getAll() {

        return ResponseEntity.ok(flightService.getAllFlights());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Flight>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(flightService.getFlightsByStatus(status));
    }

    // ── Airline Staff ────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Flight> addFlight(
            @Valid @RequestBody FlightRequest request,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flightService.addFlight(request));
    }

    @PutMapping("/{flightId}")
    public ResponseEntity<Flight> updateFlight(
            @PathVariable Integer flightId,
            @Valid @RequestBody FlightRequest request,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(flightService.updateFlight(flightId, request));
    }

    @PutMapping("/{flightId}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable Integer flightId,
            @RequestBody StatusUpdateRequest request,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        flightService.updateStatus(flightId, request);
        return ResponseEntity.ok(Map.of("message", "Flight status updated to " + request.getStatus()));
    }

    @DeleteMapping("/{flightId}")
    public ResponseEntity<Map<String, String>> deleteFlight(
            @PathVariable Integer flightId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        flightService.deleteFlight(flightId);
        return ResponseEntity.ok(Map.of("message", "Flight deleted"));
    }

    // ── Internal / Service-to-Service ───────────────────────

    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<List<Flight>> getByAirline(@PathVariable Integer airlineId) {
        return ResponseEntity.ok(flightService.getFlightsByAirline(airlineId));
    }

    @GetMapping("/airline/{airlineId}/count")
    public ResponseEntity<Map<String, Long>> countByAirline(@PathVariable Integer airlineId) {
        return ResponseEntity.ok(Map.of("count", flightService.countByAirline(airlineId)));
    }

    @PutMapping("/{flightId}/seats/decrement")
    public ResponseEntity<Map<String, String>> decrementSeats(
            @PathVariable Integer flightId,
            @RequestParam(defaultValue = "1") int count) {
        flightService.decrementSeats(flightId, count);
        return ResponseEntity.ok(Map.of("message", "Seats decremented"));
    }

    @PutMapping("/{flightId}/seats/increment")
    public ResponseEntity<Map<String, String>> incrementSeats(
            @PathVariable Integer flightId,
            @RequestParam(defaultValue = "1") int count) {
        flightService.incrementSeats(flightId, count);
        return ResponseEntity.ok(Map.of("message", "Seats incremented"));
    }
}