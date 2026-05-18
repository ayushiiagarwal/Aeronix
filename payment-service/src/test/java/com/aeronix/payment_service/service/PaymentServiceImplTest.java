package com.aeronix.payment_service.service;

import com.aeronix.payment_service.client.BookingClient;
import com.aeronix.payment_service.client.NotificationClient;
import com.aeronix.payment_service.dto.*;
import com.aeronix.payment_service.entity.Payment;
import com.aeronix.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingClient bookingClient;
    @Mock private NotificationClient notificationClient;

    @InjectMocks private PaymentServiceImpl paymentService;

    private Payment pendingPayment;
    private Payment paidPayment;
    private Map<String, Object> mockBooking;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "rzp_test_secret");
        ReflectionTestUtils.setField(paymentService, "defaultCurrency", "INR");
        ReflectionTestUtils.setField(paymentService, "refundFee0to4", 100);
        ReflectionTestUtils.setField(paymentService, "refundFee4to24", 50);
        ReflectionTestUtils.setField(paymentService, "refundFee24to72", 25);
        ReflectionTestUtils.setField(paymentService, "refundFee72plus", 0);

        pendingPayment = Payment.builder()
                .paymentId("pay-uuid-001")
                .bookingId("booking-uuid-001")
                .userId(1)
                .amount(5350.0)
                .currency("INR")
                .status(Payment.PaymentStatus.PENDING)
                .paymentMode(Payment.PaymentMode.UPI)
                .gatewayOrderId("order_abc123")
                .initiatedAt(LocalDateTime.now())
                .refundAmount(0.0)
                .build();

        paidPayment = Payment.builder()
                .paymentId("pay-uuid-001")
                .bookingId("booking-uuid-001")
                .userId(1)
                .amount(5350.0)
                .currency("INR")
                .status(Payment.PaymentStatus.PAID)
                .paymentMode(Payment.PaymentMode.UPI)
                .gatewayOrderId("order_abc123")
                .gatewayPaymentId("pay_gw_001")
                .transactionId("TXN001")
                .paidAt(LocalDateTime.now())
                .refundAmount(0.0)
                .build();

        mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "booking-uuid-001");
        mockBooking.put("status", "PENDING");
        mockBooking.put("pnrCode", "ABC123");
        mockBooking.put("userId", 1);
    }

    // ── initiatePayment ───────────────────────────────────────

    @Test
    void initiatePayment_success() {
        when(bookingClient.getBookingById("booking-uuid-001")).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId("booking-uuid-001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId("booking-uuid-001");
        req.setAmount(5350.0);
        req.setPaymentMode("UPI");

        PaymentResponse result = paymentService.initiatePayment(req, 1);

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo("booking-uuid-001");
        assertThat(result.getAmount()).isEqualTo(5350.0);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getRazorpayKeyId()).isEqualTo("rzp_test_key");
        assertThat(result.getGatewayOrderId()).startsWith("order_");
    }

    @Test
    void initiatePayment_uses_default_currency_when_not_provided() {
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(any())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId("booking-uuid-001");
        req.setAmount(5350.0);
        req.setPaymentMode("CARD");
        req.setCurrency(null);

        PaymentResponse result = paymentService.initiatePayment(req, 1);

        assertThat(result.getCurrency()).isEqualTo("INR");
    }

    @Test
    void initiatePayment_uses_provided_currency() {
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(any())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId("booking-uuid-001");
        req.setAmount(5350.0);
        req.setPaymentMode("CARD");
        req.setCurrency("USD");

        PaymentResponse result = paymentService.initiatePayment(req, 1);

        assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    void initiatePayment_booking_not_found_throws() {
        when(bookingClient.getBookingById("nonexistent")).thenReturn(null);

        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId("nonexistent");
        req.setAmount(5350.0);
        req.setPaymentMode("UPI");

        assertThatThrownBy(() -> paymentService.initiatePayment(req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void initiatePayment_booking_not_pending_throws() {
        mockBooking.put("status", "CONFIRMED");
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId("booking-uuid-001");
        req.setAmount(5350.0);
        req.setPaymentMode("UPI");

        assertThatThrownBy(() -> paymentService.initiatePayment(req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not in PENDING state");
    }

    @Test
    void initiatePayment_booking_cancelled_throws() {
        mockBooking.put("status", "CANCELLED");
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId("booking-uuid-001");
        req.setAmount(5350.0);
        req.setPaymentMode("UPI");

        assertThatThrownBy(() -> paymentService.initiatePayment(req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not in PENDING state");
    }

    @Test
    void initiatePayment_already_paid_throws() {
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(any())).thenReturn(Optional.of(paidPayment));

        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId("booking-uuid-001");
        req.setAmount(5350.0);
        req.setPaymentMode("UPI");

        assertThatThrownBy(() -> paymentService.initiatePayment(req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void initiatePayment_existing_pending_payment_allowed() {
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(any())).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId("booking-uuid-001");
        req.setAmount(5350.0);
        req.setPaymentMode("UPI");

        PaymentResponse result = paymentService.initiatePayment(req, 1);

        assertThat(result).isNotNull();
    }

    @Test
    void initiatePayment_all_payment_modes() {
        for (String mode : List.of("CARD", "UPI", "NETBANKING", "WALLET")) {
            when(bookingClient.getBookingById(any())).thenReturn(mockBooking);
            when(paymentRepository.findByBookingId(any())).thenReturn(Optional.empty());
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            InitiatePaymentRequest req = new InitiatePaymentRequest();
            req.setBookingId("booking-uuid-001");
            req.setAmount(5350.0);
            req.setPaymentMode(mode);

            assertThatCode(() -> paymentService.initiatePayment(req, 1))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void initiatePayment_saves_payment_to_repository() {
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);
        when(paymentRepository.findByBookingId(any())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId("booking-uuid-001");
        req.setAmount(5350.0);
        req.setPaymentMode("UPI");

        paymentService.initiatePayment(req, 1);

        verify(paymentRepository).save(any(Payment.class));
    }

    // ── processPayment ────────────────────────────────────────

    @Test
    void processPayment_valid_signature_success() {
        when(paymentRepository.findByGatewayOrderId("order_abc123"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        GatewayCallbackRequest req = new GatewayCallbackRequest();
        req.setRazorpayOrderId("order_abc123");
        req.setRazorpayPaymentId("pay_gw_001");
        req.setRazorpaySignature("valid_signature");

        Payment result = paymentService.processPayment(req);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        assertThat(result.getGatewayPaymentId()).isEqualTo("pay_gw_001");
        assertThat(result.getPaidAt()).isNotNull();
    }

    @Test
    void processPayment_confirms_booking() {
        when(paymentRepository.findByGatewayOrderId("order_abc123"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        GatewayCallbackRequest req = new GatewayCallbackRequest();
        req.setRazorpayOrderId("order_abc123");
        req.setRazorpayPaymentId("pay_gw_001");
        req.setRazorpaySignature("valid_signature");

        paymentService.processPayment(req);

        verify(bookingClient).confirmBooking("booking-uuid-001", "pay-uuid-001");
    }

    @Test
    void processPayment_sends_success_notification() {
        when(paymentRepository.findByGatewayOrderId("order_abc123"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        GatewayCallbackRequest req = new GatewayCallbackRequest();
        req.setRazorpayOrderId("order_abc123");
        req.setRazorpayPaymentId("pay_gw_001");
        req.setRazorpaySignature("valid_signature");

        paymentService.processPayment(req);

        verify(notificationClient).sendPaymentSuccess(1, "booking-uuid-001", "ABC123", 5350.0);
    }

    @Test
    void processPayment_null_signature_fails() {
        when(paymentRepository.findByGatewayOrderId("order_abc123"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GatewayCallbackRequest req = new GatewayCallbackRequest();
        req.setRazorpayOrderId("order_abc123");
        req.setRazorpayPaymentId("pay_gw_001");
        req.setRazorpaySignature(null);

        assertThatThrownBy(() -> paymentService.processPayment(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("signature verification failed");

        verify(notificationClient).sendPaymentFailed(1, "booking-uuid-001", 5350.0);
    }

    @Test
    void processPayment_blank_signature_fails() {
        when(paymentRepository.findByGatewayOrderId("order_abc123"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GatewayCallbackRequest req = new GatewayCallbackRequest();
        req.setRazorpayOrderId("order_abc123");
        req.setRazorpayPaymentId("pay_gw_001");
        req.setRazorpaySignature("   ");

        assertThatThrownBy(() -> paymentService.processPayment(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("signature verification failed");
    }

    @Test
    void processPayment_order_not_found_throws() {
        when(paymentRepository.findByGatewayOrderId("nonexistent"))
                .thenReturn(Optional.empty());

        GatewayCallbackRequest req = new GatewayCallbackRequest();
        req.setRazorpayOrderId("nonexistent");
        req.setRazorpayPaymentId("pay_gw_001");
        req.setRazorpaySignature("sig");

        assertThatThrownBy(() -> paymentService.processPayment(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found for order");
    }

    @Test
    void processPayment_sets_failed_status_on_bad_signature() {
        when(paymentRepository.findByGatewayOrderId("order_abc123"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GatewayCallbackRequest req = new GatewayCallbackRequest();
        req.setRazorpayOrderId("order_abc123");
        req.setRazorpayPaymentId("pay_gw_001");
        req.setRazorpaySignature(null);

        assertThatThrownBy(() -> paymentService.processPayment(req));

        assertThat(pendingPayment.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        assertThat(pendingPayment.getFailedAt()).isNotNull();
    }

    @Test
    void processPayment_booking_null_uses_na_pnr() {
        when(paymentRepository.findByGatewayOrderId("order_abc123"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(null);

        GatewayCallbackRequest req = new GatewayCallbackRequest();
        req.setRazorpayOrderId("order_abc123");
        req.setRazorpayPaymentId("pay_gw_001");
        req.setRazorpaySignature("valid_sig");

        paymentService.processPayment(req);

        verify(notificationClient).sendPaymentSuccess(eq(1), any(), eq("N/A"), eq(5350.0));
    }

    // ── simulateSuccessfulPayment ─────────────────────────────

    @Test
    void simulateSuccessfulPayment_success() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        Payment result = paymentService.simulateSuccessfulPayment("booking-uuid-001");

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        assertThat(result.getGatewayResponse()).isEqualTo("SIMULATED_SUCCESS");
        assertThat(result.getTransactionId()).startsWith("TXN_");
        assertThat(result.getGatewayPaymentId()).startsWith("pay_sim_");
        assertThat(result.getPaidAt()).isNotNull();
    }

    @Test
    void simulateSuccessfulPayment_confirms_booking() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        paymentService.simulateSuccessfulPayment("booking-uuid-001");

        verify(bookingClient).confirmBooking("booking-uuid-001", "pay-uuid-001");
    }

    @Test
    void simulateSuccessfulPayment_sends_notification() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        paymentService.simulateSuccessfulPayment("booking-uuid-001");

        verify(notificationClient).sendPaymentSuccess(eq(1), eq("booking-uuid-001"), eq("ABC123"), eq(5350.0));
    }

    @Test
    void simulateSuccessfulPayment_not_found_throws() {
        when(paymentRepository.findByBookingId("nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.simulateSuccessfulPayment("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void simulateSuccessfulPayment_null_booking_uses_na_pnr() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(null);

        paymentService.simulateSuccessfulPayment("booking-uuid-001");

        verify(notificationClient).sendPaymentSuccess(eq(1), any(), eq("N/A"), eq(5350.0));
    }

    // ── refundPayment ─────────────────────────────────────────

    @Test
    void refundPayment_full_refund_success() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(paidPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        RefundRequest req = new RefundRequest();
        req.setBookingId("booking-uuid-001");
        req.setReason("Changed plans");

        RefundResponse result = paymentService.refundPayment(req, 1);

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo("booking-uuid-001");
        assertThat(result.getRefundAmount()).isGreaterThan(0);
        assertThat(result.getCancellationFee()).isGreaterThanOrEqualTo(0);
        assertThat(result.getRefundTransactionId()).startsWith("rfnd_");
        assertThat(result.getMessage()).isEqualTo("Refund initiated successfully");
        assertThat(result.getEstimatedCreditDays()).isEqualTo("5-7 working days");
    }

    @Test
    void refundPayment_applies_25_percent_cancellation_fee() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(paidPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        RefundRequest req = new RefundRequest();
        req.setBookingId("booking-uuid-001");
        req.setReason("Changed plans");

        RefundResponse result = paymentService.refundPayment(req, 1);

        assertThat(result.getCancellationFee()).isEqualTo(5350.0 * 25 / 100);
        assertThat(result.getRefundAmount()).isEqualTo(5350.0 - result.getCancellationFee());
    }

    @Test
    void refundPayment_partial_refund_capped_at_paid_minus_fee() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(paidPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        RefundRequest req = new RefundRequest();
        req.setBookingId("booking-uuid-001");
        req.setReason("Changed plans");
        req.setRefundAmount(1000.0); // less than max refundable

        RefundResponse result = paymentService.refundPayment(req, 1);

        assertThat(result.getRefundAmount()).isEqualTo(1000.0);
    }

    @Test
    void refundPayment_status_partially_refunded_when_not_full() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(paidPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        RefundRequest req = new RefundRequest();
        req.setBookingId("booking-uuid-001");
        req.setReason("Changed plans");
        req.setRefundAmount(1000.0);

        RefundResponse result = paymentService.refundPayment(req, 1);

        assertThat(result.getStatus()).isEqualTo("PARTIALLY_REFUNDED");
    }

    @Test
    void refundPayment_not_paid_throws() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(pendingPayment));

        RefundRequest req = new RefundRequest();
        req.setBookingId("booking-uuid-001");
        req.setReason("Changed plans");

        assertThatThrownBy(() -> paymentService.refundPayment(req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot refund payment with status");
    }

    @Test
    void refundPayment_failed_status_throws() {
        pendingPayment.setStatus(Payment.PaymentStatus.FAILED);
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(pendingPayment));

        RefundRequest req = new RefundRequest();
        req.setBookingId("booking-uuid-001");
        req.setReason("Changed plans");

        assertThatThrownBy(() -> paymentService.refundPayment(req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot refund");
    }

    @Test
    void refundPayment_not_found_throws() {
        when(paymentRepository.findByBookingId("nonexistent"))
                .thenReturn(Optional.empty());

        RefundRequest req = new RefundRequest();
        req.setBookingId("nonexistent");
        req.setReason("reason");

        assertThatThrownBy(() -> paymentService.refundPayment(req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void refundPayment_updates_booking_to_cancelled() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(paidPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        RefundRequest req = new RefundRequest();
        req.setBookingId("booking-uuid-001");
        req.setReason("Cancel");

        paymentService.refundPayment(req, 1);

        verify(bookingClient).updateBookingStatus("booking-uuid-001", "CANCELLED");
    }

    @Test
    void refundPayment_sends_refund_notification() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(paidPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingClient.getBookingById(any())).thenReturn(mockBooking);

        RefundRequest req = new RefundRequest();
        req.setBookingId("booking-uuid-001");
        req.setReason("Cancel");

        paymentService.refundPayment(req, 1);

        verify(notificationClient).sendRefundInitiated(eq(1), eq("booking-uuid-001"), anyDouble());
    }

    // ── getPaymentByBooking ───────────────────────────────────

    @Test
    void getPaymentByBooking_found() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(paidPayment));

        Optional<Payment> result = paymentService.getPaymentByBooking("booking-uuid-001");

        assertThat(result).isPresent();
        assertThat(result.get().getPaymentId()).isEqualTo("pay-uuid-001");
    }

    @Test
    void getPaymentByBooking_not_found() {
        when(paymentRepository.findByBookingId("nonexistent"))
                .thenReturn(Optional.empty());

        Optional<Payment> result = paymentService.getPaymentByBooking("nonexistent");

        assertThat(result).isEmpty();
    }

    // ── getPaymentsByUser ─────────────────────────────────────

    @Test
    void getPaymentsByUser_returns_list() {
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(1))
                .thenReturn(List.of(paidPayment));

        List<Payment> result = paymentService.getPaymentsByUser(1);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPaymentsByUser_no_payments_returns_empty() {
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(99))
                .thenReturn(Collections.emptyList());

        List<Payment> result = paymentService.getPaymentsByUser(99);

        assertThat(result).isEmpty();
    }

    // ── getPaymentStatus ──────────────────────────────────────

    @Test
    void getPaymentStatus_paid() {
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(paidPayment));

        String status = paymentService.getPaymentStatus("pay-uuid-001");

        assertThat(status).isEqualTo("PAID");
    }

    @Test
    void getPaymentStatus_pending() {
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(pendingPayment));

        String status = paymentService.getPaymentStatus("pay-uuid-001");

        assertThat(status).isEqualTo("PENDING");
    }

    @Test
    void getPaymentStatus_not_found_throws() {
        when(paymentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    // ── updatePaymentStatus ───────────────────────────────────

    @Test
    void updatePaymentStatus_to_failed() {
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.updatePaymentStatus("pay-uuid-001", "FAILED");

        assertThat(pendingPayment.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
    }

    @Test
    void updatePaymentStatus_to_refunded() {
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(paidPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.updatePaymentStatus("pay-uuid-001", "REFUNDED");

        assertThat(paidPayment.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
    }

    @Test
    void updatePaymentStatus_invalid_throws() {
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.updatePaymentStatus("pay-uuid-001", "INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatePaymentStatus_not_found_throws() {
        when(paymentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.updatePaymentStatus("nonexistent", "FAILED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    // ── generateReceipt ───────────────────────────────────────

    @Test
    void generateReceipt_contains_payment_id() {
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(paidPayment));

        String receipt = paymentService.generateReceipt("pay-uuid-001");

        assertThat(receipt).contains("pay-uuid-001");
    }

    @Test
    void generateReceipt_contains_booking_id() {
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(paidPayment));

        String receipt = paymentService.generateReceipt("pay-uuid-001");

        assertThat(receipt).contains("booking-uuid-001");
    }

    @Test
    void generateReceipt_contains_amount() {
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(paidPayment));

        String receipt = paymentService.generateReceipt("pay-uuid-001");

        assertThat(receipt).contains("5350.00");
    }

    @Test
    void generateReceipt_contains_status() {
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(paidPayment));

        String receipt = paymentService.generateReceipt("pay-uuid-001");

        assertThat(receipt).contains("PAID");
    }

    @Test
    void generateReceipt_not_found_throws() {
        when(paymentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.generateReceipt("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void generateReceipt_null_paid_at_shows_na() {
        paidPayment.setPaidAt(null);
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(paidPayment));

        String receipt = paymentService.generateReceipt("pay-uuid-001");

        assertThat(receipt).contains("N/A");
    }

    // ── getRevenue ────────────────────────────────────────────

    @Test
    void getRevenue_returns_report() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to   = LocalDateTime.now();

        when(paymentRepository.findByPaidAtBetween(from, to)).thenReturn(List.of(paidPayment));
        when(paymentRepository.sumTotalRevenue()).thenReturn(150000.0);
        when(paymentRepository.countSuccessfulPayments()).thenReturn(30L);

        RevenueReport report = paymentService.getRevenue(from, to);

        assertThat(report.getTotalRevenue()).isEqualTo(150000.0);
        assertThat(report.getPeriodRevenue()).isEqualTo(5350.0);
        assertThat(report.getTotalTransactions()).isEqualTo(30L);
        assertThat(report.getPeriodTransactions()).isEqualTo(1L);
    }

    @Test
    void getRevenue_null_total_revenue_returns_zero() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to   = LocalDateTime.now();

        when(paymentRepository.findByPaidAtBetween(from, to)).thenReturn(Collections.emptyList());
        when(paymentRepository.sumTotalRevenue()).thenReturn(null);
        when(paymentRepository.countSuccessfulPayments()).thenReturn(0L);

        RevenueReport report = paymentService.getRevenue(from, to);

        assertThat(report.getTotalRevenue()).isEqualTo(0.0);
        assertThat(report.getPeriodRevenue()).isEqualTo(0.0);
    }

    // ── getTotalRevenue ───────────────────────────────────────

    @Test
    void getTotalRevenue_returns_value() {
        when(paymentRepository.sumTotalRevenue()).thenReturn(250000.0);

        assertThat(paymentService.getTotalRevenue()).isEqualTo(250000.0);
    }

    @Test
    void getTotalRevenue_null_returns_zero() {
        when(paymentRepository.sumTotalRevenue()).thenReturn(null);

        assertThat(paymentService.getTotalRevenue()).isEqualTo(0.0);
    }

    // ── countSuccessfulPayments ───────────────────────────────

    @Test
    void countSuccessfulPayments_returns_count() {
        when(paymentRepository.countSuccessfulPayments()).thenReturn(42L);

        assertThat(paymentService.countSuccessfulPayments()).isEqualTo(42L);
    }

    @Test
    void countSuccessfulPayments_zero() {
        when(paymentRepository.countSuccessfulPayments()).thenReturn(0L);

        assertThat(paymentService.countSuccessfulPayments()).isEqualTo(0L);
    }

    // ── getAllPayments ─────────────────────────────────────────

    @Test
    void getAllPayments_returns_all() {
        when(paymentRepository.findAll()).thenReturn(List.of(pendingPayment, paidPayment));

        List<Payment> result = paymentService.getAllPayments();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllPayments_empty() {
        when(paymentRepository.findAll()).thenReturn(Collections.emptyList());

        List<Payment> result = paymentService.getAllPayments();

        assertThat(result).isEmpty();
    }

    // ── getPaymentById ────────────────────────────────────────

    @Test
    void getPaymentById_found() {
        when(paymentRepository.findById("pay-uuid-001")).thenReturn(Optional.of(paidPayment));

        Optional<Payment> result = paymentService.getPaymentById("pay-uuid-001");

        assertThat(result).isPresent();
    }

    @Test
    void getPaymentById_not_found() {
        when(paymentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<Payment> result = paymentService.getPaymentById("nonexistent");

        assertThat(result).isEmpty();
    }

    // ── handlePaymentFailure ──────────────────────────────────

    @Test
    void handlePaymentFailure_marks_failed() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.handlePaymentFailure("booking-uuid-001", "Insufficient balance");

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        assertThat(result.getFailedAt()).isNotNull();
        assertThat(result.getGatewayResponse()).contains("Insufficient balance");
    }

    @Test
    void handlePaymentFailure_sends_notification() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.handlePaymentFailure("booking-uuid-001", "Bank declined");

        verify(notificationClient).sendPaymentFailed(1, "booking-uuid-001", 5350.0);
    }

    @Test
    void handlePaymentFailure_not_found_throws() {
        when(paymentRepository.findByBookingId("nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.handlePaymentFailure("nonexistent", "reason"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void handlePaymentFailure_stores_reason_in_gateway_response() {
        when(paymentRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.handlePaymentFailure("booking-uuid-001", "Card expired");

        assertThat(pendingPayment.getGatewayResponse()).isEqualTo("FAILED: Card expired");
    }
}