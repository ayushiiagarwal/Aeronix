package com.aeronix.payment_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundResponse {
    private String paymentId;
    private String bookingId;
    private Double refundAmount;
    private Double cancellationFee;
    private String refundTransactionId;
    private String status;
    private String message;
    private String estimatedCreditDays;
}