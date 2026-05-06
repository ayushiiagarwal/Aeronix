package com.aeronix.auth_service.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phone;
    private String passportNumber;
    private String nationality;
}