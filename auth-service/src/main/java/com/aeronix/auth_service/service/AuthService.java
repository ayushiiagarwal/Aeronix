package com.aeronix.auth_service.service;

import com.aeronix.auth_service.dto.*;
import com.aeronix.auth_service.entity.User;

import java.util.List;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String token);
    boolean validateToken(String token);
    AuthResponse refreshToken(String refreshToken);
    User getUserById(Integer userId);
    User getUserByEmail(String email);
    User updateProfile(Integer userId, UpdateProfileRequest request);
    void changePassword(Integer userId, ChangePasswordRequest request);
    void deactivateAccount(Integer userId);
    List<User> getAllUsers();
    List<User> getUsersByRole(String role);
    void reactivateUser(Integer userId);
    void deleteUser(Integer userId);
}