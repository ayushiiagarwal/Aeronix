package com.aeronix.booking_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class AddOnRequest {
    @NotBlank
    @Pattern(regexp = "LUGGAGE|MEAL|SEAT_UPGRADE", message = "type must be LUGGAGE, MEAL, or SEAT_UPGRADE")
    private String type;

    @PositiveOrZero(message = "Value must be 0 or more")
    private Double value;      // kg for luggage, 0 for meal

    @Pattern(regexp = "VEG|NON_VEG|JAIN|VEGAN", message = "Invalid meal type")
    private String mealType;   // VEG, NON_VEG, JAIN, VEGAN
}