package com.aeronix.notification_service.controller;

import com.aeronix.notification_service.dto.*;
import com.aeronix.notification_service.entity.Notification;
import com.aeronix.notification_service.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        sampleNotification = Notification.builder()
                .notificationId(1).recipientId(1)
                .type(Notification.NotificationType.BOOKING_CONFIRMED)
                .title("Booking Confirmed")
                .message("Your booking is confirmed.")
                .channel(Notification.NotificationChannel.APP)
                .relatedBookingId("booking-uuid-001")
                .isSent(true).isRead(false)
                .sentAt(LocalDateTime.now()).build();
    }

    @Test
    void send_returns_201() throws Exception {
        when(notificationService.send(any())).thenReturn(sampleNotification);

        Map<String, Object> req = Map.of(
                "recipientId", 1,
                "type", "BOOKING_CONFIRMED",
                "bookingId", "booking-uuid-001"
        );

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationId").value(1))
                .andExpect(jsonPath("$.type").value("BOOKING_CONFIRMED"));
    }

    @Test
    void sendBookingConfirmation_returns_200() throws Exception {
        doNothing().when(notificationService).sendBookingConfirmation(any());

        Map<String, Object> req = Map.of(
                "recipientId", 1,
                "bookingId", "booking-uuid-001",
                "pnrCode", "ABC123",
                "email", "john@example.com"
        );

        mockMvc.perform(post("/api/notifications/booking-confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Booking confirmation sent"));
    }

    @Test
    void sendBulk_returns_200() throws Exception {
        doNothing().when(notificationService).sendBulk(any());

        Map<String, Object> req = Map.of(
                "recipientIds", List.of(1, 2, 3),
                "type", "BROADCAST",
                "title", "Update",
                "message", "System update tonight"
        );

        mockMvc.perform(post("/api/notifications/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bulk notification sent to 3 recipients"));
    }

    @Test
    void sendBulk_null_recipients_returns_zero_in_message() throws Exception {
        doNothing().when(notificationService).sendBulk(any());

        Map<String, Object> req = new HashMap<>();
        req.put("type", "BROADCAST");
        req.put("message", "msg");

        mockMvc.perform(post("/api/notifications/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bulk notification sent to 0 recipients"));
    }

    @Test
    void sendFlightAlert_returns_200() throws Exception {
        doNothing().when(notificationService).sendFlightAlert(any());

        Map<String, Object> req = Map.of(
                "recipientIds", List.of(1, 2),
                "alertType", "FLIGHT_DELAY",
                "message", "Delayed by 30 mins",
                "delayMinutes", 30
        );

        mockMvc.perform(post("/api/notifications/flight-alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Flight alert dispatched"));
    }

    @Test
    void getMyNotifications_returns_200() throws Exception {
        when(notificationService.getByRecipient(1)).thenReturn(List.of(sampleNotification));

        mockMvc.perform(get("/api/notifications/my").header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].notificationId").value(1));
    }

    @Test
    void getUnread_returns_200() throws Exception {
        when(notificationService.getUnread(1)).thenReturn(List.of(sampleNotification));

        mockMvc.perform(get("/api/notifications/my/unread").header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getUnreadCount_returns_200() throws Exception {
        when(notificationService.getUnreadCount(1)).thenReturn(5);

        mockMvc.perform(get("/api/notifications/my/unread/count").header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void markAsRead_returns_200() throws Exception {
        doNothing().when(notificationService).markAsRead(1);

        mockMvc.perform(put("/api/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification marked as read"));
    }

    @Test
    void markAllRead_returns_200() throws Exception {
        doNothing().when(notificationService).markAllRead(1);

        mockMvc.perform(put("/api/notifications/my/read-all").header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));
    }

    @Test
    void delete_returns_200() throws Exception {
        doNothing().when(notificationService).deleteNotification(1);

        mockMvc.perform(delete("/api/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification deleted"));
    }

    @Test
    void getByBooking_returns_200() throws Exception {
        when(notificationService.getByBooking("booking-uuid-001"))
                .thenReturn(List.of(sampleNotification));

        mockMvc.perform(get("/api/notifications/booking/booking-uuid-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAll_admin_returns_200() throws Exception {
        when(notificationService.getAll()).thenReturn(List.of(sampleNotification));

        mockMvc.perform(get("/api/notifications").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAll_non_admin_returns_403() throws Exception {
        mockMvc.perform(get("/api/notifications").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByUser_admin_returns_200() throws Exception {
        when(notificationService.getByRecipient(1)).thenReturn(List.of(sampleNotification));

        mockMvc.perform(get("/api/notifications/user/1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByUser_non_admin_returns_403() throws Exception {
        mockMvc.perform(get("/api/notifications/user/1").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }
}