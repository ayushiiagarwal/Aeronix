package com.aeronix.booking_service.client;

import com.aeronix.booking_service.config.RabbitMQConfig;
import com.aeronix.booking_service.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RabbitTemplate rabbitTemplate;

    public void sendBookingConfirmation(Integer userId, String bookingId,
                                        String pnrCode, String email, String phone,
                                        String passengerName, String flightNumber,
                                        String origin, String destination,
                                        String departureTime, String seatNumber,
                                        Double totalFare) {
        publish(RabbitMQConfig.KEY_BOOKING_CONFIRMED, NotificationEvent.builder()
                .userId(userId).bookingId(bookingId).pnrCode(pnrCode)
                .email(email).phone(phone).passengerName(passengerName)
                .flightNumber(flightNumber).origin(origin).destination(destination)
                .departureTime(departureTime).seatNumber(seatNumber)
                .totalFare(totalFare).type("BOOKING_CONFIRMED").build());
    }

    public void sendCancellationNotification(Integer userId, String bookingId,
                                              String pnrCode, String email) {
        publish(RabbitMQConfig.KEY_BOOKING_CANCELLED, NotificationEvent.builder()
                .userId(userId).bookingId(bookingId).pnrCode(pnrCode)
                .email(email).type("BOOKING_CANCELLED").build());
    }

    public void sendCheckinReminder(Integer userId, String bookingId, String email) {
        publish(RabbitMQConfig.KEY_CHECKIN_REMINDER, NotificationEvent.builder()
                .userId(userId).bookingId(bookingId)
                .email(email).type("CHECKIN_REMINDER").build());
    }

    private void publish(String routingKey, NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
            log.info("Published event [{}] for bookingId={}", event.getType(), event.getBookingId());
        } catch (Exception e) {
            log.warn("Failed to publish notification event: {}", e.getMessage());
        }
    }
}
