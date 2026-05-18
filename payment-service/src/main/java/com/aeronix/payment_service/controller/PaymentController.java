package com.aeronix.payment_service.controller;

import com.aeronix.payment_service.dto.*;
import com.aeronix.payment_service.entity.Payment;
import com.aeronix.payment_service.service.PaymentService;
import com.aeronix.payment_service.service.PaymentServiceImpl;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentServiceImpl paymentServiceImpl;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ── Initiate Payment ─────────────────────────────────────

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiate(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiatePayment(request, userId));
    }

    // ── Simulate Payment (dev/test only) ─────────────────────

    @PostMapping("/simulate/{bookingId}")
    public ResponseEntity<Payment> simulate(@PathVariable String bookingId) {
        return ResponseEntity.ok(paymentServiceImpl.simulateSuccessfulPayment(bookingId));
    }

    // ── Payment Failure ──────────────────────────────────────

    @PostMapping("/failure")
    public ResponseEntity<Payment> handleFailure(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(paymentService.handlePaymentFailure(
                body.get("bookingId"), body.get("reason")));
    }

    // ── Refund ───────────────────────────────────────────────

    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refund(
            @Valid @RequestBody RefundRequest request,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(paymentService.refundPayment(request, userId));
    }

    // ── Razorpay ─────────────────────────────────────────────

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-User-Id") Integer userId) throws Exception {
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject options = new JSONObject();
        options.put("amount", ((Number) body.get("amount")).intValue() * 100);
        options.put("currency", "INR");
        options.put("receipt", body.get("bookingId").toString());
        options.put("payment_capture", 1);
        Order order = client.orders.create(options);
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.get("id"));
        response.put("amount", order.get("amount"));
        response.put("currency", order.get("currency"));
        response.put("keyId", razorpayKeyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyRazorpayPayment(
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") Integer userId) throws Exception {
        String data = body.get("razorpayOrderId") + "|" + body.get("razorpayPaymentId");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256"));
        byte[] digest = mac.doFinal(data.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        if (sb.toString().equals(body.get("razorpaySignature"))) {
            return ResponseEntity.ok(Map.of("status", "verified",
                    "paymentId", body.get("razorpayPaymentId")));
        }
        return ResponseEntity.status(400).body(Map.of("status", "failed"));
    }

    // ── Passenger Queries ────────────────────────────────────

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Payment> getByBooking(@PathVariable String bookingId) {
        return paymentService.getPaymentByBooking(bookingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my")
    public ResponseEntity<List<Payment>> getMyPayments(
            @RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<Map<String, String>> getStatus(
            @PathVariable String paymentId) {
        return ResponseEntity.ok(Map.of(
                "paymentId", paymentId,
                "status", paymentService.getPaymentStatus(paymentId)));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getById(@PathVariable String paymentId) {
        return paymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<Map<String, String>> getReceipt(
            @PathVariable String paymentId) {
        return ResponseEntity.ok(Map.of(
                "receipt", paymentService.generateReceipt(paymentId)));
    }

    // ── Admin ────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Payment>> getAll(
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/revenue")
    public ResponseEntity<RevenueReport> getRevenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        LocalDateTime start = from != null ? from : LocalDateTime.now().minusMonths(1);
        LocalDateTime end   = to   != null ? to   : LocalDateTime.now();
        return ResponseEntity.ok(paymentService.getRevenue(start, end));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(Map.of(
                "totalRevenue",      paymentService.getTotalRevenue(),
                "totalTransactions", paymentService.countSuccessfulPayments()
        ));
    }

    @PutMapping("/{paymentId}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable String paymentId,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Role") String role) {
        if (!role.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        paymentService.updatePaymentStatus(paymentId, body.get("status"));
        return ResponseEntity.ok(Map.of("message", "Payment status updated"));
    }
}