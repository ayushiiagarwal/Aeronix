package com.aeronix.seat_service.service;

import com.aeronix.seat_service.dto.*;
import com.aeronix.seat_service.entity.Seat;

import java.util.List;
import java.util.Optional;

public interface SeatService {
    List<Seat> addSeatsForFlight(BulkSeatRequest request);
    List<Seat> getAvailableSeats(Integer flightId);
    List<Seat> getAvailableByClass(Integer flightId, String seatClass);
    Optional<Seat> getSeatById(Integer seatId);
    Seat holdSeat(Integer seatId, HoldRequest request);
    void releaseSeat(Integer seatId);
    void confirmSeat(Integer seatId);
    Seat updateSeat(Integer seatId, SeatRequest request);
    SeatMapResponse getSeatMap(Integer flightId);
    int countAvailableByClass(Integer flightId, String seatClass);
    void deleteSeatsForFlight(Integer flightId);
    List<Seat> getAllSeatsByFlight(Integer flightId);
    void releaseExpiredHolds();
    void releaseUserHoldsOnFlight(Integer flightId, String userId);
    void blockSeat(Integer seatId);
}