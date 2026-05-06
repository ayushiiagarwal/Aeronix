package com.aeronix.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String fullName;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6)
    private String password;

    private String phone;
    private String passportNumber;
    private String nationality;

    private String role; // PASSENGER, AIRLINE_STAFF, ADMIN
}