package com.aeronix.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InitiatePaymentRequest {

    @NotBlank
    private String bookingId;

    @NotNull @Positive
    private Double amount;

    @NotBlank
    private String paymentMode;   // CARD, UPI, NETBANKING, WALLET

    private String currency = "INR";
}