package com.aeronix.notification_service.service;;

import com.aeronix.notification_service.dto.*;
import com.aeronix.notification_service.entity.Notification;

import java.util.List;

public interface NotificationService {
    Notification send(SendNotificationRequest request);
    void sendBookingConfirmation(BookingConfirmationRequest request);
    void sendBulk(BulkNotificationRequest request);
    void sendFlightAlert(FlightAlertRequest request);
    void markAsRead(Integer notificationId);
    void markAllRead(Integer recipientId);
    List<Notification> getByRecipient(Integer recipientId);
    int getUnreadCount(Integer recipientId);
    void deleteNotification(Integer notificationId);
    List<Notification> getAll();
    List<Notification> getByBooking(String bookingId);
    List<Notification> getUnread(Integer recipientId);
}