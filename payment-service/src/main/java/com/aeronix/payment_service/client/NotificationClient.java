package com.aeronix.payment_service.client;

import com.aeronix.payment_service.config.RabbitMQConfig;
import com.aeronix.payment_service.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RabbitTemplate rabbitTemplate;

    public void sendPaymentSuccess(Integer userId, String bookingId,
                                   String pnrCode, Double amount) {
        publish(RabbitMQConfig.KEY_PAYMENT_SUCCESS, NotificationEvent.builder()
                .userId(userId).bookingId(bookingId)
                .pnrCode(pnrCode).amount(amount).type("PAYMENT_SUCCESS").build());
    }

    public void sendPaymentFailed(Integer userId, String bookingId, Double amount) {
        publish(RabbitMQConfig.KEY_PAYMENT_FAILED, NotificationEvent.builder()
                .userId(userId).bookingId(bookingId)
                .amount(amount).type("PAYMENT_FAILED").build());
    }

    public void sendRefundInitiated(Integer userId, String bookingId, Double amount) {
        publish(RabbitMQConfig.KEY_REFUND_INITIATED, NotificationEvent.builder()
                .userId(userId).bookingId(bookingId)
                .amount(amount).type("REFUND_INITIATED").build());
    }

    private void publish(String routingKey, NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
            log.info("Published event [{}] for bookingId={}", event.getType(), event.getBookingId());
        } catch (Exception e) {
            log.warn("Failed to publish payment event: {}", e.getMessage());
        }
    }
}
