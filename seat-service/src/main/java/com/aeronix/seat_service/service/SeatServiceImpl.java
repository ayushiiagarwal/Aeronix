package com.aeronix.seat_service.service;

import com.aeronix.seat_service.dto.*;
import com.aeronix.seat_service.entity.Seat;
import com.aeronix.seat_service.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;

    @Value("${seat.hold.expiry.minutes:15}")
    private int holdExpiryMinutes;

    @Override
    @Transactional
    public List<Seat> addSeatsForFlight(BulkSeatRequest request) {
        // Delete existing seats for the flight before re-adding
        seatRepository.deleteByFlightId(request.getFlightId());

        List<Seat> seats = new ArrayList<>();

        // If explicit seat list provided
        if (request.getSeats() != null && !request.getSeats().isEmpty()) {
            for (SeatRequest sr : request.getSeats()) {
                seats.add(buildSeat(sr, request.getFlightId()));
            }
            return seatRepository.saveAll(seats);
        }

        // Auto-generate from row config
        String[] cols = {"A", "B", "C", "D", "E", "F"};
        int seatsPerRow = request.getSeatsPerRow() != null ? request.getSeatsPerRow() : 6;
        int currentRow = 1;

        // First class
        if (request.getFirstClassRows() != null) {
            for (int r = 0; r < request.getFirstClassRows(); r++, currentRow++) {
                for (int c = 0; c < Math.min(seatsPerRow, cols.length); c++) {
                    seats.add(buildAutoSeat(request.getFlightId(), currentRow,
                            cols[c], Seat.SeatClass.FIRST, seatsPerRow));
                }
            }
        }

        // Business
        if (request.getBusinessRows() != null) {
            for (int r = 0; r < request.getBusinessRows(); r++, currentRow++) {
                for (int c = 0; c < Math.min(seatsPerRow, cols.length); c++) {
                    seats.add(buildAutoSeat(request.getFlightId(), currentRow,
                            cols[c], Seat.SeatClass.BUSINESS, seatsPerRow));
                }
            }
        }

        // Economy
        if (request.getEconomyRows() != null) {
            for (int r = 0; r < request.getEconomyRows(); r++, currentRow++) {
                for (int c = 0; c < Math.min(seatsPerRow, cols.length); c++) {
                    seats.add(buildAutoSeat(request.getFlightId(), currentRow,
                            cols[c], Seat.SeatClass.ECONOMY, seatsPerRow));
                }
            }
        }

        return seatRepository.saveAll(seats);
    }

    @Override
    public List<Seat> getAvailableSeats(Integer flightId) {
        return seatRepository.findAvailableByFlightId(flightId);
    }

    @Override
    public List<Seat> getAvailableByClass(Integer flightId, String seatClass) {
        return seatRepository.findAvailableByFlightIdAndClass(
                flightId, Seat.SeatClass.valueOf(seatClass.toUpperCase()));
    }

    @Override
    public Optional<Seat> getSeatById(Integer seatId) {
        return seatRepository.findById(seatId);
    }

    @Override
    @Transactional
    public Seat holdSeat(Integer seatId, HoldRequest request) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));

        if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
            throw new RuntimeException("Seat " + seat.getSeatNumber() +
                    " is not available. Current status: " + seat.getStatus());
        }

        seat.setStatus(Seat.SeatStatus.HELD);
        seat.setHeldAt(LocalDateTime.now());
        seat.setHeldByUserId(request.getUserId());
        return seatRepository.save(seat);
    }

    @Override
    @Transactional
    public void releaseSeat(Integer seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));
        seat.setStatus(Seat.SeatStatus.AVAILABLE);
        seat.setHeldAt(null);
        seat.setHeldByUserId(null);
        seatRepository.save(seat);
    }

    @Override
    @Transactional
    public void confirmSeat(Integer seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));
        if (seat.getStatus() != Seat.SeatStatus.HELD &&
                seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
            throw new RuntimeException("Cannot confirm seat with status: " + seat.getStatus());
        }
        seat.setStatus(Seat.SeatStatus.CONFIRMED);
        seatRepository.save(seat);
    }

    @Override
    @Transactional
    public Seat updateSeat(Integer seatId, SeatRequest request) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));
        seat.setSeatClass(Seat.SeatClass.valueOf(request.getSeatClass().toUpperCase()));
        seat.setWindow(request.isWindow());
        seat.setAisle(request.isAisle());
        seat.setHasExtraLegroom(request.isHasExtraLegroom());
        if (request.getPriceMultiplier() != null) {
            seat.setPriceMultiplier(request.getPriceMultiplier());
        }
        return seatRepository.save(seat);
    }

    @Override
    public SeatMapResponse getSeatMap(Integer flightId) {
        List<Seat> all = seatRepository.findByFlightId(flightId);
        Map<String, List<Seat>> byClass = all.stream()
                .collect(Collectors.groupingBy(s -> s.getSeatClass().name()));

        int totalAvailable = (int) all.stream()
                .filter(s -> s.getStatus() == Seat.SeatStatus.AVAILABLE)
                .count();

        return new SeatMapResponse(flightId, byClass, totalAvailable, all.size());
    }

    @Override
    public int countAvailableByClass(Integer flightId, String seatClass) {
        return seatRepository.countAvailableByClass(
                flightId, Seat.SeatClass.valueOf(seatClass.toUpperCase()));
    }

    @Override
    @Transactional
    public void deleteSeatsForFlight(Integer flightId) {
        seatRepository.deleteByFlightId(flightId);
    }

    @Override
    public List<Seat> getAllSeatsByFlight(Integer flightId) {
        return seatRepository.findByFlightId(flightId);
    }

    @Override
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime expiry = LocalDateTime.now().minusMinutes(holdExpiryMinutes);
        int released = seatRepository.releaseExpiredHolds(expiry);
        if (released > 0) {
            log.info("Released {} expired seat holds", released);
        }
    }

    @Override
    @Transactional
    public void releaseUserHoldsOnFlight(Integer flightId, String userId) {
        List<Seat> held = seatRepository.findHeldByUserOnFlight(flightId, userId);
        held.forEach(s -> {
            s.setStatus(Seat.SeatStatus.AVAILABLE);
            s.setHeldAt(null);
            s.setHeldByUserId(null);
        });
        seatRepository.saveAll(held);
    }

    @Override
    @Transactional
    public void blockSeat(Integer seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));
        seat.setStatus(Seat.SeatStatus.BLOCKED);
        seatRepository.save(seat);
    }

    // ── Helpers ──────────────────────────────────────────────

    private Seat buildSeat(SeatRequest sr, Integer flightId) {
        return Seat.builder()
                .flightId(flightId)
                .seatNumber(sr.getSeatNumber())
                .seatClass(Seat.SeatClass.valueOf(sr.getSeatClass().toUpperCase()))
                .seatRow(sr.getSeatRow())
                .seatColumn(sr.getSeatColumn())
                .isWindow(sr.isWindow())
                .isAisle(sr.isAisle())
                .hasExtraLegroom(sr.isHasExtraLegroom())
                .priceMultiplier(sr.getPriceMultiplier() != null ? sr.getPriceMultiplier() : 1.0)
                .status(Seat.SeatStatus.AVAILABLE)
                .build();
    }

    private Seat buildAutoSeat(Integer flightId, int row, String col,
                               Seat.SeatClass seatClass, int seatsPerRow) {
        String seatNumber = row + col;
        boolean isWindow = col.equals("A") || col.equals(String.valueOf((char)('A' + seatsPerRow - 1)));
        boolean isAisle  = col.equals("C") || col.equals("D");
        boolean extraLeg = row == 1 || row % 10 == 0;

        double multiplier = switch (seatClass) {
            case FIRST    -> 4.0;
            case BUSINESS -> 2.5;
            default       -> isWindow ? 1.1 : (isAisle ? 1.05 : 1.0);
        };

        return Seat.builder()
                .flightId(flightId)
                .seatNumber(seatNumber)
                .seatClass(seatClass)
                .seatRow(row)
                .seatColumn(col)
                .isWindow(isWindow)
                .isAisle(isAisle)
                .hasExtraLegroom(extraLeg)
                .priceMultiplier(multiplier)
                .status(Seat.SeatStatus.AVAILABLE)
                .build();
    }
}