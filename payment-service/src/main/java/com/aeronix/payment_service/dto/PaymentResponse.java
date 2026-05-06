package com.aeronix.payment_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private String paymentId;
    private String bookingId;
    private String gatewayOrderId;
    private Double amount;
    private String currency;
    private String status;
    private String paymentMode;
    private String razorpayKeyId;
    private String message;
}