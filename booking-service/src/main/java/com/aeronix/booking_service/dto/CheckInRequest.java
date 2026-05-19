package com.aeronix.booking_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CheckInRequest {

    @NotBlank(message = "Booking ID is required")
    private String bookingId;

    private List<Integer> newSeatIds;  // optional re-seat
}