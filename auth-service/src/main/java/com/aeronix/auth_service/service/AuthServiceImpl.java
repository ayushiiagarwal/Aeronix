package com.aeronix.auth_service.service;

import com.aeronix.auth_service.dto.*;
import com.aeronix.auth_service.entity.User;
import com.aeronix.auth_service.repository.UserRepository;
import com.aeronix.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User.Role role = User.Role.PASSENGER;
        if (request.getRole() != null) {
            try { role = User.Role.valueOf(request.getRole().toUpperCase()); }
            catch (Exception ignored) {}
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .passportNumber(request.getPassportNumber())
                .nationality(request.getNationality())
                .role(role)
                .provider(User.Provider.LOCAL)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(jwtUtil.generateToken(user))
                .refreshToken(jwtUtil.generateRefreshToken(user))
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return AuthResponse.builder()
                .accessToken(jwtUtil.generateToken(user))
                .refreshToken(jwtUtil.generateRefreshToken(user))
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public void logout(String token) {
        // Stateless JWT — client discards token.
        // In production: add token to a Redis blacklist here.
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        String userId = jwtUtil.extractUserId(refreshToken);
        User user = userRepository.findById(Integer.parseInt(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthResponse.builder()
                .accessToken(jwtUtil.generateToken(user))
                .refreshToken(jwtUtil.generateRefreshToken(user))
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @Override
    @Transactional
    public User updateProfile(Integer userId, UpdateProfileRequest request) {
        User user = getUserById(userId);
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getPassportNumber() != null) user.setPassportNumber(request.getPassportNumber());
        if (request.getNationality() != null) user.setNationality(request.getNationality());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateAccount(Integer userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> getUsersByRole(String role) {
        return userRepository.findAllByRole(User.Role.valueOf(role.toUpperCase()));
    }

    @Override
    @Transactional
    public void reactivateUser(Integer userId) {
        User user = getUserById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
    }
}
