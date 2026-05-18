package com.aeronix.seat_service.service;

import com.aeronix.seat_service.dto.*;
import com.aeronix.seat_service.entity.Seat;
import com.aeronix.seat_service.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    @Mock private SeatRepository seatRepository;
    @InjectMocks private SeatServiceImpl seatService;

    private Seat availableSeat;
    private Seat heldSeat;
    private Seat confirmedSeat;
    private Seat blockedSeat;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatService, "holdExpiryMinutes", 15);

        availableSeat = Seat.builder()
                .seatId(1).flightId(1).seatNumber("12A")
                .seatClass(Seat.SeatClass.ECONOMY).seatRow(12).seatColumn("A")
                .isWindow(true).isAisle(false).hasExtraLegroom(false)
                .priceMultiplier(1.1).status(Seat.SeatStatus.AVAILABLE).build();

        heldSeat = Seat.builder()
                .seatId(2).flightId(1).seatNumber("12B")
                .seatClass(Seat.SeatClass.ECONOMY).seatRow(12).seatColumn("B")
                .isWindow(false).isAisle(false).hasExtraLegroom(false)
                .priceMultiplier(1.0).status(Seat.SeatStatus.HELD)
                .heldAt(LocalDateTime.now()).heldByUserId("1").build();

        confirmedSeat = Seat.builder()
                .seatId(3).flightId(1).seatNumber("1A")
                .seatClass(Seat.SeatClass.FIRST).seatRow(1).seatColumn("A")
                .isWindow(true).isAisle(false).hasExtraLegroom(true)
                .priceMultiplier(4.0).status(Seat.SeatStatus.CONFIRMED).build();

        blockedSeat = Seat.builder()
                .seatId(4).flightId(1).seatNumber("15C")
                .seatClass(Seat.SeatClass.ECONOMY).seatRow(15).seatColumn("C")
                .isWindow(false).isAisle(true).hasExtraLegroom(false)
                .priceMultiplier(1.05).status(Seat.SeatStatus.BLOCKED).build();
    }

    // ── addSeatsForFlight ─────────────────────────────────────

    @Test
    void addSeatsForFlight_with_explicit_seat_list() {
        SeatRequest sr = new SeatRequest();
        sr.setSeatNumber("12A");
        sr.setSeatClass("ECONOMY");
        sr.setSeatRow(12);
        sr.setSeatColumn("A");
        sr.setWindow(true);
        sr.setAisle(false);
        sr.setHasExtraLegroom(false);

        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(List.of(sr));

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Seat> result = seatService.addSeatsForFlight(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeatNumber()).isEqualTo("12A");
        assertThat(result.get(0).getStatus()).isEqualTo(Seat.SeatStatus.AVAILABLE);
        verify(seatRepository).deleteByFlightId(1);
    }

    @Test
    void addSeatsForFlight_explicit_seat_defaults_price_multiplier_to_1() {
        SeatRequest sr = new SeatRequest();
        sr.setSeatNumber("12A");
        sr.setSeatClass("ECONOMY");
        sr.setSeatRow(12);
        sr.setSeatColumn("A");
        sr.setPriceMultiplier(null);

        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(List.of(sr));

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Seat> result = seatService.addSeatsForFlight(req);

        assertThat(result.get(0).getPriceMultiplier()).isEqualTo(1.0);
    }

    @Test
    void addSeatsForFlight_auto_generate_economy_rows() {
        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(null);
        req.setEconomyRows(5);
        req.setSeatsPerRow(6);

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Seat> result = seatService.addSeatsForFlight(req);

        assertThat(result).hasSize(30); // 5 rows x 6 cols
        assertThat(result).allMatch(s -> s.getSeatClass() == Seat.SeatClass.ECONOMY);
    }

    @Test
    void addSeatsForFlight_auto_generate_all_classes() {
        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(null);
        req.setFirstClassRows(2);
        req.setBusinessRows(3);
        req.setEconomyRows(10);
        req.setSeatsPerRow(6);

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Seat> result = seatService.addSeatsForFlight(req);

        assertThat(result).hasSize(90); // 15 rows x 6 cols
        long firstCount    = result.stream().filter(s -> s.getSeatClass() == Seat.SeatClass.FIRST).count();
        long businessCount = result.stream().filter(s -> s.getSeatClass() == Seat.SeatClass.BUSINESS).count();
        long economyCount  = result.stream().filter(s -> s.getSeatClass() == Seat.SeatClass.ECONOMY).count();
        assertThat(firstCount).isEqualTo(12);
        assertThat(businessCount).isEqualTo(18);
        assertThat(economyCount).isEqualTo(60);
    }

    @Test
    void addSeatsForFlight_auto_window_seats_correctly_flagged() {
        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(null);
        req.setEconomyRows(1);
        req.setSeatsPerRow(6);

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Seat> result = seatService.addSeatsForFlight(req);

        Seat seatA = result.stream().filter(s -> s.getSeatColumn().equals("A")).findFirst().orElseThrow();
        Seat seatB = result.stream().filter(s -> s.getSeatColumn().equals("B")).findFirst().orElseThrow();
        assertThat(seatA.isWindow()).isTrue();
        assertThat(seatB.isWindow()).isFalse();
    }

    @Test
    void addSeatsForFlight_auto_aisle_seats_correctly_flagged() {
        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(null);
        req.setEconomyRows(1);
        req.setSeatsPerRow(6);

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Seat> result = seatService.addSeatsForFlight(req);

        Seat seatC = result.stream().filter(s -> s.getSeatColumn().equals("C")).findFirst().orElseThrow();
        Seat seatD = result.stream().filter(s -> s.getSeatColumn().equals("D")).findFirst().orElseThrow();
        assertThat(seatC.isAisle()).isTrue();
        assertThat(seatD.isAisle()).isTrue();
    }

    @Test
    void addSeatsForFlight_first_class_multiplier_is_4() {
        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(null);
        req.setFirstClassRows(1);
        req.setSeatsPerRow(6);

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Seat> result = seatService.addSeatsForFlight(req);

        assertThat(result).allMatch(s -> s.getPriceMultiplier() == 4.0);
    }

    @Test
    void addSeatsForFlight_business_class_multiplier_is_2_5() {
        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(null);
        req.setBusinessRows(1);
        req.setSeatsPerRow(6);

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Seat> result = seatService.addSeatsForFlight(req);

        assertThat(result).allMatch(s -> s.getPriceMultiplier() == 2.5);
    }

    @Test
    void addSeatsForFlight_row1_has_extra_legroom() {
        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(null);
        req.setEconomyRows(5);
        req.setSeatsPerRow(6);

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Seat> result = seatService.addSeatsForFlight(req);

        List<Seat> row1Seats = result.stream().filter(s -> s.getSeatRow() == 1).toList();
        assertThat(row1Seats).allMatch(Seat::isHasExtraLegroom);
    }

    @Test
    void addSeatsForFlight_deletes_existing_seats_first() {
        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(Collections.emptyList());

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        seatService.addSeatsForFlight(req);

        verify(seatRepository).deleteByFlightId(1);
    }

    @Test
    void addSeatsForFlight_default_seats_per_row_is_6() {
        BulkSeatRequest req = new BulkSeatRequest();
        req.setFlightId(1);
        req.setSeats(null);
        req.setEconomyRows(1);
        req.setSeatsPerRow(null);

        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Seat> result = seatService.addSeatsForFlight(req);

        assertThat(result).hasSize(6);
    }

    // ── getAvailableSeats ─────────────────────────────────────

    @Test
    void getAvailableSeats_returns_list() {
        when(seatRepository.findAvailableByFlightId(1)).thenReturn(List.of(availableSeat));

        List<Seat> result = seatService.getAvailableSeats(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Seat.SeatStatus.AVAILABLE);
    }

    @Test
    void getAvailableSeats_empty_when_none_available() {
        when(seatRepository.findAvailableByFlightId(1)).thenReturn(Collections.emptyList());

        List<Seat> result = seatService.getAvailableSeats(1);

        assertThat(result).isEmpty();
    }

    // ── getAvailableByClass ───────────────────────────────────

    @Test
    void getAvailableByClass_economy() {
        when(seatRepository.findAvailableByFlightIdAndClass(1, Seat.SeatClass.ECONOMY))
                .thenReturn(List.of(availableSeat));

        List<Seat> result = seatService.getAvailableByClass(1, "ECONOMY");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeatClass()).isEqualTo(Seat.SeatClass.ECONOMY);
    }

    @Test
    void getAvailableByClass_business() {
        when(seatRepository.findAvailableByFlightIdAndClass(1, Seat.SeatClass.BUSINESS))
                .thenReturn(Collections.emptyList());

        List<Seat> result = seatService.getAvailableByClass(1, "BUSINESS");

        assertThat(result).isEmpty();
    }

    @Test
    void getAvailableByClass_case_insensitive() {
        when(seatRepository.findAvailableByFlightIdAndClass(1, Seat.SeatClass.ECONOMY))
                .thenReturn(List.of(availableSeat));

        List<Seat> result = seatService.getAvailableByClass(1, "economy");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAvailableByClass_invalid_class_throws() {
        assertThatThrownBy(() -> seatService.getAvailableByClass(1, "PREMIUM"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── getSeatById ───────────────────────────────────────────

    @Test
    void getSeatById_found() {
        when(seatRepository.findById(1)).thenReturn(Optional.of(availableSeat));

        Optional<Seat> result = seatService.getSeatById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getSeatNumber()).isEqualTo("12A");
    }

    @Test
    void getSeatById_not_found() {
        when(seatRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Seat> result = seatService.getSeatById(99);

        assertThat(result).isEmpty();
    }

    // ── holdSeat ──────────────────────────────────────────────

    @Test
    void holdSeat_available_success() {
        when(seatRepository.findById(1)).thenReturn(Optional.of(availableSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HoldRequest req = new HoldRequest();
        req.setUserId("1");

        Seat result = seatService.holdSeat(1, req);

        assertThat(result.getStatus()).isEqualTo(Seat.SeatStatus.HELD);
        assertThat(result.getHeldByUserId()).isEqualTo("1");
        assertThat(result.getHeldAt()).isNotNull();
    }

    @Test
    void holdSeat_already_held_throws() {
        when(seatRepository.findById(2)).thenReturn(Optional.of(heldSeat));

        HoldRequest req = new HoldRequest();
        req.setUserId("2");

        assertThatThrownBy(() -> seatService.holdSeat(2, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void holdSeat_confirmed_throws() {
        when(seatRepository.findById(3)).thenReturn(Optional.of(confirmedSeat));

        HoldRequest req = new HoldRequest();
        req.setUserId("1");

        assertThatThrownBy(() -> seatService.holdSeat(3, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void holdSeat_blocked_throws() {
        when(seatRepository.findById(4)).thenReturn(Optional.of(blockedSeat));

        HoldRequest req = new HoldRequest();
        req.setUserId("1");

        assertThatThrownBy(() -> seatService.holdSeat(4, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void holdSeat_not_found_throws() {
        when(seatRepository.findById(99)).thenReturn(Optional.empty());

        HoldRequest req = new HoldRequest();
        req.setUserId("1");

        assertThatThrownBy(() -> seatService.holdSeat(99, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Seat not found");
    }

    // ── releaseSeat ───────────────────────────────────────────

    @Test
    void releaseSeat_held_seat_becomes_available() {
        when(seatRepository.findById(2)).thenReturn(Optional.of(heldSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        seatService.releaseSeat(2);

        assertThat(heldSeat.getStatus()).isEqualTo(Seat.SeatStatus.AVAILABLE);
        assertThat(heldSeat.getHeldAt()).isNull();
        assertThat(heldSeat.getHeldByUserId()).isNull();
    }

    @Test
    void releaseSeat_clears_held_by_user_id() {
        when(seatRepository.findById(2)).thenReturn(Optional.of(heldSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        seatService.releaseSeat(2);

        assertThat(heldSeat.getHeldByUserId()).isNull();
    }

    @Test
    void releaseSeat_not_found_throws() {
        when(seatRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seatService.releaseSeat(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Seat not found");
    }

    // ── confirmSeat ───────────────────────────────────────────

    @Test
    void confirmSeat_held_becomes_confirmed() {
        when(seatRepository.findById(2)).thenReturn(Optional.of(heldSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        seatService.confirmSeat(2);

        assertThat(heldSeat.getStatus()).isEqualTo(Seat.SeatStatus.CONFIRMED);
    }

    @Test
    void confirmSeat_available_becomes_confirmed() {
        when(seatRepository.findById(1)).thenReturn(Optional.of(availableSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        seatService.confirmSeat(1);

        assertThat(availableSeat.getStatus()).isEqualTo(Seat.SeatStatus.CONFIRMED);
    }

    @Test
    void confirmSeat_blocked_throws() {
        when(seatRepository.findById(4)).thenReturn(Optional.of(blockedSeat));

        assertThatThrownBy(() -> seatService.confirmSeat(4))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot confirm seat with status");
    }

    @Test
    void confirmSeat_already_confirmed_throws() {
        when(seatRepository.findById(3)).thenReturn(Optional.of(confirmedSeat));

        assertThatThrownBy(() -> seatService.confirmSeat(3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot confirm seat with status");
    }

    @Test
    void confirmSeat_not_found_throws() {
        when(seatRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seatService.confirmSeat(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Seat not found");
    }

    // ── updateSeat ────────────────────────────────────────────

    @Test
    void updateSeat_success() {
        when(seatRepository.findById(1)).thenReturn(Optional.of(availableSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SeatRequest req = new SeatRequest();
        req.setSeatClass("BUSINESS");
        req.setWindow(false);
        req.setAisle(true);
        req.setHasExtraLegroom(true);
        req.setPriceMultiplier(2.5);

        Seat result = seatService.updateSeat(1, req);

        assertThat(result.getSeatClass()).isEqualTo(Seat.SeatClass.BUSINESS);
        assertThat(result.isAisle()).isTrue();
        assertThat(result.isHasExtraLegroom()).isTrue();
        assertThat(result.getPriceMultiplier()).isEqualTo(2.5);
    }

    @Test
    void updateSeat_null_price_multiplier_keeps_existing() {
        when(seatRepository.findById(1)).thenReturn(Optional.of(availableSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SeatRequest req = new SeatRequest();
        req.setSeatClass("ECONOMY");
        req.setPriceMultiplier(null);

        Seat result = seatService.updateSeat(1, req);

        assertThat(result.getPriceMultiplier()).isEqualTo(1.1);
    }

    @Test
    void updateSeat_case_insensitive_class() {
        when(seatRepository.findById(1)).thenReturn(Optional.of(availableSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SeatRequest req = new SeatRequest();
        req.setSeatClass("first");
        req.setPriceMultiplier(null);

        Seat result = seatService.updateSeat(1, req);

        assertThat(result.getSeatClass()).isEqualTo(Seat.SeatClass.FIRST);
    }

    @Test
    void updateSeat_not_found_throws() {
        when(seatRepository.findById(99)).thenReturn(Optional.empty());

        SeatRequest req = new SeatRequest();
        req.setSeatClass("ECONOMY");

        assertThatThrownBy(() -> seatService.updateSeat(99, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Seat not found");
    }

    // ── getSeatMap ────────────────────────────────────────────

    @Test
    void getSeatMap_groups_by_class() {
        when(seatRepository.findByFlightId(1))
                .thenReturn(List.of(availableSeat, confirmedSeat));

        SeatMapResponse result = seatService.getSeatMap(1);

        assertThat(result.getFlightId()).isEqualTo(1);
        assertThat(result.getSeatsByClass()).containsKeys("ECONOMY", "FIRST");
        assertThat(result.getTotalSeats()).isEqualTo(2);
    }

    @Test
    void getSeatMap_counts_available_correctly() {
        when(seatRepository.findByFlightId(1))
                .thenReturn(List.of(availableSeat, heldSeat, confirmedSeat, blockedSeat));

        SeatMapResponse result = seatService.getSeatMap(1);

        assertThat(result.getTotalAvailable()).isEqualTo(1);
        assertThat(result.getTotalSeats()).isEqualTo(4);
    }

    @Test
    void getSeatMap_empty_flight_returns_empty_map() {
        when(seatRepository.findByFlightId(99)).thenReturn(Collections.emptyList());

        SeatMapResponse result = seatService.getSeatMap(99);

        assertThat(result.getSeatsByClass()).isEmpty();
        assertThat(result.getTotalSeats()).isEqualTo(0);
        assertThat(result.getTotalAvailable()).isEqualTo(0);
    }

    // ── countAvailableByClass ─────────────────────────────────

    @Test
    void countAvailableByClass_returns_count() {
        when(seatRepository.countAvailableByClass(1, Seat.SeatClass.ECONOMY)).thenReturn(50);

        int count = seatService.countAvailableByClass(1, "ECONOMY");

        assertThat(count).isEqualTo(50);
    }

    @Test
    void countAvailableByClass_zero() {
        when(seatRepository.countAvailableByClass(1, Seat.SeatClass.FIRST)).thenReturn(0);

        int count = seatService.countAvailableByClass(1, "FIRST");

        assertThat(count).isEqualTo(0);
    }

    @Test
    void countAvailableByClass_invalid_class_throws() {
        assertThatThrownBy(() -> seatService.countAvailableByClass(1, "PREMIUM"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── deleteSeatsForFlight ──────────────────────────────────

    @Test
    void deleteSeatsForFlight_calls_repository() {
        doNothing().when(seatRepository).deleteByFlightId(1);

        seatService.deleteSeatsForFlight(1);

        verify(seatRepository).deleteByFlightId(1);
    }

    // ── getAllSeatsByFlight ────────────────────────────────────

    @Test
    void getAllSeatsByFlight_returns_all() {
        when(seatRepository.findByFlightId(1))
                .thenReturn(List.of(availableSeat, heldSeat, confirmedSeat));

        List<Seat> result = seatService.getAllSeatsByFlight(1);

        assertThat(result).hasSize(3);
    }

    @Test
    void getAllSeatsByFlight_empty() {
        when(seatRepository.findByFlightId(99)).thenReturn(Collections.emptyList());

        List<Seat> result = seatService.getAllSeatsByFlight(99);

        assertThat(result).isEmpty();
    }

    // ── releaseExpiredHolds ───────────────────────────────────

    @Test
    void releaseExpiredHolds_calls_repository_with_correct_expiry() {
        when(seatRepository.releaseExpiredHolds(any())).thenReturn(3);

        seatService.releaseExpiredHolds();

        verify(seatRepository).releaseExpiredHolds(any(LocalDateTime.class));
    }

    @Test
    void releaseExpiredHolds_zero_released_no_log() {
        when(seatRepository.releaseExpiredHolds(any())).thenReturn(0);

        assertThatCode(() -> seatService.releaseExpiredHolds()).doesNotThrowAnyException();
    }

    @Test
    void releaseExpiredHolds_multiple_released_no_exception() {
        when(seatRepository.releaseExpiredHolds(any())).thenReturn(10);

        assertThatCode(() -> seatService.releaseExpiredHolds()).doesNotThrowAnyException();
    }

    // ── releaseUserHoldsOnFlight ──────────────────────────────

    @Test
    void releaseUserHoldsOnFlight_releases_all_user_held_seats() {
        when(seatRepository.findHeldByUserOnFlight(1, "1")).thenReturn(List.of(heldSeat));
        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        seatService.releaseUserHoldsOnFlight(1, "1");

        assertThat(heldSeat.getStatus()).isEqualTo(Seat.SeatStatus.AVAILABLE);
        assertThat(heldSeat.getHeldAt()).isNull();
        assertThat(heldSeat.getHeldByUserId()).isNull();
    }

    @Test
    void releaseUserHoldsOnFlight_no_held_seats_does_nothing() {
        when(seatRepository.findHeldByUserOnFlight(1, "99")).thenReturn(Collections.emptyList());

        assertThatCode(() -> seatService.releaseUserHoldsOnFlight(1, "99"))
                .doesNotThrowAnyException();

        verify(seatRepository).saveAll(Collections.emptyList());
    }

    @Test
    void releaseUserHoldsOnFlight_multiple_seats_all_released() {
        Seat heldSeat2 = Seat.builder()
                .seatId(5).flightId(1).seatNumber("12C")
                .seatClass(Seat.SeatClass.ECONOMY).seatRow(12).seatColumn("C")
                .status(Seat.SeatStatus.HELD).heldAt(LocalDateTime.now()).heldByUserId("1")
                .build();

        when(seatRepository.findHeldByUserOnFlight(1, "1")).thenReturn(List.of(heldSeat, heldSeat2));
        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        seatService.releaseUserHoldsOnFlight(1, "1");

        assertThat(heldSeat.getStatus()).isEqualTo(Seat.SeatStatus.AVAILABLE);
        assertThat(heldSeat2.getStatus()).isEqualTo(Seat.SeatStatus.AVAILABLE);
    }

    // ── blockSeat ─────────────────────────────────────────────

    @Test
    void blockSeat_available_becomes_blocked() {
        when(seatRepository.findById(1)).thenReturn(Optional.of(availableSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        seatService.blockSeat(1);

        assertThat(availableSeat.getStatus()).isEqualTo(Seat.SeatStatus.BLOCKED);
    }

    @Test
    void blockSeat_held_becomes_blocked() {
        when(seatRepository.findById(2)).thenReturn(Optional.of(heldSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        seatService.blockSeat(2);

        assertThat(heldSeat.getStatus()).isEqualTo(Seat.SeatStatus.BLOCKED);
    }

    @Test
    void blockSeat_not_found_throws() {
        when(seatRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seatService.blockSeat(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Seat not found");
    }
}