package com.aeronix.passenger_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class BulkPassengerRequest {

    @NotBlank
    private String bookingId;

    @NotEmpty
    private List<PassengerRequest> passengers;
}