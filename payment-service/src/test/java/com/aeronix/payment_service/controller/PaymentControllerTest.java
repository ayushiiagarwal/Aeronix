package com.aeronix.payment_service.controller;

import com.aeronix.payment_service.dto.*;
import com.aeronix.payment_service.entity.Payment;
import com.aeronix.payment_service.service.PaymentService;
import com.aeronix.payment_service.service.PaymentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private PaymentService paymentService;
    @MockBean  private PaymentServiceImpl paymentServiceImpl;

    private ObjectMapper objectMapper;
    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        samplePayment = Payment.builder()
                .paymentId("pay-uuid-001")
                .bookingId("booking-uuid-001")
                .userId(1)
                .amount(5350.0)
                .currency("INR")
                .status(Payment.PaymentStatus.PAID)
                .paymentMode(Payment.PaymentMode.UPI)
                .gatewayOrderId("order_abc123")
                .refundAmount(0.0)
                .build();
    }

    @Test
    void initiatePayment_returns_201() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .paymentId("pay-uuid-001").bookingId("booking-uuid-001")
                .amount(5350.0).currency("INR").status("PENDING")
                .gatewayOrderId("order_abc123").razorpayKeyId("rzp_key")
                .message("Payment session created. Complete payment using gateway.")
                .build();

        when(paymentService.initiatePayment(any(), eq(1))).thenReturn(response);

        Map<String, Object> req = Map.of(
                "bookingId", "booking-uuid-001",
                "amount", 5350.0,
                "paymentMode", "UPI"
        );

        mockMvc.perform(post("/api/payments/initiate")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.gatewayOrderId").value("order_abc123"));
    }

    @Test
    void simulate_returns_200() throws Exception {
        when(paymentServiceImpl.simulateSuccessfulPayment("booking-uuid-001"))
                .thenReturn(samplePayment);

        mockMvc.perform(post("/api/payments/simulate/booking-uuid-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void handleFailure_returns_200() throws Exception {
        samplePayment.setStatus(Payment.PaymentStatus.FAILED);
        when(paymentService.handlePaymentFailure("booking-uuid-001", "Declined"))
                .thenReturn(samplePayment);

        mockMvc.perform(post("/api/payments/failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"booking-uuid-001\",\"reason\":\"Declined\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void refund_returns_200() throws Exception {
        RefundResponse response = RefundResponse.builder()
                .paymentId("pay-uuid-001").bookingId("booking-uuid-001")
                .refundAmount(4012.5).cancellationFee(1337.5)
                .refundTransactionId("rfnd_abc123").status("PARTIALLY_REFUNDED")
                .message("Refund initiated successfully")
                .estimatedCreditDays("5-7 working days").build();

        when(paymentService.refundPayment(any(), eq(1))).thenReturn(response);

        Map<String, Object> req = Map.of(
                "bookingId", "booking-uuid-001",
                "reason", "Changed plans"
        );

        mockMvc.perform(post("/api/payments/refund")
                        .header("X-User-Id", 1)
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundAmount").value(4012.5));
    }

    @Test
    void getByBooking_found_returns_200() throws Exception {
        when(paymentService.getPaymentByBooking("booking-uuid-001"))
                .thenReturn(Optional.of(samplePayment));

        mockMvc.perform(get("/api/payments/booking/booking-uuid-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("pay-uuid-001"));
    }

    @Test
    void getByBooking_not_found_returns_404() throws Exception {
        when(paymentService.getPaymentByBooking("nonexistent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/booking/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyPayments_returns_200() throws Exception {
        when(paymentService.getPaymentsByUser(1)).thenReturn(List.of(samplePayment));

        mockMvc.perform(get("/api/payments/my").header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getStatus_returns_200() throws Exception {
        when(paymentService.getPaymentStatus("pay-uuid-001")).thenReturn("PAID");

        mockMvc.perform(get("/api/payments/pay-uuid-001/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentId").value("pay-uuid-001"));
    }

    @Test
    void getById_found_returns_200() throws Exception {
        when(paymentService.getPaymentById("pay-uuid-001"))
                .thenReturn(Optional.of(samplePayment));

        mockMvc.perform(get("/api/payments/pay-uuid-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("pay-uuid-001"));
    }

    @Test
    void getById_not_found_returns_404() throws Exception {
        when(paymentService.getPaymentById("nonexistent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReceipt_returns_200() throws Exception {
        when(paymentService.generateReceipt("pay-uuid-001")).thenReturn("===RECEIPT===");

        mockMvc.perform(get("/api/payments/pay-uuid-001/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receipt").value("===RECEIPT==="));
    }

    @Test
    void getAll_admin_returns_200() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(List.of(samplePayment));

        mockMvc.perform(get("/api/payments").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAll_non_admin_returns_403() throws Exception {
        mockMvc.perform(get("/api/payments").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRevenue_admin_returns_200() throws Exception {
        RevenueReport report = RevenueReport.builder()
                .totalRevenue(150000.0).periodRevenue(5350.0)
                .totalTransactions(30L).periodTransactions(1L)
                .fromDate("2026-05-01").toDate("2026-05-15").build();

        when(paymentService.getRevenue(any(), any())).thenReturn(report);

        mockMvc.perform(get("/api/payments/revenue").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(150000.0));
    }

    @Test
    void getRevenue_non_admin_returns_403() throws Exception {
        mockMvc.perform(get("/api/payments/revenue").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAnalytics_admin_returns_200() throws Exception {
        when(paymentService.getTotalRevenue()).thenReturn(250000.0);
        when(paymentService.countSuccessfulPayments()).thenReturn(50L);

        mockMvc.perform(get("/api/payments/analytics").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(250000.0))
                .andExpect(jsonPath("$.totalTransactions").value(50));
    }

    @Test
    void getAnalytics_non_admin_returns_403() throws Exception {
        mockMvc.perform(get("/api/payments/analytics").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_admin_returns_200() throws Exception {
        doNothing().when(paymentService).updatePaymentStatus("pay-uuid-001", "FAILED");

        mockMvc.perform(put("/api/payments/pay-uuid-001/status")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FAILED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment status updated"));
    }

    @Test
    void updateStatus_non_admin_returns_403() throws Exception {
        mockMvc.perform(put("/api/payments/pay-uuid-001/status")
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FAILED\"}"))
                .andExpect(status().isForbidden());
    }
}