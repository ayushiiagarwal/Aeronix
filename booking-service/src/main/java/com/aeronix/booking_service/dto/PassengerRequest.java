package com.aeronix.booking_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PassengerRequest {
    private String title;

    @NotBlank(message = "First name is required")
    private String firstName;
    private String lastName;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    private String gender;
    private String passportNumber;
    private String nationality;
    private LocalDate passportExpiry;

    @Pattern(regexp = "ADULT|CHILD|INFANT", message = "Passenger Type must be ADULT, CHILD, or INFANT")
    private String passengerType;   // ADULT, CHILD, INFANT

    @Pattern(regexp = "VEG|NON_VEG|JAIN|VEGAN",message = "Invalid meal preference")
    private String mealPreference;
}