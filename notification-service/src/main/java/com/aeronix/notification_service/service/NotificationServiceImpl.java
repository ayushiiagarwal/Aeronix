package com.aeronix.notification_service.service;

import com.aeronix.notification_service.dto.*;
import com.aeronix.notification_service.entity.Notification;
import com.aeronix.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    // ── Core Send ────────────────────────────────────────────

    @Override
    @Transactional
    public Notification send(SendNotificationRequest request) {
        Notification.NotificationType type;
        try {
            type = Notification.NotificationType.valueOf(request.getType().toUpperCase());
        } catch (Exception e) {
            type = Notification.NotificationType.BROADCAST;
        }

        Notification.NotificationChannel channel = Notification.NotificationChannel.APP;
        try {
            if (request.getChannel() != null) {
                channel = Notification.NotificationChannel.valueOf(
                        request.getChannel().toUpperCase());
            }
        } catch (Exception ignored) {}

        String title = request.getTitle() != null
                ? request.getTitle() : buildTitle(type);
        String message = request.getMessage() != null
                ? request.getMessage() : buildMessage(type, request);

        // Persist in-app notification
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .type(type)
                .title(title)
                .message(message)
                .channel(channel)
                .relatedBookingId(request.getBookingId())
                .recipientEmail(request.getEmail())
                .recipientPhone(request.getPhone())
                .isSent(false)
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        dispatchToChannels(notification, channel);

        return notification;
    }

    // ── Booking Confirmation ─────────────────────────────────

    @Override
    @Transactional
    public void sendBookingConfirmation(BookingConfirmationRequest request) {
        String title   = "Booking Confirmed — PNR: " + request.getPnrCode();
        String message = "Your booking " + request.getPnrCode() + " is confirmed. " +
                "Flight: " + request.getFlightNumber() + " | " +
                request.getOrigin() + " → " + request.getDestination() + " | " +
                "Dep: " + request.getDepartureTime();

        // 1. In-app notification
        Notification appNotif = Notification.builder()
                .recipientId(request.getRecipientId())
                .type(Notification.NotificationType.BOOKING_CONFIRMED)
                .title(title)
                .message(message)
                .channel(Notification.NotificationChannel.APP)
                .relatedBookingId(request.getBookingId())
                .recipientEmail(request.getEmail())
                .recipientPhone(request.getPhone())
                .isSent(true)
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(appNotif);

        // 2. Email notification (async)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String htmlBody = emailService.buildBookingConfirmationEmail(
                    request.getPassengerName() != null ? request.getPassengerName() : "Passenger",
                    request.getPnrCode(),
                    request.getFlightNumber() != null ? request.getFlightNumber() : "N/A",
                    request.getOrigin() != null ? request.getOrigin() : "N/A",
                    request.getDestination() != null ? request.getDestination() : "N/A",
                    request.getDepartureTime() != null ? request.getDepartureTime() : "N/A",
                    request.getSeatNumber() != null ? request.getSeatNumber() : "N/A",
                    request.getTotalFare() != null ? request.getTotalFare() : 0.0);

            emailService.sendHtmlEmail(request.getEmail(), title, htmlBody);

            Notification emailNotif = Notification.builder()
                    .recipientId(request.getRecipientId())
                    .type(Notification.NotificationType.BOOKING_CONFIRMED)
                    .title(title)
                    .message(message)
                    .channel(Notification.NotificationChannel.EMAIL)
                    .relatedBookingId(request.getBookingId())
                    .recipientEmail(request.getEmail())
                    .isSent(true)
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(emailNotif);
        }

        // 3. SMS notification (async)
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String smsBody = smsService.buildBookingConfirmationSms(
                    request.getPnrCode(),
                    request.getFlightNumber() != null ? request.getFlightNumber() : "N/A",
                    request.getOrigin() != null ? request.getOrigin() : "N/A",
                    request.getDestination() != null ? request.getDestination() : "N/A",
                    request.getDepartureTime() != null ? request.getDepartureTime() : "N/A");

            smsService.sendSms(request.getPhone(), smsBody);

            Notification smsNotif = Notification.builder()
                    .recipientId(request.getRecipientId())
                    .type(Notification.NotificationType.BOOKING_CONFIRMED)
                    .title(title)
                    .message(smsBody)
                    .channel(Notification.NotificationChannel.SMS)
                    .relatedBookingId(request.getBookingId())
                    .recipientPhone(request.getPhone())
                    .isSent(true)
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(smsNotif);
        }

        log.info("Booking confirmation sent for PNR: {}", request.getPnrCode());
    }

    // ── Bulk / Broadcast ─────────────────────────────────────

    @Override
    @Transactional
    public void sendBulk(BulkNotificationRequest request) {
        if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
            log.warn("Bulk notification called with empty recipient list");
            return;
        }

        Notification.NotificationType type;
        try {
            type = Notification.NotificationType.valueOf(request.getType().toUpperCase());
        } catch (Exception e) {
            type = Notification.NotificationType.BROADCAST;
        }

        for (Integer recipientId : request.getRecipientIds()) {
            Notification notification = Notification.builder()
                    .recipientId(recipientId)
                    .type(type)
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .channel(Notification.NotificationChannel.APP)
                    .relatedBookingId(request.getBookingId())
                    .isSent(true)
                    .isRead(false)
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        }

        log.info("Bulk notification sent to {} recipients", request.getRecipientIds().size());
    }

    // ── Flight Alert ─────────────────────────────────────────

    @Override
    @Transactional
    public void sendFlightAlert(FlightAlertRequest request) {
        if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
            log.warn("Flight alert called with empty recipient list");
            return;
        }

        Notification.NotificationType type;
        try {
            type = Notification.NotificationType.valueOf(request.getAlertType().toUpperCase());
        } catch (Exception e) {
            type = Notification.NotificationType.FLIGHT_DELAY;
        }

        String title = buildAlertTitle(type, request);

        for (Integer recipientId : request.getRecipientIds()) {
            // In-app
            Notification appNotif = Notification.builder()
                    .recipientId(recipientId)
                    .type(type)
                    .title(title)
                    .message(request.getMessage())
                    .channel(Notification.NotificationChannel.APP)
                    .isSent(true)
                    .isRead(false)
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(appNotif);
        }

        log.info("Flight alert '{}' sent to {} passengers",
                request.getAlertType(), request.getRecipientIds().size());
    }

    // ── Read State ───────────────────────────────────────────

    @Override
    @Transactional
    public void markAsRead(Integer notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Override
    @Transactional
    public void markAllRead(Integer recipientId) {
        notificationRepository.markAllReadForRecipient(recipientId);
    }

    // ── Queries ──────────────────────────────────────────────

    @Override
    public List<Notification> getByRecipient(Integer recipientId) {
        return notificationRepository.findByRecipientIdOrderBySentAtDesc(recipientId);
    }

    @Override
    public int getUnreadCount(Integer recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    @Transactional
    public void deleteNotification(Integer notificationId) {
        notificationRepository.deleteByNotificationId(notificationId);
    }

    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }

    @Override
    public List<Notification> getByBooking(String bookingId) {
        return notificationRepository.findByRelatedBookingId(bookingId);
    }

    @Override
    public List<Notification> getUnread(Integer recipientId) {
        return notificationRepository.findByRecipientIdAndIsRead(recipientId, false);
    }

    // ── Channel Dispatch ─────────────────────────────────────

    @Async
    public void dispatchToChannels(Notification notification,
                                   Notification.NotificationChannel channel) {
        try {
            switch (channel) {
                case EMAIL -> {
                    if (notification.getRecipientEmail() != null) {
                        emailService.sendSimpleEmail(
                                notification.getRecipientEmail(),
                                notification.getTitle(),
                                notification.getMessage());
                    }
                }
                case SMS -> {
                    if (notification.getRecipientPhone() != null) {
                        smsService.sendSms(
                                notification.getRecipientPhone(),
                                notification.getMessage());
                    }
                }
                case ALL -> {
                    if (notification.getRecipientEmail() != null) {
                        emailService.sendSimpleEmail(
                                notification.getRecipientEmail(),
                                notification.getTitle(),
                                notification.getMessage());
                    }
                    if (notification.getRecipientPhone() != null) {
                        smsService.sendSms(
                                notification.getRecipientPhone(),
                                notification.getMessage());
                    }
                }
                default -> {} // APP only — already persisted
            }

            // Mark as sent
            notification.setSent(true);
            notificationRepository.save(notification);

        } catch (Exception e) {
            log.error("Channel dispatch failed for notification {}: {}",
                    notification.getNotificationId(), e.getMessage());
            notification.setFailureReason(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private String buildTitle(Notification.NotificationType type) {
        return switch (type) {
            case BOOKING_CONFIRMED  -> "Booking Confirmed";
            case PAYMENT_FAILED     -> "Payment Failed";
            case FLIGHT_DELAY       -> "Flight Delayed";
            case GATE_CHANGE        -> "Gate Changed";
            case CHECKIN_REMINDER   -> "Check-In Reminder";
            case BOARDING           -> "Boarding Pass Ready";
            case CANCELLATION       -> "Booking Cancelled";
            case REFUND_INITIATED   -> "Refund Initiated";
            case FLIGHT_CANCELLED   -> "Flight Cancelled";
            default                 -> "SkyBooker Notification";
        };
    }

    private String buildMessage(Notification.NotificationType type,
                                SendNotificationRequest req) {
        return switch (type) {
            case BOOKING_CONFIRMED ->
                    "Your booking is confirmed. PNR: " + req.getPnrCode();
            case CHECKIN_REMINDER ->
                    "Web check-in is open for your upcoming flight. PNR: " + req.getPnrCode();
            case CANCELLATION ->
                    "Your booking " + req.getPnrCode() + " has been cancelled.";
            case REFUND_INITIATED ->
                    "Your refund has been initiated and will be credited in 5-7 working days.";
            default ->
                    req.getMessage() != null ? req.getMessage() : "You have a new notification.";
        };
    }

    private String buildAlertTitle(Notification.NotificationType type,
                                   FlightAlertRequest request) {
        return switch (type) {
            case FLIGHT_DELAY     -> "⚠ Flight Delayed — " +
                    (request.getDelayMinutes() != null
                            ? request.getDelayMinutes() + " min" : "");
            case GATE_CHANGE      -> "🚪 Gate Changed — New Gate: " + request.getNewGate();
            case FLIGHT_CANCELLED -> "❌ Flight Cancelled";
            default               -> "Flight Alert";
        };
    }
}