package com.aeronix.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String passwordHash;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Provider provider = Provider.LOCAL;

    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private boolean isActive = true;

    private String passportNumber;

    private String nationality;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role {
        PASSENGER, AIRLINE_STAFF, ADMIN
    }

    public enum Provider {
        LOCAL, GOOGLE
    }
}