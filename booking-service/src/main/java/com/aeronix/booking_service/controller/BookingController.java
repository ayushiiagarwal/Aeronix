package com.aeronix.booking_service.controller;

import com.aeronix.booking_service.dto.*;
import com.aeronix.booking_service.entity.Booking;
import com.aeronix.booking_service.service.BookingService;
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
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ── Passenger ────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, userId));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getById(@PathVariable String bookingId) {
        return bookingService.getBookingById(bookingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pnr/{pnrCode}")
    public ResponseEntity<Booking> getByPnr(@PathVariable String pnrCode) {
        return bookingService.getBookingByPnr(pnrCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my")
    public ResponseEntity<List<Booking>> getMyBookings(
            @RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    @GetMapping("/my/upcoming")
    public ResponseEntity<List<Booking>> getUpcoming(
            @RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(bookingService.getUpcomingBookings(userId));
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable String bookingId,
            @RequestBody CancelRequest request,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") String role) {
        // Admin can cancel any booking; passenger only their own
        if (role.equals("ADMIN")) {
            return ResponseEntity.ok(
                    bookingService.cancelBooking(bookingId, request, null));
        }
        return ResponseEntity.ok(
                bookingService.cancelBooking(bookingId, request, userId));
    }

    @PostMapping("/checkin")
    public ResponseEntity<Booking> webCheckIn(
            @Valid @RequestBody CheckInRequest request,
            @RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(bookingService.webCheckIn(request, userId));
    }

    @PostMapping("/{bookingId}/addons")
    public ResponseEntity<Booking> addAddOn(
            @PathVariable String bookingId,
            @Valid @RequestBody AddOnRequest request,
            @RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(bookingService.addAddOn(bookingId, request));
    }

    // ── Fare Calculation (Public) ────────────────────────────

    @GetMapping("/fare")
    public ResponseEntity<FareSummary> calculateFare(
            @RequestParam Integer flightId,
            @RequestParam String seatClass,
            @RequestParam(defaultValue = "1") int passengerCount,
            @RequestParam(required = false) Double extraLuggage) {
        return ResponseEntity.ok(bookingService.calculateFare(
                flightId, seatClass, passengerCount, extraLuggage));
    }

    // ── Airline Staff ────────────────────────────────────────

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<Booking>> getByFlight(
            @PathVariable Integer flightId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(bookingService.getBookingsByFlight(flightId));
    }

    @GetMapping("/flight/{flightId}/revenue")
    public ResponseEntity<Map<String, Object>> getFlightRevenue(
            @PathVariable Integer flightId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(Map.of(
                "flightId",         flightId,
                "totalRevenue",     bookingService.sumRevenueByFlight(flightId),
                "confirmedBookings", bookingService.countConfirmedByFlight(flightId)
        ));
    }

    // ── Admin ────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings(
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Booking>> getByStatus(
            @PathVariable String status,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(bookingService.getBookingsByStatus(status));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getPlatformAnalytics(
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(Map.of(
                "totalBookings", bookingService.countTotalBookings(),
                "totalRevenue",  bookingService.sumTotalRevenue()
        ));
    }

    // ── Internal / Payment Service Callback ─────────────────

    @PutMapping("/{bookingId}/confirm")
    public ResponseEntity<Booking> confirmAfterPayment(
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                bookingService.confirmBookingAfterPayment(
                        bookingId, body.get("paymentId")));
    }

    @PutMapping("/{bookingId}/status")
    public ResponseEntity<Booking> updateStatus(
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                bookingService.updateStatus(bookingId, body.get("status")));
    }
}