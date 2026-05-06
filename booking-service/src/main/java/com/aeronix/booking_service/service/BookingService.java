package com.aeronix.booking_service.service;

import com.aeronix.booking_service.dto.*;
import com.aeronix.booking_service.entity.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingService {
    Booking createBooking(BookingRequest request, Integer userId);
    Optional<Booking> getBookingById(String bookingId);
    Optional<Booking> getBookingByPnr(String pnrCode);
    List<Booking> getBookingsByUser(Integer userId);
    List<Booking> getBookingsByFlight(Integer flightId);
    List<Booking> getBookingsByStatus(String status);
    Booking cancelBooking(String bookingId, CancelRequest request, Integer userId);
    Booking updateStatus(String bookingId, String status);
    FareSummary calculateFare(Integer flightId, String seatClass,
                              int passengerCount, Double extraLuggage);
    Booking addAddOn(String bookingId, AddOnRequest request);
    List<Booking> getUpcomingBookings(Integer userId);
    Booking webCheckIn(CheckInRequest request, Integer userId);
    List<Booking> getAllBookings();
    long countTotalBookings();
    Double sumTotalRevenue();
    Double sumRevenueByFlight(Integer flightId);
    int countConfirmedByFlight(Integer flightId);
    Booking confirmBookingAfterPayment(String bookingId, String paymentId);
}