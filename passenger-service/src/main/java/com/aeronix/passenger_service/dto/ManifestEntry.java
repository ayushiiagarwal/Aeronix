package com.aeronix.passenger_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManifestEntry {
    private Integer passengerId;
    private String bookingId;
    private String pnrCode;
    private String fullName;
    private String seatNumber;
    private String seatClass;
    private String mealPreference;
    private String passengerType;
    private boolean checkedIn;
    private String passportNumber;
    private String nationality;
}