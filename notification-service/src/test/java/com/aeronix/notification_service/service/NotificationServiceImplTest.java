package com.aeronix.notification_service.service;

import com.aeronix.notification_service.dto.*;
import com.aeronix.notification_service.entity.Notification;
import com.aeronix.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailService emailService;
    @Mock private SmsService smsService;

    @InjectMocks private NotificationServiceImpl notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .notificationId(1)
                .recipientId(1)
                .type(Notification.NotificationType.BOOKING_CONFIRMED)
                .title("Booking Confirmed")
                .message("Your booking is confirmed. PNR: ABC123")
                .channel(Notification.NotificationChannel.APP)
                .relatedBookingId("booking-uuid-001")
                .recipientEmail("john@example.com")
                .recipientPhone("9999999999")
                .isSent(true)
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
    }

    // ── send ──────────────────────────────────────────────────

    @Test
    void send_app_channel_persists_notification() {
        when(notificationRepository.save(any())).thenReturn(sampleNotification);

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("BOOKING_CONFIRMED");
        req.setBookingId("booking-uuid-001");
        req.setEmail("john@example.com");

        Notification result = notificationService.send(req);

        assertThat(result).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void send_builds_title_from_type_when_not_provided() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("BOOKING_CONFIRMED");
        req.setTitle(null);

        Notification result = notificationService.send(req);

        assertThat(result.getTitle()).isEqualTo("Booking Confirmed");
    }

    @Test
    void send_uses_provided_title() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("BOOKING_CONFIRMED");
        req.setTitle("Custom Title");

        Notification result = notificationService.send(req);

        assertThat(result.getTitle()).isEqualTo("Custom Title");
    }

    @Test
    void send_builds_message_from_type_when_not_provided() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("CHECKIN_REMINDER");
        req.setPnrCode("ABC123");
        req.setMessage(null);

        Notification result = notificationService.send(req);

        assertThat(result.getMessage()).contains("check-in");
    }

    @Test
    void send_uses_provided_message() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("BOOKING_CONFIRMED");
        req.setMessage("Custom message here");

        Notification result = notificationService.send(req);

        assertThat(result.getMessage()).isEqualTo("Custom message here");
    }

    @Test
    void send_invalid_type_defaults_to_broadcast() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("UNKNOWN_TYPE");

        Notification result = notificationService.send(req);

        assertThat(result.getType()).isEqualTo(Notification.NotificationType.BROADCAST);
    }

    @Test
    void send_invalid_channel_defaults_to_app() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("BOOKING_CONFIRMED");
        req.setChannel("INVALID_CHANNEL");

        Notification result = notificationService.send(req);

        assertThat(result.getChannel()).isEqualTo(Notification.NotificationChannel.APP);
    }

    @Test
    void send_null_channel_defaults_to_app() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("BOOKING_CONFIRMED");
        req.setChannel(null);

        Notification result = notificationService.send(req);

        assertThat(result.getChannel()).isEqualTo(Notification.NotificationChannel.APP);
    }

    @Test
    void send_sets_is_read_false() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("BOOKING_CONFIRMED");

        Notification result = notificationService.send(req);

        assertThat(result.isRead()).isFalse();
    }

    @Test
    void send_sets_sent_at() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("BOOKING_CONFIRMED");

        Notification result = notificationService.send(req);

        assertThat(result.getSentAt()).isNotNull();
    }

    @Test
    void send_all_notification_types_build_correct_titles() {
        Map<String, String> expectedTitles = Map.of(
                "PAYMENT_FAILED",   "Payment Failed",
                "FLIGHT_DELAY",     "Flight Delayed",
                "GATE_CHANGE",      "Gate Changed",
                "CHECKIN_REMINDER", "Check-In Reminder",
                "BOARDING",         "Boarding Pass Ready",
                "CANCELLATION",     "Booking Cancelled",
                "REFUND_INITIATED", "Refund Initiated",
                "FLIGHT_CANCELLED", "Flight Cancelled"
        );

        for (Map.Entry<String, String> entry : expectedTitles.entrySet()) {
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SendNotificationRequest req = new SendNotificationRequest();
            req.setRecipientId(1);
            req.setType(entry.getKey());

            Notification result = notificationService.send(req);

            assertThat(result.getTitle()).isEqualTo(entry.getValue());
        }
    }

    @Test
    void send_cancellation_builds_correct_message() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("CANCELLATION");
        req.setPnrCode("ABC123");

        Notification result = notificationService.send(req);

        assertThat(result.getMessage()).contains("ABC123").contains("cancelled");
    }

    @Test
    void send_refund_initiated_builds_correct_message() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("REFUND_INITIATED");

        Notification result = notificationService.send(req);

        assertThat(result.getMessage()).contains("refund").contains("5-7 working days");
    }

    @Test
    void send_unknown_type_with_no_message_returns_default() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1);
        req.setType("BROADCAST");
        req.setMessage(null);

        Notification result = notificationService.send(req);

        assertThat(result.getMessage()).isEqualTo("You have a new notification.");
    }

    // ── sendBookingConfirmation ───────────────────────────────

    @Test
    void sendBookingConfirmation_saves_app_notification() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BookingConfirmationRequest req = buildConfirmationRequest();

        notificationService.sendBookingConfirmation(req);

        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    void sendBookingConfirmation_with_email_sends_email_and_saves_email_notification() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(emailService.buildBookingConfirmationEmail(any(), any(), any(), any(), any(), any(), any(), anyDouble()))
                .thenReturn("<html>email</html>");

        BookingConfirmationRequest req = buildConfirmationRequest();
        req.setEmail("john@example.com");

        notificationService.sendBookingConfirmation(req);

        verify(emailService).sendHtmlEmail(eq("john@example.com"), anyString(), anyString());
        verify(notificationRepository, atLeast(2)).save(any(Notification.class));
    }

    @Test
    void sendBookingConfirmation_null_email_skips_email_sending() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BookingConfirmationRequest req = buildConfirmationRequest();
        req.setEmail(null);

        notificationService.sendBookingConfirmation(req);

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    @Test
    void sendBookingConfirmation_blank_email_skips_email_sending() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BookingConfirmationRequest req = buildConfirmationRequest();
        req.setEmail("  ");

        notificationService.sendBookingConfirmation(req);

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    @Test
    void sendBookingConfirmation_with_phone_sends_sms_and_saves_sms_notification() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(smsService.buildBookingConfirmationSms(any(), any(), any(), any(), any()))
                .thenReturn("SMS body");

        BookingConfirmationRequest req = buildConfirmationRequest();
        req.setPhone("9999999999");

        notificationService.sendBookingConfirmation(req);

        verify(smsService).sendSms(eq("9999999999"), anyString());
        verify(notificationRepository, atLeast(2)).save(any(Notification.class));
    }

    @Test
    void sendBookingConfirmation_null_phone_skips_sms() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BookingConfirmationRequest req = buildConfirmationRequest();
        req.setPhone(null);

        notificationService.sendBookingConfirmation(req);

        verify(smsService, never()).sendSms(any(), any());
    }

    @Test
    void sendBookingConfirmation_blank_phone_skips_sms() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BookingConfirmationRequest req = buildConfirmationRequest();
        req.setPhone("  ");

        notificationService.sendBookingConfirmation(req);

        verify(smsService, never()).sendSms(any(), any());
    }

    @Test
    void sendBookingConfirmation_null_passenger_name_uses_passenger_default() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(emailService.buildBookingConfirmationEmail(any(), any(), any(), any(), any(), any(), any(), anyDouble()))
                .thenReturn("<html>email</html>");

        BookingConfirmationRequest req = buildConfirmationRequest();
        req.setPassengerName(null);
        req.setEmail("john@example.com");

        notificationService.sendBookingConfirmation(req);

        verify(emailService).buildBookingConfirmationEmail(
                eq("Passenger"), any(), any(), any(), any(), any(), any(), anyDouble());
    }

    @Test
    void sendBookingConfirmation_null_flight_number_uses_na() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(emailService.buildBookingConfirmationEmail(any(), any(), any(), any(), any(), any(), any(), anyDouble()))
                .thenReturn("<html>email</html>");

        BookingConfirmationRequest req = buildConfirmationRequest();
        req.setFlightNumber(null);
        req.setEmail("john@example.com");

        notificationService.sendBookingConfirmation(req);

        verify(emailService).buildBookingConfirmationEmail(
                any(), any(), eq("N/A"), any(), any(), any(), any(), anyDouble());
    }

    @Test
    void sendBookingConfirmation_title_contains_pnr() {
        when(notificationRepository.save(any())).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            assertThat(n.getTitle()).contains("ABC123");
            return n;
        });

        notificationService.sendBookingConfirmation(buildConfirmationRequest());
    }

    @Test
    void sendBookingConfirmation_with_email_and_phone_saves_three_notifications() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(emailService.buildBookingConfirmationEmail(any(), any(), any(), any(), any(), any(), any(), anyDouble()))
                .thenReturn("<html>email</html>");
        when(smsService.buildBookingConfirmationSms(any(), any(), any(), any(), any()))
                .thenReturn("SMS body");

        BookingConfirmationRequest req = buildConfirmationRequest();
        req.setEmail("john@example.com");
        req.setPhone("9999999999");

        notificationService.sendBookingConfirmation(req);

        verify(notificationRepository, times(3)).save(any(Notification.class));
    }

    // ── sendBulk ──────────────────────────────────────────────

    @Test
    void sendBulk_sends_to_all_recipients() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BulkNotificationRequest req = new BulkNotificationRequest();
        req.setRecipientIds(List.of(1, 2, 3));
        req.setType("BROADCAST");
        req.setTitle("Service Update");
        req.setMessage("Scheduled maintenance tonight");

        notificationService.sendBulk(req);

        verify(notificationRepository, times(3)).save(any(Notification.class));
    }

    @Test
    void sendBulk_empty_recipients_does_nothing() {
        BulkNotificationRequest req = new BulkNotificationRequest();
        req.setRecipientIds(Collections.emptyList());
        req.setType("BROADCAST");
        req.setMessage("msg");

        notificationService.sendBulk(req);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendBulk_null_recipients_does_nothing() {
        BulkNotificationRequest req = new BulkNotificationRequest();
        req.setRecipientIds(null);
        req.setType("BROADCAST");
        req.setMessage("msg");

        notificationService.sendBulk(req);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendBulk_invalid_type_defaults_to_broadcast() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BulkNotificationRequest req = new BulkNotificationRequest();
        req.setRecipientIds(List.of(1));
        req.setType("UNKNOWN");
        req.setTitle("Title");
        req.setMessage("msg");

        notificationService.sendBulk(req);

        verify(notificationRepository).save(argThat(n ->
                n.getType() == Notification.NotificationType.BROADCAST));
    }

    @Test
    void sendBulk_single_recipient() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BulkNotificationRequest req = new BulkNotificationRequest();
        req.setRecipientIds(List.of(1));
        req.setType("FLIGHT_CANCELLED");
        req.setTitle("Flight Cancelled");
        req.setMessage("Your flight has been cancelled");

        notificationService.sendBulk(req);

        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void sendBulk_each_notification_is_app_channel() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BulkNotificationRequest req = new BulkNotificationRequest();
        req.setRecipientIds(List.of(1, 2));
        req.setType("BROADCAST");
        req.setMessage("msg");

        notificationService.sendBulk(req);

        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getChannel() == Notification.NotificationChannel.APP));
    }

    @Test
    void sendBulk_each_notification_is_unread() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BulkNotificationRequest req = new BulkNotificationRequest();
        req.setRecipientIds(List.of(1));
        req.setType("BROADCAST");
        req.setMessage("msg");

        notificationService.sendBulk(req);

        verify(notificationRepository).save(argThat(n -> !n.isRead()));
    }

    // ── sendFlightAlert ───────────────────────────────────────

    @Test
    void sendFlightAlert_sends_to_all_recipients() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FlightAlertRequest req = new FlightAlertRequest();
        req.setRecipientIds(List.of(1, 2, 3));
        req.setAlertType("FLIGHT_DELAY");
        req.setMessage("Flight delayed by 30 minutes");
        req.setDelayMinutes(30);

        notificationService.sendFlightAlert(req);

        verify(notificationRepository, times(3)).save(any(Notification.class));
    }

    @Test
    void sendFlightAlert_empty_recipients_does_nothing() {
        FlightAlertRequest req = new FlightAlertRequest();
        req.setRecipientIds(Collections.emptyList());
        req.setAlertType("FLIGHT_DELAY");
        req.setMessage("msg");

        notificationService.sendFlightAlert(req);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendFlightAlert_null_recipients_does_nothing() {
        FlightAlertRequest req = new FlightAlertRequest();
        req.setRecipientIds(null);
        req.setAlertType("FLIGHT_DELAY");
        req.setMessage("msg");

        notificationService.sendFlightAlert(req);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendFlightAlert_delay_title_contains_minutes() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FlightAlertRequest req = new FlightAlertRequest();
        req.setRecipientIds(List.of(1));
        req.setAlertType("FLIGHT_DELAY");
        req.setMessage("Delayed");
        req.setDelayMinutes(45);

        notificationService.sendFlightAlert(req);

        verify(notificationRepository).save(argThat(n -> n.getTitle().contains("45")));
    }

    @Test
    void sendFlightAlert_gate_change_title_contains_new_gate() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FlightAlertRequest req = new FlightAlertRequest();
        req.setRecipientIds(List.of(1));
        req.setAlertType("GATE_CHANGE");
        req.setMessage("Gate changed");
        req.setNewGate("B12");

        notificationService.sendFlightAlert(req);

        verify(notificationRepository).save(argThat(n -> n.getTitle().contains("B12")));
    }

    @Test
    void sendFlightAlert_cancelled_type_sets_correct_type() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FlightAlertRequest req = new FlightAlertRequest();
        req.setRecipientIds(List.of(1));
        req.setAlertType("FLIGHT_CANCELLED");
        req.setMessage("Cancelled");

        notificationService.sendFlightAlert(req);

        verify(notificationRepository).save(argThat(n ->
                n.getType() == Notification.NotificationType.FLIGHT_CANCELLED));
    }

    @Test
    void sendFlightAlert_invalid_type_defaults_to_flight_delay() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FlightAlertRequest req = new FlightAlertRequest();
        req.setRecipientIds(List.of(1));
        req.setAlertType("UNKNOWN");
        req.setMessage("msg");

        notificationService.sendFlightAlert(req);

        verify(notificationRepository).save(argThat(n ->
                n.getType() == Notification.NotificationType.FLIGHT_DELAY));
    }

    // ── markAsRead ────────────────────────────────────────────

    @Test
    void markAsRead_calls_repository() {
        when(notificationRepository.markAsRead(1)).thenReturn(1);

        notificationService.markAsRead(1);

        verify(notificationRepository).markAsRead(1);
    }

    @Test
    void markAsRead_nonexistent_id_no_exception() {
        when(notificationRepository.markAsRead(99)).thenReturn(0);

        assertThatCode(() -> notificationService.markAsRead(99))
                .doesNotThrowAnyException();
    }

    // ── markAllRead ───────────────────────────────────────────

    @Test
    void markAllRead_calls_repository_with_recipient_id() {
        when(notificationRepository.markAllReadForRecipient(1)).thenReturn(5);

        notificationService.markAllRead(1);

        verify(notificationRepository).markAllReadForRecipient(1);
    }

    @Test
    void markAllRead_no_unread_no_exception() {
        when(notificationRepository.markAllReadForRecipient(1)).thenReturn(0);

        assertThatCode(() -> notificationService.markAllRead(1))
                .doesNotThrowAnyException();
    }

    // ── getByRecipient ────────────────────────────────────────

    @Test
    void getByRecipient_returns_list() {
        when(notificationRepository.findByRecipientIdOrderBySentAtDesc(1))
                .thenReturn(List.of(sampleNotification));

        List<Notification> result = notificationService.getByRecipient(1);

        assertThat(result).hasSize(1);
    }

    @Test
    void getByRecipient_empty_for_unknown_user() {
        when(notificationRepository.findByRecipientIdOrderBySentAtDesc(99))
                .thenReturn(Collections.emptyList());

        List<Notification> result = notificationService.getByRecipient(99);

        assertThat(result).isEmpty();
    }

    // ── getUnreadCount ────────────────────────────────────────

    @Test
    void getUnreadCount_returns_count() {
        when(notificationRepository.countByRecipientIdAndIsRead(1, false)).thenReturn(3);

        int count = notificationService.getUnreadCount(1);

        assertThat(count).isEqualTo(3);
    }

    @Test
    void getUnreadCount_zero_when_all_read() {
        when(notificationRepository.countByRecipientIdAndIsRead(1, false)).thenReturn(0);

        int count = notificationService.getUnreadCount(1);

        assertThat(count).isEqualTo(0);
    }

    // ── deleteNotification ────────────────────────────────────

    @Test
    void deleteNotification_calls_repository() {
        doNothing().when(notificationRepository).deleteByNotificationId(1);

        notificationService.deleteNotification(1);

        verify(notificationRepository).deleteByNotificationId(1);
    }

    // ── getAll ────────────────────────────────────────────────

    @Test
    void getAll_returns_all() {
        when(notificationRepository.findAll()).thenReturn(List.of(sampleNotification));

        List<Notification> result = notificationService.getAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAll_empty() {
        when(notificationRepository.findAll()).thenReturn(Collections.emptyList());

        List<Notification> result = notificationService.getAll();

        assertThat(result).isEmpty();
    }

    // ── getByBooking ──────────────────────────────────────────

    @Test
    void getByBooking_returns_list() {
        when(notificationRepository.findByRelatedBookingId("booking-uuid-001"))
                .thenReturn(List.of(sampleNotification));

        List<Notification> result = notificationService.getByBooking("booking-uuid-001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRelatedBookingId()).isEqualTo("booking-uuid-001");
    }

    @Test
    void getByBooking_not_found_returns_empty() {
        when(notificationRepository.findByRelatedBookingId("nonexistent"))
                .thenReturn(Collections.emptyList());

        List<Notification> result = notificationService.getByBooking("nonexistent");

        assertThat(result).isEmpty();
    }

    // ── getUnread ─────────────────────────────────────────────

    @Test
    void getUnread_returns_unread_notifications() {
        when(notificationRepository.findByRecipientIdAndIsRead(1, false))
                .thenReturn(List.of(sampleNotification));

        List<Notification> result = notificationService.getUnread(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    void getUnread_empty_when_all_read() {
        when(notificationRepository.findByRecipientIdAndIsRead(1, false))
                .thenReturn(Collections.emptyList());

        List<Notification> result = notificationService.getUnread(1);

        assertThat(result).isEmpty();
    }

    // ── dispatchToChannels ────────────────────────────────────

    @Test
    void dispatchToChannels_email_sends_simple_email() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.dispatchToChannels(sampleNotification, Notification.NotificationChannel.EMAIL);

        verify(emailService).sendSimpleEmail(
                eq("john@example.com"), eq("Booking Confirmed"), anyString());
    }

    @Test
    void dispatchToChannels_email_null_email_skips_send() {
        sampleNotification.setRecipientEmail(null);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.dispatchToChannels(sampleNotification, Notification.NotificationChannel.EMAIL);

        verify(emailService, never()).sendSimpleEmail(any(), any(), any());
    }

    @Test
    void dispatchToChannels_sms_sends_sms() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.dispatchToChannels(sampleNotification, Notification.NotificationChannel.SMS);

        verify(smsService).sendSms(eq("9999999999"), anyString());
    }

    @Test
    void dispatchToChannels_sms_null_phone_skips_send() {
        sampleNotification.setRecipientPhone(null);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.dispatchToChannels(sampleNotification, Notification.NotificationChannel.SMS);

        verify(smsService, never()).sendSms(any(), any());
    }

    @Test
    void dispatchToChannels_all_sends_both_email_and_sms() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.dispatchToChannels(sampleNotification, Notification.NotificationChannel.ALL);

        verify(emailService).sendSimpleEmail(any(), any(), any());
        verify(smsService).sendSms(any(), any());
    }

    @Test
    void dispatchToChannels_app_skips_email_and_sms() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.dispatchToChannels(sampleNotification, Notification.NotificationChannel.APP);

        verify(emailService, never()).sendSimpleEmail(any(), any(), any());
        verify(smsService, never()).sendSms(any(), any());
    }

    @Test
    void dispatchToChannels_marks_as_sent_on_success() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.dispatchToChannels(sampleNotification, Notification.NotificationChannel.APP);

        assertThat(sampleNotification.isSent()).isTrue();
    }

    @Test
    void dispatchToChannels_email_exception_stores_failure_reason() {
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendSimpleEmail(any(), any(), any());
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.dispatchToChannels(sampleNotification, Notification.NotificationChannel.EMAIL);

        assertThat(sampleNotification.getFailureReason()).contains("SMTP error");
    }

    // ── Helpers ───────────────────────────────────────────────

    private BookingConfirmationRequest buildConfirmationRequest() {
        BookingConfirmationRequest req = new BookingConfirmationRequest();
        req.setRecipientId(1);
        req.setBookingId("booking-uuid-001");
        req.setPnrCode("ABC123");
        req.setPassengerName("John Doe");
        req.setFlightNumber("AI101");
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureTime("2026-06-01T10:00:00");
        req.setSeatNumber("12A");
        req.setTotalFare(5350.0);
        return req;
    }
}