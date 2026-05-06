package com.aeronix.passenger_service.service;

import com.aeronix.passenger_service.dto.*;
import com.aeronix.passenger_service.entity.Passenger;

import java.util.List;
import java.util.Optional;

public interface PassengerService {
    Passenger addPassenger(PassengerRequest request);
    List<Passenger> addPassengers(BulkPassengerRequest request);
    Optional<Passenger> getPassengerById(Integer passengerId);
    List<Passenger> getPassengersByBooking(String bookingId);
    Optional<Passenger> getByPassportNumber(String passportNumber);
    Optional<Passenger> getByTicketNumber(String ticketNumber);
    Passenger updatePassenger(Integer passengerId, UpdatePassengerRequest request);
    Passenger assignSeat(Integer passengerId, SeatAssignRequest request);
    void deletePassenger(Integer passengerId);
    void deletePassengersByBooking(String bookingId);
    ValidationResult validatePassengerData(PassengerRequest request);
    int getPassengerCount(String bookingId);
    List<Passenger> getPassengersByFlight(Integer flightId);
    List<ManifestEntry> getFlightManifest(Integer flightId);
    Passenger checkInPassenger(Integer passengerId);
}