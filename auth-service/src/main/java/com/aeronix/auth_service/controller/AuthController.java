package com.aeronix.auth_service.controller;

import com.aeronix.auth_service.dto.*;
import com.aeronix.auth_service.entity.User;
import com.aeronix.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Public ──────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refreshToken(body.get("refreshToken")));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validate(@RequestBody Map<String, String> body) {
        boolean valid = authService.validateToken(body.get("token"));
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    // ── Authenticated ────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader.substring(7));
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("X-User-Id") Integer userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PutMapping("/deactivate")
    public ResponseEntity<Map<String, String>> deactivate(
            @RequestHeader("X-User-Id") Integer userId) {
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account deactivated"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Integer userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @GetMapping("/user/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    // ── Admin Only ───────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/users/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }

    @PutMapping("/users/{userId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reactivateUser(@PathVariable Integer userId) {
        authService.reactivateUser(userId);
        return ResponseEntity.ok(Map.of("message", "User reactivated"));
    }

    @PutMapping("/users/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable Integer userId) {
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(Map.of("message", "User deactivated"));
    }
}