package com.aeronix.passenger_service.controller;

import com.aeronix.passenger_service.dto.*;
import com.aeronix.passenger_service.entity.Passenger;
import com.aeronix.passenger_service.service.PassengerService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    // ── Add Passengers ───────────────────────────────────────

    @PostMapping
    public ResponseEntity<Passenger> addPassenger(
            @Valid @RequestBody PassengerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(passengerService.addPassenger(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Passenger>> addPassengers(
            @Valid @RequestBody BulkPassengerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(passengerService.addPassengers(request));
    }

    // ── Get Passengers ───────────────────────────────────────

    @GetMapping("/{passengerId}")
    public ResponseEntity<Passenger> getById(@PathVariable Integer passengerId) {
        return passengerService.getPassengerById(passengerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<Passenger>> getByBooking(
            @PathVariable String bookingId) {
        return ResponseEntity.ok(passengerService.getPassengersByBooking(bookingId));
    }

    @GetMapping("/booking/{bookingId}/count")
    public ResponseEntity<Map<String, Integer>> getCount(
            @PathVariable String bookingId) {
        return ResponseEntity.ok(Map.of("count",
                passengerService.getPassengerCount(bookingId)));
    }

    @GetMapping("/passport/{passportNumber}")
    public ResponseEntity<Passenger> getByPassport(
            @PathVariable String passportNumber,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return passengerService.getByPassportNumber(passportNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ticket/{ticketNumber}")
    public ResponseEntity<Passenger> getByTicket(
            @PathVariable String ticketNumber) {
        return passengerService.getByTicketNumber(ticketNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Update ───────────────────────────────────────────────

    @PutMapping("/{passengerId}")
    public ResponseEntity<Passenger> updatePassenger(
            @PathVariable Integer passengerId,
            @RequestBody UpdatePassengerRequest request) {
        return ResponseEntity.ok(
                passengerService.updatePassenger(passengerId, request));
    }

    @PutMapping("/{passengerId}/seat")
    public ResponseEntity<Passenger> assignSeat(
            @PathVariable Integer passengerId,
            @Valid @RequestBody SeatAssignRequest request) {
        return ResponseEntity.ok(
                passengerService.assignSeat(passengerId, request));
    }

    @PutMapping("/{passengerId}/checkin")
    public ResponseEntity<Passenger> checkIn(
            @PathVariable Integer passengerId) {
        return ResponseEntity.ok(
                passengerService.checkInPassenger(passengerId));
    }

    // ── Delete ───────────────────────────────────────────────

    @DeleteMapping("/{passengerId}")
    public ResponseEntity<Map<String, String>> deletePassenger(
            @PathVariable Integer passengerId) {
        passengerService.deletePassenger(passengerId);
        return ResponseEntity.ok(Map.of("message", "Passenger deleted"));
    }

    @DeleteMapping("/booking/{bookingId}")
    public ResponseEntity<Map<String, String>> deleteByBooking(
            @PathVariable String bookingId) {
        passengerService.deletePassengersByBooking(bookingId);
        return ResponseEntity.ok(Map.of("message",
                "All passengers deleted for booking " + bookingId));
    }

    // ── Validation ───────────────────────────────────────────

    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validate(
            @RequestBody PassengerRequest request) {
        return ResponseEntity.ok(
                passengerService.validatePassengerData(request));
    }

    // ── Airline Staff — Manifest ─────────────────────────────

    @GetMapping("/flight/{flightId}/manifest")
    public ResponseEntity<List<ManifestEntry>> getManifest(
            @PathVariable Integer flightId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(passengerService.getFlightManifest(flightId));
    }

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<Passenger>> getByFlight(
            @PathVariable Integer flightId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(passengerService.getPassengersByFlight(flightId));
    }
}