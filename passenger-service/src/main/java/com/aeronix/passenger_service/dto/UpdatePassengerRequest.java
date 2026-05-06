package com.aeronix.passenger_service.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdatePassengerRequest {
    private String title;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String passportNumber;
    private String nationality;
    private LocalDate passportExpiry;
    private String mealPreference;
}