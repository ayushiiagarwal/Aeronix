package com.aeronix.flight_service.service;

import com.aeronix.flight_service.dto.*;
import com.aeronix.flight_service.entity.Flight;
import com.aeronix.flight_service.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;

    @Override
    @Transactional
    public Flight addFlight(FlightRequest req) {
        if (flightRepository.findByFlightNumber(req.getFlightNumber()).isPresent()) {
            throw new RuntimeException("Flight number already exists: " + req.getFlightNumber());
        }

        int duration = req.getDurationMinutes() != null ? req.getDurationMinutes()
                : (int) java.time.Duration.between(req.getDepartureTime(), req.getArrivalTime()).toMinutes();

        Flight flight = Flight.builder()
                .flightNumber(req.getFlightNumber())
                .airlineId(req.getAirlineId())
                .originAirportCode(req.getOriginAirportCode().toUpperCase())
                .destinationAirportCode(req.getDestinationAirportCode().toUpperCase())
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .durationMinutes(duration)
                .aircraftType(req.getAircraftType())
                .totalSeats(req.getTotalSeats())
                .availableSeats(req.getTotalSeats())
                .basePrice(req.getBasePrice())
                .economyPrice(req.getEconomyPrice() != null ? req.getEconomyPrice() : req.getBasePrice())
                .businessPrice(req.getBusinessPrice() != null ? req.getBusinessPrice() : req.getBasePrice() * 2.5)
                .firstClassPrice(req.getFirstClassPrice() != null ? req.getFirstClassPrice() : req.getBasePrice() * 4.0)
                .stops(req.getStops() != null ? req.getStops() : 0)
                .status(Flight.FlightStatus.ON_TIME)
                .build();

        return flightRepository.save(flight);
    }

    @Override
    public Optional<Flight> getFlightById(Integer flightId) {
        return flightRepository.findById(flightId);
    }

    @Override
    public Optional<Flight> getFlightByNumber(String flightNumber) {
        return flightRepository.findByFlightNumber(flightNumber);
    }

    @Override
    public List<Flight> searchFlights(FlightSearchRequest request) {
        LocalDateTime startOfDay = request.getDepartureDate().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        List<Flight> results = flightRepository.findAvailableFlights(
                request.getOrigin().toUpperCase(),
                request.getDestination().toUpperCase(),
                startOfDay,
                startOfNextDay,
                request.getPassengers()
        );

        // Apply filters
        if (request.getMinPrice() != null) {
            results = results.stream()
                    .filter(f -> getPriceByClass(f, request.getSeatClass()) >= request.getMinPrice())
                    .collect(Collectors.toList());
        }
        if (request.getMaxPrice() != null) {
            results = results.stream()
                    .filter(f -> getPriceByClass(f, request.getSeatClass()) <= request.getMaxPrice())
                    .collect(Collectors.toList());
        }
        if (request.getAirlineId() != null) {
            results = results.stream()
                    .filter(f -> f.getAirlineId().equals(request.getAirlineId()))
                    .collect(Collectors.toList());
        }
        if (request.getMaxStops() != null) {
            results = results.stream()
                    .filter(f -> f.getStops() <= request.getMaxStops())
                    .collect(Collectors.toList());
        }

        // Sort
        if ("price".equalsIgnoreCase(request.getSortBy())) {
            results.sort(Comparator.comparingDouble(Flight::getBasePrice));
        } else if ("duration".equalsIgnoreCase(request.getSortBy())) {
            results.sort(Comparator.comparingInt(Flight::getDurationMinutes));
        } else if ("departure".equalsIgnoreCase(request.getSortBy())) {
            results.sort(Comparator.comparing(Flight::getDepartureTime));
        }

        return results;
    }

    @Override
    public RoundTripResponse searchRoundTrip(RoundTripSearchRequest request) {
        FlightSearchRequest outReq = new FlightSearchRequest();
        outReq.setOrigin(request.getOrigin());
        outReq.setDestination(request.getDestination());
        outReq.setDepartureDate(request.getDepartureDate());
        outReq.setPassengers(request.getPassengers());
        outReq.setMinPrice(request.getMinPrice());
        outReq.setMaxPrice(request.getMaxPrice());
        outReq.setAirlineId(request.getAirlineId());
        outReq.setSeatClass(request.getSeatClass());
        outReq.setMaxStops(request.getMaxStops());

        FlightSearchRequest retReq = new FlightSearchRequest();
        retReq.setOrigin(request.getDestination());
        retReq.setDestination(request.getOrigin());
        retReq.setDepartureDate(request.getReturnDate());
        retReq.setPassengers(request.getPassengers());
        retReq.setMinPrice(request.getMinPrice());
        retReq.setMaxPrice(request.getMaxPrice());
        retReq.setAirlineId(request.getAirlineId());
        retReq.setSeatClass(request.getSeatClass());
        retReq.setMaxStops(request.getMaxStops());

        return new RoundTripResponse(searchFlights(outReq), searchFlights(retReq));
    }

    @Override
    @Transactional
    public Flight updateFlight(Integer flightId, FlightRequest req) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + flightId));

        flight.setFlightNumber(req.getFlightNumber());
        flight.setOriginAirportCode(req.getOriginAirportCode().toUpperCase());
        flight.setDestinationAirportCode(req.getDestinationAirportCode().toUpperCase());
        flight.setDepartureTime(req.getDepartureTime());
        flight.setArrivalTime(req.getArrivalTime());
        if (req.getDurationMinutes() != null) flight.setDurationMinutes(req.getDurationMinutes());
        if (req.getAircraftType() != null) flight.setAircraftType(req.getAircraftType());
        if (req.getBasePrice() != null) flight.setBasePrice(req.getBasePrice());
        if (req.getEconomyPrice() != null) flight.setEconomyPrice(req.getEconomyPrice());
        if (req.getBusinessPrice() != null) flight.setBusinessPrice(req.getBusinessPrice());
        if (req.getFirstClassPrice() != null) flight.setFirstClassPrice(req.getFirstClassPrice());
        if (req.getStops() != null) flight.setStops(req.getStops());

        return flightRepository.save(flight);
    }

    @Override
    @Transactional
    public void updateStatus(Integer flightId, StatusUpdateRequest req) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + flightId));
        flight.setStatus(Flight.FlightStatus.valueOf(req.getStatus().toUpperCase()));
        flightRepository.save(flight);
        // Notification trigger happens via booking-service calling notification-service
    }

    @Override
    @Transactional
    public synchronized void decrementSeats(Integer flightId, int count) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + flightId));
        if (flight.getAvailableSeats() < count) {
            throw new RuntimeException("Not enough available seats");
        }
        flight.setAvailableSeats(flight.getAvailableSeats() - count);
        flightRepository.save(flight);
    }

    @Override
    @Transactional
    public synchronized void incrementSeats(Integer flightId, int count) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + flightId));
        flight.setAvailableSeats(Math.min(flight.getAvailableSeats() + count, flight.getTotalSeats()));
        flightRepository.save(flight);
    }

    @Override
    @Transactional
    public void deleteFlight(Integer flightId) {
        flightRepository.deleteById(flightId);
    }

    @Override
    public List<Flight> getFlightsByAirline(Integer airlineId) {
        return flightRepository.findByAirlineId(airlineId);
    }

    @Override
    public List<Flight> getFlightsByStatus(String status) {
        return flightRepository.findByStatus(Flight.FlightStatus.valueOf(status.toUpperCase()));
    }

    @Override
    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    @Override
    public long countByAirline(Integer airlineId) {
        return flightRepository.countByAirlineId(airlineId);
    }

    // ── Helpers ──────────────────────────────────────────────

    private double getPriceByClass(Flight flight, String seatClass) {
        if (seatClass == null) return flight.getBasePrice();
        return switch (seatClass.toUpperCase()) {
            case "BUSINESS" -> flight.getBusinessPrice() != null ? flight.getBusinessPrice() : flight.getBasePrice();
            case "FIRST"    -> flight.getFirstClassPrice() != null ? flight.getFirstClassPrice() : flight.getBasePrice();
            default         -> flight.getEconomyPrice() != null ? flight.getEconomyPrice() : flight.getBasePrice();
        };
    }
}
