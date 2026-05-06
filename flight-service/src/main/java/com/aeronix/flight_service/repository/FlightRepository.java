package com.aeronix.flight_service.repository;

import com.aeronix.flight_service.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Integer> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    /**
     * Finds available flights for a given route and date.
     * Uses >= start-of-day and < start-of-next-day to match the departure date
     * without CAST, which is not portable JPQL.
     */
    @Query("SELECT f FROM Flight f WHERE f.originAirportCode = :origin " +
            "AND f.destinationAirportCode = :dest " +
            "AND f.departureTime >= :startOfDay " +
            "AND f.departureTime < :startOfNextDay " +
            "AND f.availableSeats >= :passengers " +
            "AND f.status != 'CANCELLED'")
    List<Flight> findAvailableFlights(
            @Param("origin") String origin,
            @Param("dest") String dest,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay,
            @Param("passengers") int passengers);

    List<Flight> findByAirlineId(Integer airlineId);

    List<Flight> findByStatus(Flight.FlightStatus status);

    @Query("SELECT f FROM Flight f WHERE f.originAirportCode = :origin " +
            "AND f.destinationAirportCode = :dest " +
            "AND f.status != 'CANCELLED'")
    List<Flight> findByOriginAndDest(
            @Param("origin") String origin,
            @Param("dest") String dest);

    long countByAirlineId(Integer airlineId);

    @Query("SELECT f FROM Flight f WHERE f.departureTime BETWEEN :start AND :end " +
            "AND f.status = 'ON_TIME'")
    List<Flight> findFlightsDepartingBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT f FROM Flight f WHERE f.basePrice BETWEEN :minPrice AND :maxPrice " +
            "AND f.originAirportCode = :origin AND f.destinationAirportCode = :dest " +
            "AND f.status != 'CANCELLED'")
    List<Flight> findByPriceRange(
            @Param("origin") String origin,
            @Param("dest") String dest,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice);
}
