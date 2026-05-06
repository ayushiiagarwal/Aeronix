package com.aeronix.seat_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"flight_id", "seat_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer seatId;

    @Column(nullable = false)
    private Integer flightId;

    @Column(nullable = false)
    private String seatNumber;   // e.g. 12A

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatClass seatClass;

    @Column(nullable = false)
    private Integer seatRow;

    @Column(nullable = false)
    private String seatColumn;       // A, B, C, D, E, F

    private boolean isWindow;
    private boolean isAisle;
    private boolean hasExtraLegroom;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Builder.Default
    private Double priceMultiplier = 1.0;

    // Optimistic locking
    @Version
    private Integer version;

    // Set when status = HELD
    private LocalDateTime heldAt;
    private String heldByUserId;

    public enum SeatClass {
        ECONOMY, BUSINESS, FIRST
    }

    public enum SeatStatus {
        AVAILABLE, HELD, CONFIRMED, BLOCKED
    }
}