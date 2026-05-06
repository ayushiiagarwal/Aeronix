package com.aeronix.payment_service.dto;

import lombok.Data;
import java.util.Map;

@Data
public class GatewayCallbackRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private String bookingId;
    private Map<String, Object> payload;
}