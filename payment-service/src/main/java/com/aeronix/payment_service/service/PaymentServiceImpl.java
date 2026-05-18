package com.aeronix.payment_service.service;

import com.aeronix.payment_service.client.*;
import com.aeronix.payment_service.dto.*;
import com.aeronix.payment_service.entity.Payment;
import com.aeronix.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingClient bookingClient;
    private final NotificationClient notificationClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String defaultCurrency;

    @Value("${refund.policy.0to4hours:100}")
    private int refundFee0to4;

    @Value("${refund.policy.4to24hours:50}")
    private int refundFee4to24;

    @Value("${refund.policy.24to72hours:25}")
    private int refundFee24to72;

    @Value("${refund.policy.72plusHours:0}")
    private int refundFee72plus;

    // ── Initiate Payment ─────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request,
                                           Integer userId) {
        // Validate booking exists
        Map<String, Object> booking = bookingClient.getBookingById(request.getBookingId());
        if (booking == null) {
            throw new RuntimeException("Booking not found: " + request.getBookingId());
        }

        String bookingStatus = (String) booking.get("status");
        if (!"PENDING".equals(bookingStatus)) {
            throw new RuntimeException(
                    "Booking is not in PENDING state. Current: " + bookingStatus);
        }

        // Check for existing payment
        Optional<Payment> existing = paymentRepository.findByBookingId(request.getBookingId());
        if (existing.isPresent() &&
                existing.get().getStatus() == Payment.PaymentStatus.PAID) {
            throw new RuntimeException("Booking already paid");
        }

        // Generate gateway order ID (simulated — replace with real Razorpay SDK call)
        String gatewayOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String paymentId = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .bookingId(request.getBookingId())
                .userId(userId)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : defaultCurrency)
                .status(Payment.PaymentStatus.PENDING)
                .paymentMode(Payment.PaymentMode.valueOf(request.getPaymentMode().toUpperCase()))
                .gatewayOrderId(gatewayOrderId)
                .initiatedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info("Payment initiated: paymentId={}, bookingId={}, amount={}",
                paymentId, request.getBookingId(), request.getAmount());

        return PaymentResponse.builder()
                .paymentId(paymentId)
                .bookingId(request.getBookingId())
                .gatewayOrderId(gatewayOrderId)
                .amount(request.getAmount())
                .currency(payment.getCurrency())
                .status("PENDING")
                .paymentMode(request.getPaymentMode())
                .razorpayKeyId(razorpayKeyId)
                .message("Payment session created. Complete payment using gateway.")
                .build();
    }

    // ── Process Gateway Callback ─────────────────────────────

    @Override
    @Transactional
    public Payment processPayment(GatewayCallbackRequest request) {
        Payment payment = paymentRepository
                .findByGatewayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException(
                        "Payment not found for order: " + request.getRazorpayOrderId()));

        // Signature verification (simplified — use Razorpay SDK in production)
        boolean signatureValid = verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!signatureValid) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailedAt(LocalDateTime.now());
            payment.setGatewayResponse("SIGNATURE_MISMATCH");
            paymentRepository.save(payment);

            // Notify failure
            notificationClient.sendPaymentFailed(
                    payment.getUserId(), payment.getBookingId(), payment.getAmount());

            throw new RuntimeException("Payment signature verification failed");
        }

        // Mark as PAID
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setGatewayPaymentId(request.getRazorpayPaymentId());
        payment.setGatewaySignature(request.getRazorpaySignature());
        payment.setTransactionId(request.getRazorpayPaymentId());
        payment.setGatewayResponse(request.getPayload() != null
                ? request.getPayload().toString() : "SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Confirm booking in booking-service
        bookingClient.confirmBooking(payment.getBookingId(), payment.getPaymentId());

        // Fetch booking for PNR
        Map<String, Object> booking = bookingClient.getBookingById(payment.getBookingId());
        String pnrCode = booking != null ? (String) booking.get("pnrCode") : "N/A";

        // Notify success
        notificationClient.sendPaymentSuccess(
                payment.getUserId(), payment.getBookingId(),
                pnrCode, payment.getAmount());

        log.info("Payment processed: paymentId={}, bookingId={}",
                payment.getPaymentId(), payment.getBookingId());

        return payment;
    }

    // ── Simulate Payment (for testing without real gateway) ──

    @Transactional
    public Payment simulateSuccessfulPayment(String bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + bookingId));

        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setTransactionId("TXN_" + System.currentTimeMillis());
        payment.setGatewayPaymentId("pay_sim_" + UUID.randomUUID().toString().substring(0, 8));
        payment.setGatewayResponse("SIMULATED_SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        bookingClient.confirmBooking(bookingId, payment.getPaymentId());

        Map<String, Object> booking = bookingClient.getBookingById(bookingId);
        String pnrCode = booking != null ? (String) booking.get("pnrCode") : "N/A";

        notificationClient.sendPaymentSuccess(
                payment.getUserId(), bookingId, pnrCode, payment.getAmount());

        return payment;
    }

    // ── Refund ───────────────────────────────────────────────

    @Override
    @Transactional
    public RefundResponse refundPayment(RefundRequest request, Integer userId) {
        Payment payment = paymentRepository.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new RuntimeException(
                        "Payment not found for booking: " + request.getBookingId()));

        if (payment.getStatus() != Payment.PaymentStatus.PAID) {
            throw new RuntimeException(
                    "Cannot refund payment with status: " + payment.getStatus());
        }

        // Calculate cancellation fee based on policy
        double cancellationFeePercent = calculateCancellationFee(payment.getBookingId());
        double paidAmount = payment.getAmount();
        double cancellationFee = paidAmount * cancellationFeePercent / 100.0;
        double refundAmount = request.getRefundAmount() != null
                ? Math.min(request.getRefundAmount(), paidAmount - cancellationFee)
                : paidAmount - cancellationFee;

        // Process refund via gateway (simulated)
        String refundTxnId = "rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        payment.setStatus(refundAmount >= paidAmount
                ? Payment.PaymentStatus.REFUNDED
                : Payment.PaymentStatus.PARTIALLY_REFUNDED);
        payment.setRefundAmount(refundAmount);
        payment.setRefundTransactionId(refundTxnId);
        payment.setRefundReason(request.getReason());
        payment.setRefundedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update booking status to CANCELLED
        bookingClient.updateBookingStatus(request.getBookingId(), "CANCELLED");

        // Notify
        notificationClient.sendRefundInitiated(
                payment.getUserId(), request.getBookingId(), refundAmount);

        log.info("Refund processed: bookingId={}, refundAmount={}, fee={}",
                request.getBookingId(), refundAmount, cancellationFee);

        return RefundResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(request.getBookingId())
                .refundAmount(refundAmount)
                .cancellationFee(cancellationFee)
                .refundTransactionId(refundTxnId)
                .status(payment.getStatus().name())
                .message("Refund initiated successfully")
                .estimatedCreditDays("5-7 working days")
                .build();
    }

    // ── Queries ──────────────────────────────────────────────

    @Override
    public Optional<Payment> getPaymentByBooking(String bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    @Override
    public List<Payment> getPaymentsByUser(Integer userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public String getPaymentStatus(String paymentId) {
        return paymentRepository.findById(paymentId)
                .map(p -> p.getStatus().name())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    @Override
    @Transactional
    public void updatePaymentStatus(String paymentId, String status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        payment.setStatus(Payment.PaymentStatus.valueOf(status.toUpperCase()));
        paymentRepository.save(payment);
    }

    @Override
    public String generateReceipt(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        return buildReceiptText(payment);
    }

    @Override
    public RevenueReport getRevenue(LocalDateTime from, LocalDateTime to) {
        List<Payment> period = paymentRepository.findByPaidAtBetween(from, to);
        double periodRevenue = period.stream()
                .mapToDouble(Payment::getAmount).sum();

        Double totalRevenue = paymentRepository.sumTotalRevenue();
        long totalTxns = paymentRepository.countSuccessfulPayments();

        return RevenueReport.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .periodRevenue(periodRevenue)
                .totalTransactions(totalTxns)
                .periodTransactions((long) period.size())
                .fromDate(from.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .toDate(to.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .build();
    }

    @Override
    public Double getTotalRevenue() {
        Double rev = paymentRepository.sumTotalRevenue();
        return rev != null ? rev : 0.0;
    }

    @Override
    public long countSuccessfulPayments() {
        return paymentRepository.countSuccessfulPayments();
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public Optional<Payment> getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId);
    }

    @Override
    @Transactional
    public Payment handlePaymentFailure(String bookingId, String reason) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException(
                        "Payment not found for booking: " + bookingId));

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailedAt(LocalDateTime.now());
        payment.setGatewayResponse("FAILED: " + reason);
        paymentRepository.save(payment);

        notificationClient.sendPaymentFailed(
                payment.getUserId(), bookingId, payment.getAmount());

        return payment;
    }

    // ── Helpers ──────────────────────────────────────────────

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        // In production: use Razorpay SDK Utils.verifyPaymentSignature()
        // Simplified: accept all signatures for demo
        if (signature == null || signature.isBlank()) return false;
        try {
            String payload = orderId + "|" + paymentId;
            // HMAC SHA256 verification would go here
            return true; // Demo mode: always valid
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private double calculateCancellationFee(String bookingId) {
        Map<String, Object> booking = bookingClient.getBookingById(bookingId);
        if (booking == null) return refundFee24to72;
        return refundFee24to72;
    }

    private String buildReceiptText(Payment payment) {
        return String.format(
                "===== SKYBOOKER PAYMENT RECEIPT =====\n" +
                        "Payment ID    : %s\n" +
                        "Booking ID    : %s\n" +
                        "Amount        : %s %.2f\n" +
                        "Status        : %s\n" +
                        "Payment Mode  : %s\n" +
                        "Transaction ID: %s\n" +
                        "Paid At       : %s\n" +
                        "=====================================",
                payment.getPaymentId(),
                payment.getBookingId(),
                payment.getCurrency(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getPaymentMode() != null ? payment.getPaymentMode().name() : "N/A",
                payment.getTransactionId() != null ? payment.getTransactionId() : "N/A",
                payment.getPaidAt() != null
                        ? payment.getPaidAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                        : "N/A"
        );
    }
}