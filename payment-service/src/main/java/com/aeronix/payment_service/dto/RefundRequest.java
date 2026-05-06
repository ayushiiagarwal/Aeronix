package com.aeronix.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
public class RefundRequest {

    @NotBlank
    private String bookingId;

    private Double refundAmount;  // null = full refund
    private String reason;
}