package com.aeronix.passenger_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PassengerRequest {

    @NotNull
    private String bookingId;

    private String title;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotNull
    private LocalDate dateOfBirth;

    private String gender;

    private String passportNumber;

    private String nationality;

    private LocalDate passportExpiry;

    private String passengerType;    // ADULT, CHILD, INFANT

    private String mealPreference;   // VEG, NON_VEG, JAIN, VEGAN, NONE

    private Integer seatId;
    private String seatNumber;
}