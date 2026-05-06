package com.aeronix.booking_service.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PassengerRequest {
    private String title;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String passportNumber;
    private String nationality;
    private LocalDate passportExpiry;
    private String passengerType;   // ADULT, CHILD, INFANT
    private String mealPreference;
}