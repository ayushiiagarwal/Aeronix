package com.aeronix.booking_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {

    @NotNull
    private Integer flightId;

    private Integer returnFlightId;   // for round trips

    @NotNull
    private String seatClass;         // ECONOMY, BUSINESS, FIRST

    @NotEmpty
    private List<PassengerRequest> passengers;

    private String mealPreference;    // VEG, NON_VEG, JAIN, VEGAN
    private Double extraLuggageKg;

    @NotNull
    private String contactEmail;
    private String contactPhone;

    private String tripType;          // ONE_WAY, ROUND_TRIP

    // Seat IDs selected from seat map (one per passenger)
    private List<Integer> selectedSeatIds;
    private List<Integer> returnSelectedSeatIds;
}