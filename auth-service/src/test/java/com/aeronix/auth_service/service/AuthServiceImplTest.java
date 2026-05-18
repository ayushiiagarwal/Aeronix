package com.aeronix.auth_service.service;

import com.aeronix.auth_service.dto.*;
import com.aeronix.auth_service.entity.User;
import com.aeronix.auth_service.repository.UserRepository;
import com.aeronix.auth_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthServiceImpl authService;

    private User sampleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1).fullName("John Doe")
                .email("john@example.com").passwordHash("hashed_password")
                .phone("9999999999").role(User.Role.PASSENGER)
                .provider(User.Provider.LOCAL).isActive(true).build();

        registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setPhone("9999999999");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");
    }

    // ── register ──────────────────────────────────────────────

    @Test
    void register_success() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any())).thenReturn(sampleUser);
        when(jwtUtil.generateToken(any())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh_token");

        AuthResponse result = authService.register(registerRequest);

        assertThat(result.getAccessToken()).isEqualTo("access_token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getRole()).isEqualTo("PASSENGER");
    }

    @Test
    void register_default_role_is_passenger() {
        registerRequest.setRole(null);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            assertThat(u.getRole()).isEqualTo(User.Role.PASSENGER);
            return sampleUser;
        });
        when(jwtUtil.generateToken(any())).thenReturn("token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        authService.register(registerRequest);
    }

    @Test
    void register_with_role_airline_staff() {
        registerRequest.setRole("AIRLINE_STAFF");
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setRole(User.Role.AIRLINE_STAFF);
            return u;
        });
        when(jwtUtil.generateToken(any())).thenReturn("token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        AuthResponse result = authService.register(registerRequest);

        verify(userRepository).save(argThat(u -> u.getRole() == User.Role.AIRLINE_STAFF));
    }

    @Test
    void register_invalid_role_defaults_to_passenger() {
        registerRequest.setRole("SUPERUSER");
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(sampleUser);
        when(jwtUtil.generateToken(any())).thenReturn("token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        authService.register(registerRequest);

        verify(userRepository).save(argThat(u -> u.getRole() == User.Role.PASSENGER));
    }

    @Test
    void register_duplicate_email_throws() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void register_encodes_password() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any())).thenReturn(sampleUser);
        when(jwtUtil.generateToken(any())).thenReturn("token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        authService.register(registerRequest);

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("hashed_password")));
    }

    @Test
    void register_sets_provider_local() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(sampleUser);
        when(jwtUtil.generateToken(any())).thenReturn("token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        authService.register(registerRequest);

        verify(userRepository).save(argThat(u -> u.getProvider() == User.Provider.LOCAL));
    }

    @Test
    void register_sets_is_active_true() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(sampleUser);
        when(jwtUtil.generateToken(any())).thenReturn("token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        authService.register(registerRequest);

        verify(userRepository).save(argThat(User::isActive));
    }

    // ── login ─────────────────────────────────────────────────

    @Test
    void login_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh_token");

        AuthResponse result = authService.login(loginRequest);

        assertThat(result.getAccessToken()).isEqualTo("access_token");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getRole()).isEqualTo("PASSENGER");
        assertThat(result.getUserId()).isEqualTo(1);
    }

    @Test
    void login_user_not_found_throws() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        loginRequest.setEmail("unknown@example.com");

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void login_wrong_password_throws() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        loginRequest.setPassword("wrong_password");

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_deactivated_account_throws() {
        sampleUser.setActive(false);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account is deactivated");
    }

    @Test
    void login_returns_bearer_token_type() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        AuthResponse result = authService.login(loginRequest);

        assertThat(result.getTokenType()).isEqualTo("Bearer");
    }

    // ── logout ────────────────────────────────────────────────

    @Test
    void logout_does_not_throw() {
        assertThatCode(() -> authService.logout("some_token"))
                .doesNotThrowAnyException();
    }

    // ── validateToken ─────────────────────────────────────────

    @Test
    void validateToken_valid_returns_true() {
        when(jwtUtil.validateToken("valid_token")).thenReturn(true);

        assertThat(authService.validateToken("valid_token")).isTrue();
    }

    @Test
    void validateToken_invalid_returns_false() {
        when(jwtUtil.validateToken("invalid_token")).thenReturn(false);

        assertThat(authService.validateToken("invalid_token")).isFalse();
    }

    // ── refreshToken ──────────────────────────────────────────

    @Test
    void refreshToken_success() {
        when(jwtUtil.validateToken("valid_refresh")).thenReturn(true);
        when(jwtUtil.extractUserId("valid_refresh")).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        when(jwtUtil.generateToken(any())).thenReturn("new_access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("new_refresh");

        AuthResponse result = authService.refreshToken("valid_refresh");

        assertThat(result.getAccessToken()).isEqualTo("new_access");
        assertThat(result.getRefreshToken()).isEqualTo("new_refresh");
    }

    @Test
    void refreshToken_invalid_token_throws() {
        when(jwtUtil.validateToken("bad_token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("bad_token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refreshToken_user_not_found_throws() {
        when(jwtUtil.validateToken("valid_refresh")).thenReturn(true);
        when(jwtUtil.extractUserId("valid_refresh")).thenReturn("99");
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("valid_refresh"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── getUserById ───────────────────────────────────────────

    @Test
    void getUserById_found() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));

        User result = authService.getUserById(1);

        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void getUserById_not_found_throws() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── getUserByEmail ────────────────────────────────────────

    @Test
    void getUserByEmail_found() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));

        User result = authService.getUserByEmail("john@example.com");

        assertThat(result.getFullName()).isEqualTo("John Doe");
    }

    @Test
    void getUserByEmail_not_found_throws() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserByEmail("unknown@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── updateProfile ─────────────────────────────────────────

    @Test
    void updateProfile_success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("John Updated");
        req.setPhone("8888888888");
        req.setNationality("Indian");

        User result = authService.updateProfile(1, req);

        assertThat(result.getFullName()).isEqualTo("John Updated");
        assertThat(result.getPhone()).isEqualTo("8888888888");
        assertThat(result.getNationality()).isEqualTo("Indian");
    }

    @Test
    void updateProfile_partial_preserves_existing() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("New Name");

        User result = authService.updateProfile(1, req);

        assertThat(result.getFullName()).isEqualTo("New Name");
        assertThat(result.getPhone()).isEqualTo("9999999999");
    }

    @Test
    void updateProfile_not_found_throws() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateProfile(99, new UpdateProfileRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── changePassword ────────────────────────────────────────

    @Test
    void changePassword_success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("old_password", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("new_password")).thenReturn("new_hashed");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old_password");
        req.setNewPassword("new_password");

        assertThatCode(() -> authService.changePassword(1, req))
                .doesNotThrowAnyException();

        assertThat(sampleUser.getPasswordHash()).isEqualTo("new_hashed");
    }

    @Test
    void changePassword_wrong_old_password_throws() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong", "hashed_password")).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("new_password");

        assertThatThrownBy(() -> authService.changePassword(1, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void changePassword_not_found_throws() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old");
        req.setNewPassword("new");

        assertThatThrownBy(() -> authService.changePassword(99, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── deactivateAccount ─────────────────────────────────────

    @Test
    void deactivateAccount_success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.deactivateAccount(1);

        assertThat(sampleUser.isActive()).isFalse();
    }

    @Test
    void deactivateAccount_not_found_throws() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.deactivateAccount(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── reactivateUser ────────────────────────────────────────

    @Test
    void reactivateUser_success() {
        sampleUser.setActive(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.reactivateUser(1);

        assertThat(sampleUser.isActive()).isTrue();
    }

    @Test
    void reactivateUser_not_found_throws() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reactivateUser(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── getAllUsers ───────────────────────────────────────────

    @Test
    void getAllUsers_returns_list() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<User> result = authService.getAllUsers();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllUsers_empty() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<User> result = authService.getAllUsers();

        assertThat(result).isEmpty();
    }

    // ── getUsersByRole ────────────────────────────────────────

    @Test
    void getUsersByRole_passenger() {
        when(userRepository.findAllByRole(User.Role.PASSENGER)).thenReturn(List.of(sampleUser));

        List<User> result = authService.getUsersByRole("PASSENGER");

        assertThat(result).hasSize(1);
    }

    @Test
    void getUsersByRole_case_insensitive() {
        when(userRepository.findAllByRole(User.Role.ADMIN)).thenReturn(List.of());

        List<User> result = authService.getUsersByRole("admin");

        assertThat(result).isEmpty();
        verify(userRepository).findAllByRole(User.Role.ADMIN);
    }

    @Test
    void getUsersByRole_invalid_role_throws() {
        assertThatThrownBy(() -> authService.getUsersByRole("SUPERUSER"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── deleteUser ────────────────────────────────────────────

    @Test
    void deleteUser_calls_repository() {
        doNothing().when(userRepository).deleteById(1);

        authService.deleteUser(1);

        verify(userRepository).deleteById(1);
    }
}