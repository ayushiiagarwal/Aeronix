package com.aeronix.passenger_service.service;

import com.aeronix.passenger_service.client.BookingClient;
import com.aeronix.passenger_service.client.SeatClient;
import com.aeronix.passenger_service.dto.*;
import com.aeronix.passenger_service.entity.Passenger;
import com.aeronix.passenger_service.repository.PassengerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerServiceImplTest {

    @Mock private PassengerRepository passengerRepository;
    @Mock private SeatClient seatClient;
    @Mock private BookingClient bookingClient;

    @InjectMocks private PassengerServiceImpl passengerService;

    private Passenger samplePassenger;
    private PassengerRequest passengerRequest;

    @BeforeEach
    void setUp() {
        samplePassenger = Passenger.builder()
                .passengerId(1).bookingId("booking-uuid-001")
                .title("Mr").firstName("John").lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("MALE").passportNumber("P1234567")
                .nationality("Indian")
                .passportExpiry(LocalDate.now().plusYears(3))
                .passengerType(Passenger.PassengerType.ADULT)
                .mealPreference(Passenger.MealPreference.VEG)
                .checkedIn(false).build();

        passengerRequest = new PassengerRequest();
        passengerRequest.setBookingId("booking-uuid-001");
        passengerRequest.setFirstName("John");
        passengerRequest.setLastName("Doe");
        passengerRequest.setDateOfBirth(LocalDate.of(1990, 5, 15));
        passengerRequest.setPassportNumber("P1234567");
        passengerRequest.setPassportExpiry(LocalDate.now().plusYears(3));
        passengerRequest.setPassengerType("ADULT");
    }

    // ── addPassenger ──────────────────────────────────────────

    @Test
    void addPassenger_success() {
        when(passengerRepository.existsByPassportNumberAndBookingId(any(), any())).thenReturn(false);
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Passenger result = passengerService.addPassenger(passengerRequest);

        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getPassengerType()).isEqualTo(Passenger.PassengerType.ADULT);
        assertThat(result.getTicketNumber()).startsWith("SKB");
        assertThat(result.isCheckedIn()).isFalse();
    }

    @Test
    void addPassenger_missing_first_name_throws() {
        passengerRequest.setFirstName(null);

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("validation failed")
                .hasMessageContaining("First name is required");
    }

    @Test
    void addPassenger_blank_first_name_throws() {
        passengerRequest.setFirstName("  ");

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("First name is required");
    }

    @Test
    void addPassenger_missing_last_name_throws() {
        passengerRequest.setLastName(null);

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Last name is required");
    }

    @Test
    void addPassenger_missing_dob_throws() {
        passengerRequest.setDateOfBirth(null);

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Date of birth is required");
    }

    @Test
    void addPassenger_future_dob_throws() {
        passengerRequest.setDateOfBirth(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot be in the future");
    }

    @Test
    void addPassenger_adult_under_12_throws() {
        passengerRequest.setDateOfBirth(LocalDate.now().minusYears(5));
        passengerRequest.setPassengerType("ADULT");

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Adult passenger must be 12 years or older");
    }

    @Test
    void addPassenger_child_over_12_throws() {
        passengerRequest.setDateOfBirth(LocalDate.now().minusYears(13));
        passengerRequest.setPassengerType("CHILD");

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Child passenger must be between 2 and 11 years old");
    }

    @Test
    void addPassenger_child_under_2_throws() {
        passengerRequest.setDateOfBirth(LocalDate.now().minusMonths(6));
        passengerRequest.setPassengerType("CHILD");

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Child passenger must be between 2 and 11 years old");
    }

    @Test
    void addPassenger_infant_over_2_throws() {
        passengerRequest.setDateOfBirth(LocalDate.now().minusYears(3));
        passengerRequest.setPassengerType("INFANT");

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Infant passenger must be under 2 years old");
    }

    @Test
    void addPassenger_passport_expiry_less_than_6_months_throws() {
        passengerRequest.setPassportExpiry(LocalDate.now().plusMonths(3));

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("valid for at least 6 months");
    }

    @Test
    void addPassenger_duplicate_passport_on_same_booking_throws() {
        when(passengerRepository.existsByPassportNumberAndBookingId("P1234567", "booking-uuid-001"))
                .thenReturn(true);

        assertThatThrownBy(() -> passengerService.addPassenger(passengerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already added to this booking");
    }

    @Test
    void addPassenger_null_passport_skips_duplicate_check() {
        passengerRequest.setPassportNumber(null);
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Passenger result = passengerService.addPassenger(passengerRequest);

        assertThat(result).isNotNull();
        verify(passengerRepository, never()).existsByPassportNumberAndBookingId(any(), any());
    }

    @Test
    void addPassenger_auto_detects_adult_type_from_dob() {
        passengerRequest.setPassengerType(null);
        passengerRequest.setDateOfBirth(LocalDate.now().minusYears(25));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Passenger result = passengerService.addPassenger(passengerRequest);

        assertThat(result.getPassengerType()).isEqualTo(Passenger.PassengerType.ADULT);
    }

    @Test
    void addPassenger_auto_detects_child_type_from_dob() {
        passengerRequest.setPassengerType(null);
        passengerRequest.setDateOfBirth(LocalDate.now().minusYears(7));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Passenger result = passengerService.addPassenger(passengerRequest);

        assertThat(result.getPassengerType()).isEqualTo(Passenger.PassengerType.CHILD);
    }

    @Test
    void addPassenger_auto_detects_infant_type_from_dob() {
        passengerRequest.setPassengerType(null);
        passengerRequest.setDateOfBirth(LocalDate.now().minusMonths(6));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Passenger result = passengerService.addPassenger(passengerRequest);

        assertThat(result.getPassengerType()).isEqualTo(Passenger.PassengerType.INFANT);
    }

    @Test
    void addPassenger_meal_preference_set_correctly() {
        passengerRequest.setMealPreference("VEG");
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Passenger result = passengerService.addPassenger(passengerRequest);

        assertThat(result.getMealPreference()).isEqualTo(Passenger.MealPreference.VEG);
    }

    @Test
    void addPassenger_invalid_meal_preference_defaults_to_none() {
        passengerRequest.setMealPreference("KOSHER");
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Passenger result = passengerService.addPassenger(passengerRequest);

        assertThat(result.getMealPreference()).isEqualTo(Passenger.MealPreference.NONE);
    }

    @Test
    void addPassenger_generates_ticket_number_starting_with_skb() {
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Passenger result = passengerService.addPassenger(passengerRequest);

        assertThat(result.getTicketNumber()).startsWith("SKB");
    }

    // ── addPassengers (bulk) ──────────────────────────────────

    @Test
    void addPassengers_bulk_saves_all() {
        PassengerRequest pr2 = new PassengerRequest();
        pr2.setFirstName("Jane");
        pr2.setLastName("Doe");
        pr2.setDateOfBirth(LocalDate.of(1992, 3, 10));
        pr2.setPassportExpiry(LocalDate.now().plusYears(3));
        pr2.setPassengerType("ADULT");

        BulkPassengerRequest req = new BulkPassengerRequest();
        req.setBookingId("booking-uuid-001");
        req.setPassengers(List.of(passengerRequest, pr2));

        when(passengerRepository.existsByPassportNumberAndBookingId(any(), any()))
                .thenReturn(false);
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Passenger> result = passengerService.addPassengers(req);

        assertThat(result).hasSize(2);
        verify(passengerRepository, times(2)).save(any());
    }

    @Test
    void addPassengers_sets_booking_id_on_each() {
        BulkPassengerRequest req = new BulkPassengerRequest();
        req.setBookingId("booking-uuid-001");
        req.setPassengers(List.of(passengerRequest));

        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Passenger> result = passengerService.addPassengers(req);

        assertThat(result.get(0).getBookingId()).isEqualTo("booking-uuid-001");
    }

    // ── getPassengerById ──────────────────────────────────────

    @Test
    void getPassengerById_found() {
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));

        Optional<Passenger> result = passengerService.getPassengerById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void getPassengerById_not_found() {
        when(passengerRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Passenger> result = passengerService.getPassengerById(99);

        assertThat(result).isEmpty();
    }

    // ── getPassengersByBooking ────────────────────────────────

    @Test
    void getPassengersByBooking_returns_list() {
        when(passengerRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(List.of(samplePassenger));

        List<Passenger> result = passengerService.getPassengersByBooking("booking-uuid-001");

        assertThat(result).hasSize(1);
    }

    @Test
    void getPassengersByBooking_empty() {
        when(passengerRepository.findByBookingId("nonexistent"))
                .thenReturn(Collections.emptyList());

        List<Passenger> result = passengerService.getPassengersByBooking("nonexistent");

        assertThat(result).isEmpty();
    }

    // ── getByPassportNumber ───────────────────────────────────

    @Test
    void getByPassportNumber_found() {
        when(passengerRepository.findByPassportNumber("P1234567"))
                .thenReturn(Optional.of(samplePassenger));

        Optional<Passenger> result = passengerService.getByPassportNumber("P1234567");

        assertThat(result).isPresent();
    }

    @Test
    void getByPassportNumber_not_found() {
        when(passengerRepository.findByPassportNumber("ZZZZZZ"))
                .thenReturn(Optional.empty());

        Optional<Passenger> result = passengerService.getByPassportNumber("ZZZZZZ");

        assertThat(result).isEmpty();
    }

    // ── getByTicketNumber ─────────────────────────────────────

    @Test
    void getByTicketNumber_found() {
        samplePassenger.setTicketNumber("SKB1234567890");
        when(passengerRepository.findByTicketNumber("SKB1234567890"))
                .thenReturn(Optional.of(samplePassenger));

        Optional<Passenger> result = passengerService.getByTicketNumber("SKB1234567890");

        assertThat(result).isPresent();
    }

    // ── updatePassenger ───────────────────────────────────────

    @Test
    void updatePassenger_success() {
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePassengerRequest req = new UpdatePassengerRequest();
        req.setFirstName("Johnny");
        req.setLastName("Doe Updated");
        req.setMealPreference("NON_VEG");

        Passenger result = passengerService.updatePassenger(1, req);

        assertThat(result.getFirstName()).isEqualTo("Johnny");
        assertThat(result.getLastName()).isEqualTo("Doe Updated");
        assertThat(result.getMealPreference()).isEqualTo(Passenger.MealPreference.NON_VEG);
    }

    @Test
    void updatePassenger_partial_update_preserves_existing() {
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePassengerRequest req = new UpdatePassengerRequest();
        req.setFirstName("Johnny");

        Passenger result = passengerService.updatePassenger(1, req);

        assertThat(result.getFirstName()).isEqualTo("Johnny");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getNationality()).isEqualTo("Indian");
    }

    @Test
    void updatePassenger_invalid_meal_preference_ignored() {
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePassengerRequest req = new UpdatePassengerRequest();
        req.setMealPreference("KOSHER");

        Passenger result = passengerService.updatePassenger(1, req);

        assertThat(result.getMealPreference()).isEqualTo(Passenger.MealPreference.VEG);
    }

    @Test
    void updatePassenger_not_found_throws() {
        when(passengerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerService.updatePassenger(99, new UpdatePassengerRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Passenger not found");
    }

    // ── assignSeat ────────────────────────────────────────────

    @Test
    void assignSeat_no_previous_seat_confirms_directly() {
        samplePassenger.setSeatId(null);
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SeatAssignRequest req = new SeatAssignRequest();
        req.setSeatId(101);
        req.setSeatNumber("12A");

        Passenger result = passengerService.assignSeat(1, req);

        assertThat(result.getSeatId()).isEqualTo(101);
        assertThat(result.getSeatNumber()).isEqualTo("12A");
        verify(seatClient, never()).releaseSeat(any());
        verify(seatClient).confirmSeat(101);
    }

    @Test
    void assignSeat_different_seat_releases_old_and_confirms_new() {
        samplePassenger.setSeatId(50);
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SeatAssignRequest req = new SeatAssignRequest();
        req.setSeatId(101);
        req.setSeatNumber("12A");

        passengerService.assignSeat(1, req);

        verify(seatClient).releaseSeat(50);
        verify(seatClient).confirmSeat(101);
    }

    @Test
    void assignSeat_same_seat_no_release() {
        samplePassenger.setSeatId(101);
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SeatAssignRequest req = new SeatAssignRequest();
        req.setSeatId(101);
        req.setSeatNumber("12A");

        passengerService.assignSeat(1, req);

        verify(seatClient, never()).releaseSeat(any());
        verify(seatClient).confirmSeat(101);
    }

    @Test
    void assignSeat_not_found_throws() {
        when(passengerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerService.assignSeat(99, new SeatAssignRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Passenger not found");
    }

    // ── deletePassenger ───────────────────────────────────────

    @Test
    void deletePassenger_with_seat_releases_it() {
        samplePassenger.setSeatId(101);
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));
        doNothing().when(seatClient).releaseSeat(101);
        doNothing().when(passengerRepository).deleteById(1);

        passengerService.deletePassenger(1);

        verify(seatClient).releaseSeat(101);
        verify(passengerRepository).deleteById(1);
    }

    @Test
    void deletePassenger_without_seat_no_release() {
        samplePassenger.setSeatId(null);
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));
        doNothing().when(passengerRepository).deleteById(1);

        passengerService.deletePassenger(1);

        verify(seatClient, never()).releaseSeat(any());
        verify(passengerRepository).deleteById(1);
    }

    @Test
    void deletePassenger_not_found_throws() {
        when(passengerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerService.deletePassenger(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Passenger not found");
    }

    // ── deletePassengersByBooking ─────────────────────────────

    @Test
    void deletePassengersByBooking_releases_all_seats() {
        samplePassenger.setSeatId(101);
        Passenger p2 = Passenger.builder().passengerId(2)
                .bookingId("booking-uuid-001").seatId(102).build();

        when(passengerRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(List.of(samplePassenger, p2));

        passengerService.deletePassengersByBooking("booking-uuid-001");

        verify(seatClient).releaseSeat(101);
        verify(seatClient).releaseSeat(102);
        verify(passengerRepository).deleteByBookingId("booking-uuid-001");
    }

    @Test
    void deletePassengersByBooking_passengers_without_seats_not_released() {
        samplePassenger.setSeatId(null);
        when(passengerRepository.findByBookingId("booking-uuid-001"))
                .thenReturn(List.of(samplePassenger));

        passengerService.deletePassengersByBooking("booking-uuid-001");

        verify(seatClient, never()).releaseSeat(any());
        verify(passengerRepository).deleteByBookingId("booking-uuid-001");
    }

    // ── validatePassengerData ─────────────────────────────────

    @Test
    void validatePassengerData_valid_returns_valid() {
        ValidationResult result = passengerService.validatePassengerData(passengerRequest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void validatePassengerData_multiple_errors_all_reported() {
        PassengerRequest invalid = new PassengerRequest();
        invalid.setFirstName(null);
        invalid.setLastName(null);
        invalid.setDateOfBirth(null);

        ValidationResult result = passengerService.validatePassengerData(invalid);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(3);
    }

    @Test
    void validatePassengerData_null_passport_expiry_valid() {
        passengerRequest.setPassportExpiry(null);

        ValidationResult result = passengerService.validatePassengerData(passengerRequest);

        assertThat(result.isValid()).isTrue();
    }

    // ── getPassengerCount ─────────────────────────────────────

    @Test
    void getPassengerCount_returns_count() {
        when(passengerRepository.countByBookingId("booking-uuid-001")).thenReturn(2);

        int count = passengerService.getPassengerCount("booking-uuid-001");

        assertThat(count).isEqualTo(2);
    }

    @Test
    void getPassengerCount_zero_for_unknown_booking() {
        when(passengerRepository.countByBookingId("nonexistent")).thenReturn(0);

        int count = passengerService.getPassengerCount("nonexistent");

        assertThat(count).isEqualTo(0);
    }

    // ── getPassengersByFlight ─────────────────────────────────

    @Test
    void getPassengersByFlight_returns_list() {
        when(passengerRepository.findByFlightId(1)).thenReturn(List.of(samplePassenger));

        List<Passenger> result = passengerService.getPassengersByFlight(1);

        assertThat(result).hasSize(1);
    }

    // ── checkInPassenger ──────────────────────────────────────

    @Test
    void checkInPassenger_success() {
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Passenger result = passengerService.checkInPassenger(1);

        assertThat(result.isCheckedIn()).isTrue();
        assertThat(result.getCheckedInAt()).isNotNull();
    }

    @Test
    void checkInPassenger_already_checked_in_throws() {
        samplePassenger.setCheckedIn(true);
        when(passengerRepository.findById(1)).thenReturn(Optional.of(samplePassenger));

        assertThatThrownBy(() -> passengerService.checkInPassenger(1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already checked in");
    }

    @Test
    void checkInPassenger_not_found_throws() {
        when(passengerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerService.checkInPassenger(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Passenger not found");
    }

    // ── getFlightManifest ─────────────────────────────────────

    @Test
    void getFlightManifest_builds_entries() {
        samplePassenger.setSeatNumber("12A");
        samplePassenger.setTitle("Mr");
        Map<String, Object> booking = Map.of(
                "pnrCode", "ABC123", "seatClass", "ECONOMY");

        when(passengerRepository.findByFlightId(1)).thenReturn(List.of(samplePassenger));
        when(bookingClient.getBookingById("booking-uuid-001")).thenReturn(booking);

        List<ManifestEntry> result = passengerService.getFlightManifest(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPnrCode()).isEqualTo("ABC123");
        assertThat(result.get(0).getSeatClass()).isEqualTo("ECONOMY");
        assertThat(result.get(0).getFullName()).contains("John").contains("Doe");
        assertThat(result.get(0).getSeatNumber()).isEqualTo("12A");
    }

    @Test
    void getFlightManifest_null_booking_uses_na() {
        when(passengerRepository.findByFlightId(1)).thenReturn(List.of(samplePassenger));
        when(bookingClient.getBookingById("booking-uuid-001")).thenReturn(null);

        List<ManifestEntry> result = passengerService.getFlightManifest(1);

        assertThat(result.get(0).getPnrCode()).isEqualTo("N/A");
        assertThat(result.get(0).getSeatClass()).isEqualTo("N/A");
    }

    @Test
    void getFlightManifest_empty_flight_returns_empty_list() {
        when(passengerRepository.findByFlightId(99)).thenReturn(Collections.emptyList());

        List<ManifestEntry> result = passengerService.getFlightManifest(99);

        assertThat(result).isEmpty();
    }
}