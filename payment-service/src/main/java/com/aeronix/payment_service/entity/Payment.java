package com.aeronix.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @Column(length = 36)
    private String paymentId;

    @Column(nullable = false)
    private String bookingId;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String transactionId;       // Gateway transaction ID
    private String gatewayOrderId;      // Razorpay order ID
    private String gatewayPaymentId;    // Razorpay payment ID
    private String gatewaySignature;    // Razorpay signature

    @Column(columnDefinition = "TEXT")
    private String gatewayResponse;     // Full gateway JSON response

    private LocalDateTime initiatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime failedAt;

    @Builder.Default
    private Double refundAmount = 0.0;

    private String refundTransactionId;
    private String refundReason;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED, PARTIALLY_REFUNDED
    }

    public enum PaymentMode {
        CARD, UPI, NETBANKING, WALLET
    }
}