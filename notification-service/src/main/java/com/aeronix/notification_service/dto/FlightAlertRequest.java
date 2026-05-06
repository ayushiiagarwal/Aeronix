package com.aeronix.notification_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class FlightAlertRequest {
    private Integer flightId;
    private String alertType;       // FLIGHT_DELAY, GATE_CHANGE, FLIGHT_CANCELLED
    private String message;
    private String newDepartureTime;
    private String newGate;
    private Integer delayMinutes;
    private List<Integer> passengerIds;

    // lombok needs explicit import for List
    private java.util.List<Integer> recipientIds;
}