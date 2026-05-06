package com.aeronix.passenger_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "passenger_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer passengerId;

    @Column(nullable = false)
    private String bookingId;

    private String title;           // Mr, Mrs, Ms, Dr

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private LocalDate dateOfBirth;

    private String gender;          // MALE, FEMALE, OTHER

    private String passportNumber;

    private String nationality;

    private LocalDate passportExpiry;

    private Integer seatId;

    private String seatNumber;

    private String ticketNumber;    // Generated at booking time

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PassengerType passengerType = PassengerType.ADULT;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MealPreference mealPreference = MealPreference.NONE;

    @Builder.Default
    private boolean checkedIn = false;

    private LocalDateTime checkedInAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum PassengerType {
        ADULT, CHILD, INFANT
    }

    public enum MealPreference {
        VEG, NON_VEG, JAIN, VEGAN, NONE
    }
}