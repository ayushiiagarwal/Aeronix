package com.aeronix.airline_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "airlines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer airlineId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 3)
    private String iataCode;

    @Column(length = 4)
    private String icaoCode;

    private String logoUrl;

    @Column(nullable = false)
    private String country;

    private String contactEmail;

    private String contactPhone;

    private String website;

    private String description;

    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private boolean isActive = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}