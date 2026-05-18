package com.aeronix.booking_service.dto;

import lombok.*;
import java.io.Serializable;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationEvent implements Serializable {
    private Integer userId;
    private String  bookingId;
    private String  pnrCode;
    private String  email;
    private String  phone;
    private String  passengerName;
    private String  flightNumber;
    private String  origin;
    private String  destination;
    private String  departureTime;
    private String  seatNumber;
    private Double  totalFare;
    private Double  amount;
    private String  type;
    private String  reason;
}
