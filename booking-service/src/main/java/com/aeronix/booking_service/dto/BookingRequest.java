package com.aeronix.booking_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {

    @NotNull
    private Integer flightId;

    private Integer returnFlightId;   // for round trips

    @NotNull
    @Pattern(regexp = "ECONOMY|BUSINESS|FIRST", message = "Seat Class must be ECONOMY, BUSINESS, or FIRST")
    private String seatClass;         // ECONOMY, BUSINESS, FIRST

    @NotEmpty
    @Valid
    private List<PassengerRequest> passengers;

    @Pattern(regexp = "VEG|NON_VEG|JAIN|VEGAN", message = "Invalid meal preference")
    private String mealPreference;    // VEG, NON_VEG, JAIN, VEGAN

    @PositiveOrZero(message = "Extra luggage kg must be 0 or more")
    private Double extraLuggageKg;

    @NotBlank
    @Email(message = "Invalid contact email")
    private String contactEmail;
    private String contactPhone;

    @Pattern(regexp = "ONE_WAY|ROUND_TRIP", message = "tripType must be ONE_WAY or ROUND_TRIP")
    private String tripType;          // ONE_WAY, ROUND_TRIP

    // Seat IDs selected from seat map (one per passenger)
    private List<Integer> selectedSeatIds;
    private List<Integer> returnSelectedSeatIds;
}