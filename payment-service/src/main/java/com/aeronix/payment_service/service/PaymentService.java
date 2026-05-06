package com.aeronix.payment_service.service;

import com.aeronix.payment_service.dto.*;
import com.aeronix.payment_service.entity.Payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentService {
    PaymentResponse initiatePayment(InitiatePaymentRequest request, Integer userId);
    Payment processPayment(GatewayCallbackRequest request);
    Optional<Payment> getPaymentByBooking(String bookingId);
    List<Payment> getPaymentsByUser(Integer userId);
    RefundResponse refundPayment(RefundRequest request, Integer userId);
    String getPaymentStatus(String paymentId);
    void updatePaymentStatus(String paymentId, String status);
    String generateReceipt(String paymentId);
    RevenueReport getRevenue(LocalDateTime from, LocalDateTime to);
    Double getTotalRevenue();
    long countSuccessfulPayments();
    List<Payment> getAllPayments();
    Optional<Payment> getPaymentById(String paymentId);
    Payment handlePaymentFailure(String bookingId, String reason);
}