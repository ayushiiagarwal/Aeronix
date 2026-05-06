package com.aeronix.booking_service.dto;

import lombok.Data;

@Data
public class AddOnRequest {
    private String type;       // LUGGAGE, MEAL, SEAT_UPGRADE
    private Double value;      // kg for luggage, 0 for meal
    private String mealType;   // VEG, NON_VEG, JAIN, VEGAN
}