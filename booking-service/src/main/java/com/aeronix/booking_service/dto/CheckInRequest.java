package com.aeronix.booking_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class CheckInRequest {
    private String bookingId;
    private List<Integer> newSeatIds;  // optional re-seat
}