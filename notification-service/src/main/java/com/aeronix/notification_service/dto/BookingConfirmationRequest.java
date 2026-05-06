package com.aeronix.notification_service.dto;

import lombok.Data;

@Data
public class BookingConfirmationRequest {
    private Integer recipientId;
    private String bookingId;
    private String pnrCode;
    private String email;
    private String phone;
    private String passengerName;
    private String flightNumber;
    private String origin;
    private String destination;
    private String departureTime;
    private String seatNumber;
    private String seatClass;
    private Double totalFare;
}