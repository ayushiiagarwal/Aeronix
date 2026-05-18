package com.aeronix.notification_service.messaging;

import com.aeronix.notification_service.config.RabbitMQConfig;
import com.aeronix.notification_service.dto.*;
import com.aeronix.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CONFIRMED_QUEUE)
    public void onBookingConfirmed(NotificationEvent event) {
        log.info("Received BOOKING_CONFIRMED for bookingId={}", event.getBookingId());
        BookingConfirmationRequest req = new BookingConfirmationRequest();
        req.setRecipientId(event.getUserId());
        req.setBookingId(event.getBookingId());
        req.setPnrCode(event.getPnrCode());
        req.setEmail(event.getEmail());
        req.setPhone(event.getPhone());
        req.setPassengerName(event.getPassengerName());
        req.setFlightNumber(event.getFlightNumber());
        req.setOrigin(event.getOrigin());
        req.setDestination(event.getDestination());
        req.setDepartureTime(event.getDepartureTime());
        req.setSeatNumber(event.getSeatNumber());
        req.setTotalFare(event.getTotalFare());
        notificationService.sendBookingConfirmation(req);
    }

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CANCELLED_QUEUE)
    public void onBookingCancelled(NotificationEvent event) {
        log.info("Received BOOKING_CANCELLED for bookingId={}", event.getBookingId());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(event.getUserId());
        req.setBookingId(event.getBookingId());
        req.setEmail(event.getEmail());
        req.setType("CANCELLATION");
        notificationService.send(req);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void onPaymentSuccess(NotificationEvent event) {
        log.info("Received PAYMENT_SUCCESS for bookingId={}", event.getBookingId());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(event.getUserId());
        req.setBookingId(event.getBookingId());
        req.setType("PAYMENT_SUCCESS");
        notificationService.send(req);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(NotificationEvent event) {
        log.info("Received PAYMENT_FAILED for bookingId={}", event.getBookingId());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(event.getUserId());
        req.setBookingId(event.getBookingId());
        req.setType("PAYMENT_FAILED");
        notificationService.send(req);
    }

    @RabbitListener(queues = RabbitMQConfig.REFUND_INITIATED_QUEUE)
    public void onRefundInitiated(NotificationEvent event) {
        log.info("Received REFUND_INITIATED for bookingId={}", event.getBookingId());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(event.getUserId());
        req.setBookingId(event.getBookingId());
        req.setType("REFUND_INITIATED");
        notificationService.send(req);
    }

    @RabbitListener(queues = RabbitMQConfig.CHECKIN_REMINDER_QUEUE)
    public void onCheckinReminder(NotificationEvent event) {
        log.info("Received CHECKIN_REMINDER for bookingId={}", event.getBookingId());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(event.getUserId());
        req.setBookingId(event.getBookingId());
        req.setEmail(event.getEmail());
        req.setType("CHECKIN_REMINDER");
        notificationService.send(req);
    }
}
