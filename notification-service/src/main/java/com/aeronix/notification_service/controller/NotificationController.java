package com.aeronix.notification_service.controller;

import com.aeronix.notification_service.dto.*;
import com.aeronix.notification_service.entity.Notification;
import com.aeronix.notification_service.service.NotificationService;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── Internal — triggered by other services ───────────────

    @PostMapping("/send")
    public ResponseEntity<Notification> send(
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.send(request));
    }

    @PostMapping("/booking-confirmation")
    public ResponseEntity<Map<String, String>> sendBookingConfirmation(
            @RequestBody BookingConfirmationRequest request) {
        notificationService.sendBookingConfirmation(request);
        return ResponseEntity.ok(Map.of("message", "Booking confirmation sent"));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, String>> sendBulk(
            @RequestBody BulkNotificationRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        notificationService.sendBulk(request);
        return ResponseEntity.ok(Map.of(
                "message", "Bulk notification sent to " +
                        (request.getRecipientIds() != null
                                ? request.getRecipientIds().size() : 0) + " recipients"));
    }

    @PostMapping("/flight-alert")
    public ResponseEntity<Map<String, String>> sendFlightAlert(
            @RequestBody FlightAlertRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        notificationService.sendFlightAlert(request);
        return ResponseEntity.ok(Map.of("message", "Flight alert dispatched"));
    }

    // ── Passenger — in-app notification centre ───────────────

    @GetMapping("/my")
    public ResponseEntity<List<Notification>> getMyNotifications(
            @RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(notificationService.getByRecipient(userId));
    }

    @GetMapping("/my/unread")
    public ResponseEntity<List<Notification>> getUnread(
            @RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(notificationService.getUnread(userId));
    }

    @GetMapping("/my/unread/count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(
            @RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(Map.of(
                "unreadCount", notificationService.getUnreadCount(userId)));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Integer notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PutMapping("/my/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(
            @RequestHeader("X-User-Id") Integer userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Integer notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<Notification>> getByBooking(
            @PathVariable String bookingId) {
        return ResponseEntity.ok(notificationService.getByBooking(bookingId));
    }

    // ── Admin ────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Notification>> getAll(
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getByUser(
            @PathVariable Integer userId,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(notificationService.getByRecipient(userId));
    }
}