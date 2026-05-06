package com.aeronix.airline_service.repository;

import com.aeronix.airline_service.entity.Airline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, Integer> {

    Optional<Airline> findByIataCode(String iataCode);

    Optional<Airline> findByIcaoCode(String icaoCode);

    List<Airline> findByIsActive(boolean isActive);

    List<Airline> findByCountry(String country);

    @Query("SELECT a FROM Airline a WHERE " +
            "LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.iataCode) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Airline> searchAirlines(@Param("query") String query);

    boolean existsByIataCode(String iataCode);
}