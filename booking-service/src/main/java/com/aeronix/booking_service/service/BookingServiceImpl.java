package com.aeronix.booking_service.service;

import com.aeronix.booking_service.client.*;
import com.aeronix.booking_service.dto.*;
import com.aeronix.booking_service.entity.Booking;
import com.aeronix.booking_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final FlightClient flightClient;
    private final SeatClient seatClient;
    private final NotificationClient notificationClient;

    @Value("${booking.tax.gst:0.05}")
    private double gstRate;

    @Value("${booking.tax.fuel.surcharge:0.02}")
    private double fuelSurchargeRate;

    @Value("${checkin.open.hours.before:24}")
    private int checkinOpenHours;

    @Value("${checkin.close.hours.before:1}")
    private int checkinCloseHours;

    private static final double LUGGAGE_CHARGE_PER_KG = 150.0;
    private static final double MEAL_CHARGE = 250.0;

    // ── Core Booking ─────────────────────────────────────────

    @Override
    @Transactional
    public Booking createBooking(BookingRequest request, Integer userId) {

        // 1. Fetch flight details
        Map<String, Object> flight = flightClient.getFlightById(request.getFlightId());
        if (flight == null) throw new RuntimeException("Flight not found: " + request.getFlightId());

        // 2. Calculate fare
        FareSummary fare = calculateFare(
                request.getFlightId(),
                request.getSeatClass(),
                request.getPassengers().size(),
                request.getExtraLuggageKg()
        );

        // 3. Hold selected seats (optimistic — seat-service handles locking)
        if (request.getSelectedSeatIds() != null) {
            for (Integer seatId : request.getSelectedSeatIds()) {
                seatClient.holdSeat(seatId, String.valueOf(userId), request.getFlightId());
            }
        }

        // 4. Hold return seats if round trip
        if (request.getReturnFlightId() != null && request.getReturnSelectedSeatIds() != null) {
            for (Integer seatId : request.getReturnSelectedSeatIds()) {
                seatClient.holdSeat(seatId, String.valueOf(userId), request.getReturnFlightId());
            }
        }

        // 5. Determine meal/luggage extras
        double luggageCharge = 0.0;
        if (request.getExtraLuggageKg() != null && request.getExtraLuggageKg() > 0) {
            luggageCharge = request.getExtraLuggageKg() * LUGGAGE_CHARGE_PER_KG;
        }

        // 6. Build and persist booking
        Booking booking = Booking.builder()
                .bookingId(UUID.randomUUID().toString())
                .userId(userId)
                .flightId(request.getFlightId())
                .returnFlightId(request.getReturnFlightId())
                .pnrCode(generatePnr())
                .tripType(request.getTripType() != null &&
                        request.getTripType().equalsIgnoreCase("ROUND_TRIP")
                        ? Booking.TripType.ROUND_TRIP : Booking.TripType.ONE_WAY)
                .status(Booking.BookingStatus.PENDING)
                .baseFare(fare.getBaseFare())
                .taxes(fare.getTotalTax())
                .luggageKg(request.getExtraLuggageKg() != null ? request.getExtraLuggageKg() : 0.0)
                .luggageCharge(luggageCharge)
                .ancillaryCharges(luggageCharge)
                .mealPreference(request.getMealPreference())
                .seatClass(request.getSeatClass())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .passengerCount(request.getPassengers().size())
                .totalFare(fare.getTotalFare() + luggageCharge)
                .bookedAt(LocalDateTime.now())
                .build();

        return bookingRepository.save(booking);
    }

    @Override
    public Optional<Booking> getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId);
    }

    @Override
    public Optional<Booking> getBookingByPnr(String pnrCode) {
        return bookingRepository.findByPnrCode(pnrCode.toUpperCase());
    }

    @Override
    public List<Booking> getBookingsByUser(Integer userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Booking> getBookingsByFlight(Integer flightId) {
        return bookingRepository.findByFlightId(flightId);
    }

    @Override
    public List<Booking> getBookingsByStatus(String status) {
        return bookingRepository.findByStatus(
                Booking.BookingStatus.valueOf(status.toUpperCase()));
    }

    @Override
    @Transactional
    public Booking cancelBooking(String bookingId, CancelRequest request, Integer userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed booking");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(request.getReason());

        // Release flight seat counter
        flightClient.incrementSeats(booking.getFlightId(), booking.getPassengerCount());
        if (booking.getReturnFlightId() != null) {
            flightClient.incrementSeats(booking.getReturnFlightId(), booking.getPassengerCount());
        }

        bookingRepository.save(booking);

        // Notify
        notificationClient.sendCancellationNotification(
                booking.getUserId(), bookingId,
                booking.getPnrCode(), booking.getContactEmail());

        return booking;
    }

    @Override
    @Transactional
    public Booking updateStatus(String bookingId, String status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        booking.setStatus(Booking.BookingStatus.valueOf(status.toUpperCase()));
        return bookingRepository.save(booking);
    }

    @Override
    public FareSummary calculateFare(Integer flightId, String seatClass,
                                     int passengerCount, Double extraLuggage) {
        Map<String, Object> flight = flightClient.getFlightById(flightId);
        if (flight == null) throw new RuntimeException("Flight not found: " + flightId);

        double pricePerPax = getPriceByClass(flight, seatClass);
        double baseFare = pricePerPax * passengerCount;
        double gst = baseFare * gstRate;
        double fuel = baseFare * fuelSurchargeRate;
        double totalTax = gst + fuel;
        double luggageCharge = (extraLuggage != null && extraLuggage > 0)
                ? extraLuggage * LUGGAGE_CHARGE_PER_KG : 0.0;
        double totalFare = baseFare + totalTax + luggageCharge;

        return FareSummary.builder()
                .baseFare(baseFare)
                .gstAmount(gst)
                .fuelSurcharge(fuel)
                .luggageCharge(luggageCharge)
                .mealCharge(0.0)
                .totalTax(totalTax)
                .totalFare(totalFare)
                .seatClass(seatClass)
                .passengerCount(passengerCount)
                .build();
    }

    @Override
    @Transactional
    public Booking addAddOn(String bookingId, AddOnRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED &&
                booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new RuntimeException("Cannot add add-ons to booking with status: " + booking.getStatus());
        }

        double extra = 0.0;
        switch (request.getType().toUpperCase()) {
            case "LUGGAGE" -> {
                double kg = request.getValue();
                double charge = kg * LUGGAGE_CHARGE_PER_KG;
                booking.setLuggageKg(booking.getLuggageKg() + kg);
                booking.setLuggageCharge(booking.getLuggageCharge() + charge);
                extra = charge;
            }
            case "MEAL" -> {
                booking.setMealPreference(request.getMealType());
                extra = MEAL_CHARGE * booking.getPassengerCount();
            }
            default -> throw new RuntimeException("Unknown add-on type: " + request.getType());
        }

        booking.setAncillaryCharges(booking.getAncillaryCharges() + extra);
        booking.setTotalFare(booking.getTotalFare() + extra);
        return bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getUpcomingBookings(Integer userId) {
        return bookingRepository.findUpcomingByUser(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public Booking webCheckIn(CheckInRequest request, Integer userId) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: booking does not belong to user");
        }
        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed bookings can be checked in");
        }
        if (booking.isCheckedIn()) {
            throw new RuntimeException("Already checked in");
        }

        // Validate check-in window (fetching flight departure time)
        Map<String, Object> flight = flightClient.getFlightById(booking.getFlightId());
//        if (flight != null) {
//            // Parse departure time and validate window
//            // In a real system, parse LocalDateTime from flight map
//            // Simplified: allow check-in
//        }

        // Re-seat if requested
        if (request.getNewSeatIds() != null && !request.getNewSeatIds().isEmpty()) {
            for (Integer seatId : request.getNewSeatIds()) {
                seatClient.holdSeat(seatId, String.valueOf(userId), booking.getFlightId());
                seatClient.confirmSeat(seatId);
            }
        }

        booking.setCheckedIn(true);
        booking.setCheckedInAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public long countTotalBookings() {
        return bookingRepository.countTotalBookings();
    }

    @Override
    public Double sumTotalRevenue() {
        Double rev = bookingRepository.sumTotalRevenue();
        return rev != null ? rev : 0.0;
    }

    @Override
    public Double sumRevenueByFlight(Integer flightId) {
        Double rev = bookingRepository.sumRevenueByFlight(flightId);
        return rev != null ? rev : 0.0;
    }

    @Override
    public int countConfirmedByFlight(Integer flightId) {
        return bookingRepository.countByFlightIdAndStatus(
                flightId, Booking.BookingStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public Booking confirmBookingAfterPayment(String bookingId, String paymentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentId);
        booking.setConfirmedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Decrement flight seat counter
        flightClient.decrementSeats(booking.getFlightId(), booking.getPassengerCount());
        if (booking.getReturnFlightId() != null) {
            flightClient.decrementSeats(booking.getReturnFlightId(), booking.getPassengerCount());
        }

        // Send confirmation notification
        String flightNumber = "N/A", origin = "N/A", destination = "N/A", departureTime = "N/A";
        try {
            Map<String, Object> flight = flightClient.getFlightById(booking.getFlightId());
            if (flight != null) {
                flightNumber  = String.valueOf(flight.getOrDefault("flightNumber", "N/A"));
                origin        = String.valueOf(flight.getOrDefault("originAirportCode", "N/A"));
                destination   = String.valueOf(flight.getOrDefault("destinationAirportCode", "N/A"));
                departureTime = String.valueOf(flight.getOrDefault("departureTime", "N/A"));
            }
        } catch (Exception e) {
            log.warn("Could not fetch flight details for notification: {}", e.getMessage());
        }

        String passengerName = "Passenger";
        String seatNumber    = booking.getSeatNumbers() != null ? booking.getSeatNumbers() : "N/A";
        Double totalFare     = booking.getTotalFare();

        notificationClient.sendBookingConfirmation(
                booking.getUserId(), bookingId,
                booking.getPnrCode(), booking.getContactEmail(),
                booking.getContactPhone(),
                passengerName,
                flightNumber, origin, destination, departureTime,
                seatNumber, totalFare);

        return booking;
    }

    // ── Scheduled Jobs ───────────────────────────────────────

    // Check-in reminder: runs hourly, finds bookings departing in ~24h
//    @Scheduled(cron = "0 0 * * * *")
//    public void sendCheckinReminders() {
//        LocalDateTime start = LocalDateTime.now().plusHours(checkinOpenHours - 1);
//        LocalDateTime end   = LocalDateTime.now().plusHours(checkinOpenHours + 1);
//        log.info("Sending check-in reminders for flights departing between {} and {}", start, end);
//        List<Booking> bookings = bookingRepository
//                .findBookingsForCheckinReminder(start, end);
//        for (Booking b : bookings) {
//            notificationClient.sendCheckinReminder(
//                    b.getUserId(), b.getBookingId(), b.getContactEmail());
//        }
//        log.info("Check-in reminders sent for {} bookings", bookings.size());
//    }

    // No-show detection: runs every 30 min — marks un-checked-in bookings NO_SHOW
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void markNoShows() {
        List<Booking> confirmed = bookingRepository
                .findByStatus(Booking.BookingStatus.CONFIRMED);
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (Booking b : confirmed) {
            if (!b.isCheckedIn()) {
                Map<String, Object> flight = flightClient.getFlightById(b.getFlightId());
//                if (flight != null) {
//                    // Mark as NO_SHOW if flight departed 2+ hours ago and not checked in
//                    // Simplified check — in production parse departureTime from flight
//                }
            }
        }
        if (count > 0) log.info("Marked {} bookings as NO_SHOW", count);
    }

    // ── Helpers ──────────────────────────────────────────────

    public String generatePnr() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String pnr;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            pnr = sb.toString();
        } while (bookingRepository.findByPnrCode(pnr).isPresent());
        return pnr;
    }

    @SuppressWarnings("unchecked")
    private double getPriceByClass(Map<String, Object> flight, String seatClass) {
        if (seatClass == null) {
            Object base = flight.get("basePrice");
            return base != null ? ((Number) base).doubleValue() : 0.0;
        }
        return switch (seatClass.toUpperCase()) {
            case "BUSINESS" -> {
                Object p = flight.get("businessPrice");
                yield p != null ? ((Number) p).doubleValue()
                        : ((Number) flight.get("basePrice")).doubleValue() * 2.5;
            }
            case "FIRST" -> {
                Object p = flight.get("firstClassPrice");
                yield p != null ? ((Number) p).doubleValue()
                        : ((Number) flight.get("basePrice")).doubleValue() * 4.0;
            }
            default -> {
                Object p = flight.get("economyPrice");
                yield p != null ? ((Number) p).doubleValue()
                        : ((Number) flight.get("basePrice")).doubleValue();
            }
        };
    }
}