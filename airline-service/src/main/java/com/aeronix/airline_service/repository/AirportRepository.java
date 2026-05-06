package com.aeronix.airline_service.repository;

import com.aeronix.airline_service.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Integer> {

    Optional<Airport> findByIataCode(String iataCode);

    Optional<Airport> findByIcaoCode(String icaoCode);

    List<Airport> findByCity(String city);

    List<Airport> findByCountry(String country);

    List<Airport> findByIsActive(boolean isActive);

    @Query("SELECT a FROM Airport a WHERE " +
            "LOWER(a.name)     LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.iataCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.city)     LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.country)  LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Airport> searchAirports(@Param("query") String query);

    @Query("SELECT a FROM Airport a WHERE " +
            "LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')) " +
            "AND a.isActive = true")
    List<Airport> findByCityContaining(@Param("city") String city);

    boolean existsByIataCode(String iataCode);

    @Query("SELECT a FROM Airport a WHERE a.country = :country AND a.isActive = true")
    List<Airport> findActiveByCountry(@Param("country") String country);
}