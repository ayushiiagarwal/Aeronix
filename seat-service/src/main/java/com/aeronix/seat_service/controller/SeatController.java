package com.aeronix.seat_service.controller;

import com.aeronix.seat_service.dto.*;
import com.aeronix.seat_service.entity.Seat;
import com.aeronix.seat_service.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    // ── Airline Staff / Admin ────────────────────────────────

    @PostMapping
    public ResponseEntity<List<Seat>> addSeats(
            @Valid @RequestBody BulkSeatRequest request,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatService.addSeatsForFlight(request));
    }

    @PutMapping("/{seatId}")
    public ResponseEntity<Seat> updateSeat(
            @PathVariable Integer seatId,
            @RequestBody SeatRequest request,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(seatService.updateSeat(seatId, request));
    }

    @PutMapping("/{seatId}/block")
    public ResponseEntity<Map<String, String>> blockSeat(
            @PathVariable Integer seatId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        seatService.blockSeat(seatId);
        return ResponseEntity.ok(Map.of("message", "Seat blocked"));
    }

    @DeleteMapping("/flight/{flightId}")
    public ResponseEntity<Map<String, String>> deleteSeatsForFlight(
            @PathVariable Integer flightId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        seatService.deleteSeatsForFlight(flightId);
        return ResponseEntity.ok(Map.of("message", "All seats deleted for flight " + flightId));
    }

    // ── Passenger / Public ───────────────────────────────────

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<Seat>> getAllByFlight(@PathVariable Integer flightId) {
        return ResponseEntity.ok(seatService.getAllSeatsByFlight(flightId));
    }

    @GetMapping("/flight/{flightId}/available")
    public ResponseEntity<List<Seat>> getAvailable(@PathVariable Integer flightId) {
        return ResponseEntity.ok(seatService.getAvailableSeats(flightId));
    }

    @GetMapping("/flight/{flightId}/available/{seatClass}")
    public ResponseEntity<List<Seat>> getAvailableByClass(
            @PathVariable Integer flightId,
            @PathVariable String seatClass) {
        return ResponseEntity.ok(seatService.getAvailableByClass(flightId, seatClass));
    }

    @GetMapping("/flight/{flightId}/map")
    public ResponseEntity<SeatMapResponse> getSeatMap(@PathVariable Integer flightId) {
        return ResponseEntity.ok(seatService.getSeatMap(flightId));
    }

    @GetMapping("/flight/{flightId}/count/{seatClass}")
    public ResponseEntity<Map<String, Integer>> countAvailable(
            @PathVariable Integer flightId,
            @PathVariable String seatClass) {
        int count = seatService.countAvailableByClass(flightId, seatClass);
        return ResponseEntity.ok(Map.of("availableSeats", count));
    }

    @GetMapping("/{seatId}")
    public ResponseEntity<Seat> getById(@PathVariable Integer seatId) {
        return seatService.getSeatById(seatId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Internal / Service-to-Service ───────────────────────

    @PutMapping("/{seatId}/hold")
    public ResponseEntity<Seat> holdSeat(
            @PathVariable Integer seatId,
            @RequestBody HoldRequest request) {
        return ResponseEntity.ok(seatService.holdSeat(seatId, request));
    }

    @PutMapping("/{seatId}/release")
    public ResponseEntity<Map<String, String>> releaseSeat(@PathVariable Integer seatId) {
        seatService.releaseSeat(seatId);
        return ResponseEntity.ok(Map.of("message", "Seat released"));
    }

    @PutMapping("/{seatId}/confirm")
    public ResponseEntity<Map<String, String>> confirmSeat(@PathVariable Integer seatId) {
        seatService.confirmSeat(seatId);
        return ResponseEntity.ok(Map.of("message", "Seat confirmed"));
    }

    @PutMapping("/flight/{flightId}/release-user")
    public ResponseEntity<Map<String, String>> releaseUserHolds(
            @PathVariable Integer flightId,
            @RequestParam String userId) {
        seatService.releaseUserHoldsOnFlight(flightId, userId);
        return ResponseEntity.ok(Map.of("message", "User seat holds released"));
    }
}

/*
"accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQUlSTElORV9TVEFGRiIsImZ1bGxOYW1lIjoiQWVyb25peF9TdGFmZiIsImVtYWlsIjoiYWVyb25peHN0YWZmQGdtYWlsLmNvbSIsInN1YiI6IjEiLCJpYXQiOjE3NzY4NTA1NDksImV4cCI6MTc3NjkzNjk0OX0.qi-nUwnH0QIRQ7CcnAs0CUwYbDf57fq5JdbzKm9ZkWw",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzc2ODUwNTQ5LCJleHAiOjE3Nzc0NTUzNDl9.F34kbdCh3j1sYN9KLB5WzHf6U8BIqZr3mxVD0xbTOW0",
 */