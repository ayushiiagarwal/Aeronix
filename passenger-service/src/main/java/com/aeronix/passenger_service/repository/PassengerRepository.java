package com.aeronix.passenger_service.repository;

import com.aeronix.passenger_service.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Integer> {

    List<Passenger> findByBookingId(String bookingId);

    Optional<Passenger> findByPassportNumber(String passportNumber);

    Optional<Passenger> findByTicketNumber(String ticketNumber);

    Optional<Passenger> findBySeatId(Integer seatId);

    int countByBookingId(String bookingId);

    void deleteByBookingId(String bookingId);

    @Query(value = "SELECT p.* FROM passenger_info p " +
            "WHERE p.booking_id IN (" +
            "  SELECT b.booking_id FROM bookings b " +
            "  WHERE b.flight_id = :flightId AND b.status = 'CONFIRMED'" +
            ")", nativeQuery = true)
    List<Passenger> findByFlightId(@Param("flightId") Integer flightId);

    @Query("SELECT p FROM Passenger p WHERE p.bookingId = :bookingId " +
            "AND p.checkedIn = false")
    List<Passenger> findUncheckedByBooking(@Param("bookingId") String bookingId);

    @Query("SELECT p FROM Passenger p WHERE p.bookingId = :bookingId " +
            "AND p.passengerType = :type")
    List<Passenger> findByBookingIdAndType(
            @Param("bookingId") String bookingId,
            @Param("type") Passenger.PassengerType type);

    boolean existsByPassportNumberAndBookingId(String passportNumber, String bookingId);
}
