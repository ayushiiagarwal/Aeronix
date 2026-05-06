package com.aeronix.booking_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FareSummary {
    private Double baseFare;
    private Double gstAmount;
    private Double fuelSurcharge;
    private Double luggageCharge;
    private Double mealCharge;
    private Double seatUpgradeCharge;
    private Double totalTax;
    private Double totalFare;
    private String seatClass;
    private Integer passengerCount;
}