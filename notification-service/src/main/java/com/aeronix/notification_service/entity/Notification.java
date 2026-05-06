package com.aeronix.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_recipient", columnList = "recipientId"),
                @Index(name = "idx_booking",   columnList = "relatedBookingId")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;

    @Column(nullable = false)
    private Integer recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    private String relatedBookingId;

    private String recipientEmail;

    private String recipientPhone;

    @Builder.Default
    private boolean isRead = false;

    @Builder.Default
    private boolean isSent = false;

    private String failureReason;

    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    private LocalDateTime readAt;

    public enum NotificationType {
        BOOKING_CONFIRMED,
        PAYMENT_FAILED,
        FLIGHT_DELAY,
        GATE_CHANGE,
        CHECKIN_REMINDER,
        BOARDING,
        CANCELLATION,
        REFUND_INITIATED,
        BROADCAST,
        FLIGHT_CANCELLED
    }

    public enum NotificationChannel {
        APP, EMAIL, SMS, ALL
    }
}