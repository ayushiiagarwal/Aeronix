package com.aeronix.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "aeronix.events";

    public static final String BOOKING_CONFIRMED_QUEUE = "booking.confirmed";
    public static final String BOOKING_CANCELLED_QUEUE = "booking.cancelled";
    public static final String PAYMENT_SUCCESS_QUEUE   = "payment.success";
    public static final String PAYMENT_FAILED_QUEUE    = "payment.failed";
    public static final String REFUND_INITIATED_QUEUE  = "refund.initiated";
    public static final String CHECKIN_REMINDER_QUEUE  = "checkin.reminder";

    public static final String KEY_BOOKING_CONFIRMED = "booking.confirmed";
    public static final String KEY_BOOKING_CANCELLED = "booking.cancelled";
    public static final String KEY_PAYMENT_SUCCESS   = "payment.success";
    public static final String KEY_PAYMENT_FAILED    = "payment.failed";
    public static final String KEY_REFUND_INITIATED  = "refund.initiated";
    public static final String KEY_CHECKIN_REMINDER  = "checkin.reminder";

    @Bean public TopicExchange aeronixExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean public Queue bookingConfirmedQueue() { return new Queue(BOOKING_CONFIRMED_QUEUE, true); }
    @Bean public Queue bookingCancelledQueue() { return new Queue(BOOKING_CANCELLED_QUEUE, true); }
    @Bean public Queue paymentSuccessQueue()   { return new Queue(PAYMENT_SUCCESS_QUEUE,   true); }
    @Bean public Queue paymentFailedQueue()    { return new Queue(PAYMENT_FAILED_QUEUE,    true); }
    @Bean public Queue refundInitiatedQueue()  { return new Queue(REFUND_INITIATED_QUEUE,  true); }
    @Bean public Queue checkinReminderQueue()  { return new Queue(CHECKIN_REMINDER_QUEUE,  true); }

    @Bean public Binding bindingBookingConfirmed(Queue bookingConfirmedQueue, TopicExchange aeronixExchange) { return BindingBuilder.bind(bookingConfirmedQueue).to(aeronixExchange).with(KEY_BOOKING_CONFIRMED); }
    @Bean public Binding bindingBookingCancelled(Queue bookingCancelledQueue, TopicExchange aeronixExchange) { return BindingBuilder.bind(bookingCancelledQueue).to(aeronixExchange).with(KEY_BOOKING_CANCELLED); }
    @Bean public Binding bindingPaymentSuccess(Queue paymentSuccessQueue, TopicExchange aeronixExchange)     { return BindingBuilder.bind(paymentSuccessQueue).to(aeronixExchange).with(KEY_PAYMENT_SUCCESS); }
    @Bean public Binding bindingPaymentFailed(Queue paymentFailedQueue, TopicExchange aeronixExchange)       { return BindingBuilder.bind(paymentFailedQueue).to(aeronixExchange).with(KEY_PAYMENT_FAILED); }
    @Bean public Binding bindingRefundInitiated(Queue refundInitiatedQueue, TopicExchange aeronixExchange)   { return BindingBuilder.bind(refundInitiatedQueue).to(aeronixExchange).with(KEY_REFUND_INITIATED); }
    @Bean public Binding bindingCheckinReminder(Queue checkinReminderQueue, TopicExchange aeronixExchange)   { return BindingBuilder.bind(checkinReminderQueue).to(aeronixExchange).with(KEY_CHECKIN_REMINDER); }

    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
