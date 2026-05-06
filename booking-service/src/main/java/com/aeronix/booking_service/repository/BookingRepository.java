package com.aeronix.booking_service.repository;

import com.aeronix.booking_service.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByUserId(Integer userId);

    Optional<Booking> findByPnrCode(String pnrCode);

    List<Booking> findByFlightId(Integer flightId);

    List<Booking> findByStatus(Booking.BookingStatus status);

    int countByFlightIdAndStatus(Integer flightId, Booking.BookingStatus status);

    List<Booking> findByUserIdAndStatus(Integer userId, Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.userId = :userId " +
            "AND b.status = 'CONFIRMED' AND b.bookedAt >= :since")
    List<Booking> findUpcomingByUser(
            @Param("userId") Integer userId,
            @Param("since") LocalDateTime since);

    @Query("SELECT b FROM Booking b WHERE b.flightId = :flightId " +
            "AND b.status = 'CONFIRMED'")
    List<Booking> findConfirmedByFlight(@Param("flightId") Integer flightId);

    @Query("SELECT SUM(b.totalFare) FROM Booking b WHERE b.flightId = :flightId " +
            "AND b.status = 'CONFIRMED'")
    Double sumRevenueByFlight(@Param("flightId") Integer flightId);

//    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
//            "AND b.checkedIn = false AND b.flightId IN " +
//            "(SELECT f.flightId FROM Flight f WHERE f.departureTime BETWEEN :start AND :end)")
//    List<Booking> findBookingsForCheckinReminder(
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN ('CONFIRMED','COMPLETED')")
    long countTotalBookings();

    @Query("SELECT SUM(b.totalFare) FROM Booking b WHERE b.status IN ('CONFIRMED','COMPLETED')")
    Double sumTotalRevenue();
}