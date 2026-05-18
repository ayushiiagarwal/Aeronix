package com.aeronix.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @Column(length = 36)
    private String bookingId;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private Integer flightId;

    // For round trips, store return flight id
    private Integer returnFlightId;

    @Column(nullable = false, unique = true, length = 6)
    private String pnrCode;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TripType tripType = TripType.ONE_WAY;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false)
    private Double totalFare;

    @Column(nullable = false)
    private Double baseFare;

    @Column(nullable = false)
    private Double taxes;

    @Builder.Default
    private Double ancillaryCharges = 0.0;

    private String mealPreference;

    @Builder.Default
    private Double luggageKg = 0.0;

    @Builder.Default
    private Double luggageCharge = 0.0;

    private String seatClass;
    private String seatNumbers;

    @Column(nullable = false)
    private String contactEmail;

    private String contactPhone;

    @Column(nullable = false)
    private Integer passengerCount;

    @Builder.Default
    private LocalDateTime bookedAt = LocalDateTime.now();

    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;

    private String paymentId;
    private String cancellationReason;

    @Builder.Default
    private boolean checkedIn = false;
    private LocalDateTime checkedInAt;

    @Version
    private Integer version;

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW
    }

    public enum TripType {
        ONE_WAY, ROUND_TRIP
    }
}