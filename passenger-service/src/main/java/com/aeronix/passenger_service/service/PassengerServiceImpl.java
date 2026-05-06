package com.aeronix.passenger_service.service;

import com.aeronix.passenger_service.client.*;
import com.aeronix.passenger_service.dto.*;
import com.aeronix.passenger_service.entity.Passenger;
import com.aeronix.passenger_service.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;
    private final SeatClient seatClient;
    private final BookingClient bookingClient;

    @Override
    @Transactional
    public Passenger addPassenger(PassengerRequest request) {
        // Validate first
        ValidationResult validation = validatePassengerData(request);
        if (!validation.isValid()) {
            throw new RuntimeException("Passenger validation failed: " +
                    String.join(", ", validation.getErrors()));
        }

        // Prevent duplicate passport on same booking
        if (request.getPassportNumber() != null &&
            passengerRepository.existsByPassportNumberAndBookingId(
                request.getPassportNumber(), request.getBookingId())) {
            throw new RuntimeException("Passenger with passport " +
                    request.getPassportNumber() + " already added to this booking");
        }

        Passenger passenger = buildPassenger(request);
        passenger.setTicketNumber(generateTicketNumber());
        return passengerRepository.save(passenger);
    }

    @Override
    @Transactional
    public List<Passenger> addPassengers(BulkPassengerRequest request) {
        List<Passenger> saved = new ArrayList<>();
        for (PassengerRequest pr : request.getPassengers()) {
            pr.setBookingId(request.getBookingId());
            saved.add(addPassenger(pr));
        }
        return saved;
    }

    @Override
    public Optional<Passenger> getPassengerById(Integer passengerId) {
        return passengerRepository.findById(passengerId);
    }

    @Override
    public List<Passenger> getPassengersByBooking(String bookingId) {
        return passengerRepository.findByBookingId(bookingId);
    }

    @Override
    public Optional<Passenger> getByPassportNumber(String passportNumber) {
        return passengerRepository.findByPassportNumber(passportNumber);
    }

    @Override
    public Optional<Passenger> getByTicketNumber(String ticketNumber) {
        return passengerRepository.findByTicketNumber(ticketNumber);
    }

    @Override
    @Transactional
    public Passenger updatePassenger(Integer passengerId, UpdatePassengerRequest request) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found: " + passengerId));

        if (request.getTitle() != null)
            passenger.setTitle(request.getTitle());
        if (request.getFirstName() != null)
            passenger.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            passenger.setLastName(request.getLastName());
        if (request.getDateOfBirth() != null)
            passenger.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)
            passenger.setGender(request.getGender());
        if (request.getPassportNumber() != null)
            passenger.setPassportNumber(request.getPassportNumber());
        if (request.getNationality() != null)
            passenger.setNationality(request.getNationality());
        if (request.getPassportExpiry() != null)
            passenger.setPassportExpiry(request.getPassportExpiry());
        if (request.getMealPreference() != null) {
            try {
                passenger.setMealPreference(
                    Passenger.MealPreference.valueOf(
                        request.getMealPreference().toUpperCase()));
            } catch (Exception ignored) {}
        }

        return passengerRepository.save(passenger);
    }

    @Override
    @Transactional
    public Passenger assignSeat(Integer passengerId, SeatAssignRequest request) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found: " + passengerId));

        // Release old seat if re-assigning
        if (passenger.getSeatId() != null && !passenger.getSeatId().equals(request.getSeatId())) {
            seatClient.releaseSeat(passenger.getSeatId());
        }

        passenger.setSeatId(request.getSeatId());
        passenger.setSeatNumber(request.getSeatNumber());

        // Confirm seat in seat-service
        seatClient.confirmSeat(request.getSeatId());

        return passengerRepository.save(passenger);
    }

    @Override
    @Transactional
    public void deletePassenger(Integer passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found: " + passengerId));

        // Release seat if assigned
        if (passenger.getSeatId() != null) {
            seatClient.releaseSeat(passenger.getSeatId());
        }

        passengerRepository.deleteById(passengerId);
    }

    @Override
    @Transactional
    public void deletePassengersByBooking(String bookingId) {
        List<Passenger> passengers = passengerRepository.findByBookingId(bookingId);
        // Release all seats
        passengers.stream()
                .filter(p -> p.getSeatId() != null)
                .forEach(p -> seatClient.releaseSeat(p.getSeatId()));
        passengerRepository.deleteByBookingId(bookingId);
    }

    @Override
    public ValidationResult validatePassengerData(PassengerRequest request) {
        List<String> errors = new ArrayList<>();

        // Name validation
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            errors.add("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            errors.add("Last name is required");
        }

        // Date of birth validation
        if (request.getDateOfBirth() != null) {
            if (request.getDateOfBirth().isAfter(LocalDate.now())) {
                errors.add("Date of birth cannot be in the future");
            }

            // Age-based type validation
            int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
            String type = request.getPassengerType();
            if (type != null) {
                if (type.equalsIgnoreCase("ADULT") && age < 12) {
                    errors.add("Adult passenger must be 12 years or older");
                }
                if (type.equalsIgnoreCase("CHILD") && (age < 2 || age >= 12)) {
                    errors.add("Child passenger must be between 2 and 11 years old");
                }
                if (type.equalsIgnoreCase("INFANT") && age >= 2) {
                    errors.add("Infant passenger must be under 2 years old");
                }
            }
        } else {
            errors.add("Date of birth is required");
        }

        // Passport expiry validation
        if (request.getPassportExpiry() != null) {
            if (request.getPassportExpiry().isBefore(LocalDate.now().plusMonths(6))) {
                errors.add("Passport must be valid for at least 6 months from today");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    @Override
    public int getPassengerCount(String bookingId) {
        return passengerRepository.countByBookingId(bookingId);
    }

    @Override
    public List<Passenger> getPassengersByFlight(Integer flightId) {
        return passengerRepository.findByFlightId(flightId);
    }

    @Override
    public List<ManifestEntry> getFlightManifest(Integer flightId) {
        List<Passenger> passengers = passengerRepository.findByFlightId(flightId);

        return passengers.stream().map(p -> {
            // Fetch booking for PNR
            Map<String, Object> booking = bookingClient.getBookingById(p.getBookingId());
            String pnr = booking != null ? (String) booking.get("pnrCode") : "N/A";
            String seatClass = booking != null ? (String) booking.get("seatClass") : "N/A";

            return ManifestEntry.builder()
                    .passengerId(p.getPassengerId())
                    .bookingId(p.getBookingId())
                    .pnrCode(pnr)
                    .fullName(p.getTitle() + " " + p.getFirstName() + " " + p.getLastName())
                    .seatNumber(p.getSeatNumber())
                    .seatClass(seatClass)
                    .mealPreference(p.getMealPreference().name())
                    .passengerType(p.getPassengerType().name())
                    .checkedIn(p.isCheckedIn())
                    .passportNumber(p.getPassportNumber())
                    .nationality(p.getNationality())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Passenger checkInPassenger(Integer passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found: " + passengerId));

        if (passenger.isCheckedIn()) {
            throw new RuntimeException("Passenger already checked in");
        }

        passenger.setCheckedIn(true);
        passenger.setCheckedInAt(LocalDateTime.now());
        return passengerRepository.save(passenger);
    }

    // ── Helpers ──────────────────────────────────────────────

    private Passenger buildPassenger(PassengerRequest request) {
        Passenger.PassengerType type = Passenger.PassengerType.ADULT;
        if (request.getPassengerType() != null) {
            try {
                type = Passenger.PassengerType.valueOf(
                        request.getPassengerType().toUpperCase());
            } catch (Exception ignored) {}
        }

        // Auto-detect passenger type from age if not provided
        if (request.getDateOfBirth() != null &&
            request.getPassengerType() == null) {
            int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
            if (age < 2) type = Passenger.PassengerType.INFANT;
            else if (age < 12) type = Passenger.PassengerType.CHILD;
            else type = Passenger.PassengerType.ADULT;
        }

        Passenger.MealPreference meal = Passenger.MealPreference.NONE;
        if (request.getMealPreference() != null) {
            try {
                meal = Passenger.MealPreference.valueOf(
                        request.getMealPreference().toUpperCase());
            } catch (Exception ignored) {}
        }

        return Passenger.builder()
                .bookingId(request.getBookingId())
                .title(request.getTitle())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .passportNumber(request.getPassportNumber())
                .nationality(request.getNationality())
                .passportExpiry(request.getPassportExpiry())
                .passengerType(type)
                .mealPreference(meal)
                .seatId(request.getSeatId())
                .seatNumber(request.getSeatNumber())
                .checkedIn(false)
                .build();
    }

    private String generateTicketNumber() {
        String prefix = "SKB";
        String digits = String.valueOf(System.currentTimeMillis()).substring(5);
        String suffix = String.valueOf((int)(Math.random() * 900) + 100);
        return prefix + digits + suffix;
    }
}