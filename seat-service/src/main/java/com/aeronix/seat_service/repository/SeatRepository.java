package com.aeronix.seat_service.repository;

import com.aeronix.seat_service.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {

    List<Seat> findByFlightId(Integer flightId);

    List<Seat> findByFlightIdAndSeatClass(Integer flightId, Seat.SeatClass seatClass);

    Optional<Seat> findByFlightIdAndSeatNumber(Integer flightId, String seatNumber);

    @Query("SELECT s FROM Seat s WHERE s.flightId = :flightId AND s.status = 'AVAILABLE'")
    List<Seat> findAvailableByFlightId(@Param("flightId") Integer flightId);

    @Query("SELECT s FROM Seat s WHERE s.flightId = :flightId " +
            "AND s.seatClass = :seatClass AND s.status = 'AVAILABLE'")
    List<Seat> findAvailableByFlightIdAndClass(
            @Param("flightId") Integer flightId,
            @Param("seatClass") Seat.SeatClass seatClass);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.flightId = :flightId " +
            "AND s.seatClass = :seatClass AND s.status = 'AVAILABLE'")
    int countAvailableByClass(
            @Param("flightId") Integer flightId,
            @Param("seatClass") Seat.SeatClass seatClass);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.flightId = :flightId AND s.status = 'AVAILABLE'")
    int countAvailableByFlightId(@Param("flightId") Integer flightId);

    void deleteByFlightId(Integer flightId);

    // Find expired holds (held more than X minutes ago)
    @Query("SELECT s FROM Seat s WHERE s.status = 'HELD' AND s.heldAt < :expiry")
    List<Seat> findExpiredHolds(@Param("expiry") LocalDateTime expiry);

    List<Seat> findByHeldByUserId(String userId);

    @Query("SELECT s FROM Seat s WHERE s.flightId = :flightId AND s.status = 'HELD' " +
            "AND s.heldByUserId = :userId")
    List<Seat> findHeldByUserOnFlight(
            @Param("flightId") Integer flightId,
            @Param("userId") String userId);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.heldAt = null, s.heldByUserId = null " +
            "WHERE s.status = 'HELD' AND s.heldAt < :expiry")
    int releaseExpiredHolds(@Param("expiry") LocalDateTime expiry);
}