package com.aeronix.booking_service.service;

import com.aeronix.booking_service.client.FlightClient;
import com.aeronix.booking_service.client.NotificationClient;
import com.aeronix.booking_service.client.SeatClient;
import com.aeronix.booking_service.dto.*;
import com.aeronix.booking_service.entity.Booking;
import com.aeronix.booking_service.repository.BookingRepository;
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
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private FlightClient flightClient;
    @Mock private SeatClient seatClient;
    @Mock private NotificationClient notificationClient;

    @InjectMocks private BookingServiceImpl bookingService;

    private Map<String, Object> mockFlight;
    private BookingRequest bookingRequest;
    private Booking pendingBooking;
    private Booking confirmedBooking;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "gstRate", 0.05);
        ReflectionTestUtils.setField(bookingService, "fuelSurchargeRate", 0.02);
        ReflectionTestUtils.setField(bookingService, "checkinOpenHours", 24);
        ReflectionTestUtils.setField(bookingService, "checkinCloseHours", 1);

        mockFlight = new HashMap<>();
        mockFlight.put("flightId", 1);
        mockFlight.put("flightNumber", "AI101");
        mockFlight.put("originAirportCode", "DEL");
        mockFlight.put("destinationAirportCode", "BOM");
        mockFlight.put("departureTime", "2026-06-01T10:00:00");
        mockFlight.put("basePrice", 5000.0);
        mockFlight.put("economyPrice", 5000.0);
        mockFlight.put("businessPrice", 12500.0);
        mockFlight.put("firstClassPrice", 20000.0);

        PassengerRequest passenger = new PassengerRequest();
        passenger.setFirstName("John");
        passenger.setLastName("Doe");

        bookingRequest = new BookingRequest();
        bookingRequest.setFlightId(1);
        bookingRequest.setSeatClass("ECONOMY");
        bookingRequest.setPassengers(List.of(passenger));
        bookingRequest.setContactEmail("john@example.com");
        bookingRequest.setContactPhone("9999999999");
        bookingRequest.setTripType("ONE_WAY");

        pendingBooking = Booking.builder()
                .bookingId("booking-uuid-001")
                .userId(1)
                .flightId(1)
                .pnrCode("ABC123")
                .status(Booking.BookingStatus.PENDING)
                .totalFare(5350.0)
                .baseFare(5000.0)
                .taxes(350.0)
                .ancillaryCharges(0.0)
                .luggageKg(0.0)
                .luggageCharge(0.0)
                .passengerCount(1)
                .contactEmail("john@example.com")
                .contactPhone("9999999999")
                .seatClass("ECONOMY")
                .tripType(Booking.TripType.ONE_WAY)
                .bookedAt(LocalDateTime.now())
                .build();

        confirmedBooking = Booking.builder()
                .bookingId("booking-uuid-001")
                .userId(1)
                .flightId(1)
                .pnrCode("ABC123")
                .status(Booking.BookingStatus.CONFIRMED)
                .totalFare(5350.0)
                .baseFare(5000.0)
                .taxes(350.0)
                .ancillaryCharges(0.0)
                .luggageKg(0.0)
                .luggageCharge(0.0)
                .passengerCount(1)
                .contactEmail("john@example.com")
                .contactPhone("9999999999")
                .seatClass("ECONOMY")
                .tripType(Booking.TripType.ONE_WAY)
                .bookedAt(LocalDateTime.now())
                .checkedIn(false)
                .build();
    }

    // ── createBooking ─────────────────────────────────────────

    @Test
    void createBooking_economy_success() {
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createBooking(bookingRequest, 1);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
        assertThat(result.getSeatClass()).isEqualTo("ECONOMY");
        assertThat(result.getPassengerCount()).isEqualTo(1);
        assertThat(result.getContactEmail()).isEqualTo("john@example.com");
        assertThat(result.getTripType()).isEqualTo(Booking.TripType.ONE_WAY);
        verify(bookingRepository).save(any());
    }

    @Test
    void createBooking_business_class_calculates_correct_fare() {
        bookingRequest.setSeatClass("BUSINESS");
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createBooking(bookingRequest, 1);

        assertThat(result.getBaseFare()).isEqualTo(12500.0);
    }

    @Test
    void createBooking_first_class_calculates_correct_fare() {
        bookingRequest.setSeatClass("FIRST");
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createBooking(bookingRequest, 1);

        assertThat(result.getBaseFare()).isEqualTo(20000.0);
    }

    @Test
    void createBooking_multiple_passengers_correct_fare() {
        PassengerRequest p2 = new PassengerRequest();
        p2.setFirstName("Jane");
        p2.setLastName("Doe");
        bookingRequest.setPassengers(List.of(new PassengerRequest(), p2));

        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createBooking(bookingRequest, 1);

        assertThat(result.getPassengerCount()).isEqualTo(2);
        assertThat(result.getBaseFare()).isEqualTo(10000.0);
    }

    @Test
    void createBooking_with_extra_luggage_adds_charge() {
        bookingRequest.setExtraLuggageKg(10.0);
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createBooking(bookingRequest, 1);

        assertThat(result.getLuggageKg()).isEqualTo(10.0);
        assertThat(result.getLuggageCharge()).isEqualTo(1500.0); // 10 * 150
        assertThat(result.getTotalFare()).isEqualTo(result.getBaseFare() + result.getTaxes() + 1500.0);
    }

    @Test
    void createBooking_with_zero_luggage_no_charge() {
        bookingRequest.setExtraLuggageKg(0.0);
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createBooking(bookingRequest, 1);

        assertThat(result.getLuggageCharge()).isEqualTo(0.0);
    }

    @Test
    void createBooking_with_meal_preference_stored() {
        bookingRequest.setMealPreference("VEG");
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createBooking(bookingRequest, 1);

        assertThat(result.getMealPreference()).isEqualTo("VEG");
    }

    @Test
    void createBooking_round_trip_sets_trip_type() {
        bookingRequest.setTripType("ROUND_TRIP");
        bookingRequest.setReturnFlightId(2);
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createBooking(bookingRequest, 1);

        assertThat(result.getTripType()).isEqualTo(Booking.TripType.ROUND_TRIP);
        assertThat(result.getReturnFlightId()).isEqualTo(2);
    }

    @Test
    void createBooking_with_selected_seats_calls_hold() {
        bookingRequest.setSelectedSeatIds(List.of(101, 102));
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        bookingService.createBooking(bookingRequest, 1);

        verify(seatClient).holdSeat(101, "1", 1);
        verify(seatClient).holdSeat(102, "1", 1);
    }

    @Test
    void createBooking_round_trip_holds_return_seats() {
        bookingRequest.setTripType("ROUND_TRIP");
        bookingRequest.setReturnFlightId(2);
        bookingRequest.setReturnSelectedSeatIds(List.of(201));
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        bookingService.createBooking(bookingRequest, 1);

        verify(seatClient).holdSeat(201, "1", 2);
    }

    @Test
    void createBooking_flight_not_found_throws() {
        when(flightClient.getFlightById(99)).thenReturn(null);
        bookingRequest.setFlightId(99);

        assertThatThrownBy(() -> bookingService.createBooking(bookingRequest, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flight not found");
    }

    @Test
    void createBooking_generates_unique_pnr() {
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createBooking(bookingRequest, 1);

        assertThat(result.getPnrCode()).isNotNull().hasSize(6);
    }

    @Test
    void createBooking_pnr_collision_retries_until_unique() {
        Booking existingBooking = Booking.builder().pnrCode("TAKEN1").build();
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);
        when(bookingRepository.findByPnrCode(anyString()))
                .thenReturn(Optional.of(existingBooking))
                .thenReturn(Optional.of(existingBooking))
                .thenReturn(Optional.empty());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createBooking(bookingRequest, 1);

        assertThat(result.getPnrCode()).isNotNull();
        verify(bookingRepository, atLeast(3)).findByPnrCode(anyString());
    }

    // ── getBookingById ────────────────────────────────────────

    @Test
    void getBookingById_found() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(pendingBooking));

        Optional<Booking> result = bookingService.getBookingById("booking-uuid-001");

        assertThat(result).isPresent();
        assertThat(result.get().getBookingId()).isEqualTo("booking-uuid-001");
    }

    @Test
    void getBookingById_not_found_returns_empty() {
        when(bookingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<Booking> result = bookingService.getBookingById("nonexistent");

        assertThat(result).isEmpty();
    }

    // ── getBookingByPnr ───────────────────────────────────────

    @Test
    void getBookingByPnr_found_uppercase() {
        when(bookingRepository.findByPnrCode("ABC123")).thenReturn(Optional.of(pendingBooking));

        Optional<Booking> result = bookingService.getBookingByPnr("abc123");

        assertThat(result).isPresent();
        verify(bookingRepository).findByPnrCode("ABC123");
    }

    @Test
    void getBookingByPnr_not_found() {
        when(bookingRepository.findByPnrCode("ZZZ999")).thenReturn(Optional.empty());

        Optional<Booking> result = bookingService.getBookingByPnr("ZZZ999");

        assertThat(result).isEmpty();
    }

    // ── getBookingsByUser ─────────────────────────────────────

    @Test
    void getBookingsByUser_returns_list() {
        when(bookingRepository.findByUserId(1)).thenReturn(List.of(pendingBooking));

        List<Booking> result = bookingService.getBookingsByUser(1);

        assertThat(result).hasSize(1);
    }

    @Test
    void getBookingsByUser_no_bookings_returns_empty() {
        when(bookingRepository.findByUserId(99)).thenReturn(Collections.emptyList());

        List<Booking> result = bookingService.getBookingsByUser(99);

        assertThat(result).isEmpty();
    }

    // ── getBookingsByFlight ───────────────────────────────────

    @Test
    void getBookingsByFlight_returns_list() {
        when(bookingRepository.findByFlightId(1)).thenReturn(List.of(pendingBooking, confirmedBooking));

        List<Booking> result = bookingService.getBookingsByFlight(1);

        assertThat(result).hasSize(2);
    }

    // ── getBookingsByStatus ───────────────────────────────────

    @Test
    void getBookingsByStatus_confirmed() {
        when(bookingRepository.findByStatus(Booking.BookingStatus.CONFIRMED))
                .thenReturn(List.of(confirmedBooking));

        List<Booking> result = bookingService.getBookingsByStatus("CONFIRMED");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
    }

    @Test
    void getBookingsByStatus_invalid_status_throws() {
        assertThatThrownBy(() -> bookingService.getBookingsByStatus("INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── cancelBooking ─────────────────────────────────────────

    @Test
    void cancelBooking_pending_success() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CancelRequest req = new CancelRequest();
        req.setReason("Changed plans");

        Booking result = bookingService.cancelBooking("booking-uuid-001", req, 1);

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.CANCELLED);
        assertThat(result.getCancellationReason()).isEqualTo("Changed plans");
        assertThat(result.getCancelledAt()).isNotNull();
    }

    @Test
    void cancelBooking_confirmed_success_and_releases_seats() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CancelRequest req = new CancelRequest();
        req.setReason("Illness");

        bookingService.cancelBooking("booking-uuid-001", req, 1);

        verify(flightClient).incrementSeats(1, 1);
    }

    @Test
    void cancelBooking_already_cancelled_throws() {
        pendingBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(pendingBooking));

        CancelRequest req = new CancelRequest();
        assertThatThrownBy(() -> bookingService.cancelBooking("booking-uuid-001", req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancelBooking_completed_throws() {
        pendingBooking.setStatus(Booking.BookingStatus.COMPLETED);
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(pendingBooking));

        CancelRequest req = new CancelRequest();
        assertThatThrownBy(() -> bookingService.cancelBooking("booking-uuid-001", req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot cancel a completed");
    }

    @Test
    void cancelBooking_not_found_throws() {
        when(bookingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        CancelRequest req = new CancelRequest();
        assertThatThrownBy(() -> bookingService.cancelBooking("nonexistent", req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void cancelBooking_round_trip_releases_both_flights() {
        confirmedBooking.setReturnFlightId(2);
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CancelRequest req = new CancelRequest();
        req.setReason("Trip cancelled");
        bookingService.cancelBooking("booking-uuid-001", req, 1);

        verify(flightClient).incrementSeats(1, 1);
        verify(flightClient).incrementSeats(2, 1);
    }

    @Test
    void cancelBooking_sends_notification() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CancelRequest req = new CancelRequest();
        req.setReason("Test");
        bookingService.cancelBooking("booking-uuid-001", req, 1);

        verify(notificationClient).sendCancellationNotification(
                eq(1), eq("booking-uuid-001"), eq("ABC123"), eq("john@example.com"));
    }

    // ── confirmBookingAfterPayment ────────────────────────────

    @Test
    void confirmBookingAfterPayment_success() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        Booking result = bookingService.confirmBookingAfterPayment("booking-uuid-001", "pay-001");

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
        assertThat(result.getPaymentId()).isEqualTo("pay-001");
        assertThat(result.getConfirmedAt()).isNotNull();
    }

    @Test
    void confirmBookingAfterPayment_decrements_seats() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        bookingService.confirmBookingAfterPayment("booking-uuid-001", "pay-001");

        verify(flightClient).decrementSeats(1, 1);
    }

    @Test
    void confirmBookingAfterPayment_round_trip_decrements_both() {
        pendingBooking.setReturnFlightId(2);
        pendingBooking.setTripType(Booking.TripType.ROUND_TRIP);
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        bookingService.confirmBookingAfterPayment("booking-uuid-001", "pay-001");

        verify(flightClient).decrementSeats(1, 1);
        verify(flightClient).decrementSeats(2, 1);
    }

    @Test
    void confirmBookingAfterPayment_sends_confirmation_notification() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        bookingService.confirmBookingAfterPayment("booking-uuid-001", "pay-001");

        verify(notificationClient).sendBookingConfirmation(
                eq(1), eq("booking-uuid-001"), eq("ABC123"),
                eq("john@example.com"), eq("9999999999"),
                anyString(), eq("AI101"), eq("DEL"), eq("BOM"),
                anyString(), anyString(), eq(5350.0));
    }

    @Test
    void confirmBookingAfterPayment_not_found_throws() {
        when(bookingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmBookingAfterPayment("nonexistent", "pay-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void confirmBookingAfterPayment_flight_fetch_fails_gracefully() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(flightClient.getFlightById(1)).thenThrow(new RuntimeException("flight-service down"));

        assertThatCode(() -> bookingService.confirmBookingAfterPayment("booking-uuid-001", "pay-001"))
                .doesNotThrowAnyException();
    }

    // ── calculateFare ─────────────────────────────────────────

    @Test
    void calculateFare_economy_1_passenger() {
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        FareSummary fare = bookingService.calculateFare(1, "ECONOMY", 1, null);

        assertThat(fare.getBaseFare()).isEqualTo(5000.0);
        assertThat(fare.getGstAmount()).isEqualTo(250.0);       // 5% of 5000
        assertThat(fare.getFuelSurcharge()).isEqualTo(100.0);   // 2% of 5000
        assertThat(fare.getTotalTax()).isEqualTo(350.0);
        assertThat(fare.getTotalFare()).isEqualTo(5350.0);
        assertThat(fare.getLuggageCharge()).isEqualTo(0.0);
    }

    @Test
    void calculateFare_business_2_passengers() {
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        FareSummary fare = bookingService.calculateFare(1, "BUSINESS", 2, null);

        assertThat(fare.getBaseFare()).isEqualTo(25000.0);
        assertThat(fare.getPassengerCount()).isEqualTo(2);
    }

    @Test
    void calculateFare_with_luggage() {
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        FareSummary fare = bookingService.calculateFare(1, "ECONOMY", 1, 5.0);

        assertThat(fare.getLuggageCharge()).isEqualTo(750.0); // 5 * 150
        assertThat(fare.getTotalFare()).isEqualTo(5350.0 + 750.0);
    }

    @Test
    void calculateFare_flight_not_found_throws() {
        when(flightClient.getFlightById(99)).thenReturn(null);

        assertThatThrownBy(() -> bookingService.calculateFare(99, "ECONOMY", 1, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flight not found");
    }

    @Test
    void calculateFare_null_seat_class_uses_base_price() {
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        FareSummary fare = bookingService.calculateFare(1, null, 1, null);

        assertThat(fare.getBaseFare()).isEqualTo(5000.0);
    }

    @Test
    void calculateFare_first_class() {
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        FareSummary fare = bookingService.calculateFare(1, "FIRST", 1, null);

        assertThat(fare.getBaseFare()).isEqualTo(20000.0);
    }

    // ── addAddOn ──────────────────────────────────────────────

    @Test
    void addAddOn_luggage_to_confirmed_booking() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AddOnRequest req = new AddOnRequest();
        req.setType("LUGGAGE");
        req.setValue(10.0);

        Booking result = bookingService.addAddOn("booking-uuid-001", req);

        assertThat(result.getLuggageKg()).isEqualTo(10.0);
        assertThat(result.getLuggageCharge()).isEqualTo(1500.0);
        assertThat(result.getTotalFare()).isEqualTo(confirmedBooking.getTotalFare() + 1500.0);
    }

    @Test
    void addAddOn_luggage_to_pending_booking() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AddOnRequest req = new AddOnRequest();
        req.setType("LUGGAGE");
        req.setValue(5.0);

        Booking result = bookingService.addAddOn("booking-uuid-001", req);

        assertThat(result.getLuggageCharge()).isEqualTo(750.0);
    }

    @Test
    void addAddOn_meal_to_confirmed_booking() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AddOnRequest req = new AddOnRequest();
        req.setType("MEAL");
        req.setMealType("VEG");

        Booking result = bookingService.addAddOn("booking-uuid-001", req);

        assertThat(result.getMealPreference()).isEqualTo("VEG");
        assertThat(result.getTotalFare()).isEqualTo(confirmedBooking.getTotalFare() + 250.0); // MEAL_CHARGE * 1 passenger
    }

    @Test
    void addAddOn_cancelled_booking_throws() {
        confirmedBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));

        AddOnRequest req = new AddOnRequest();
        req.setType("LUGGAGE");
        req.setValue(5.0);

        assertThatThrownBy(() -> bookingService.addAddOn("booking-uuid-001", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot add add-ons");
    }

    @Test
    void addAddOn_completed_booking_throws() {
        confirmedBooking.setStatus(Booking.BookingStatus.COMPLETED);
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));

        AddOnRequest req = new AddOnRequest();
        req.setType("LUGGAGE");
        req.setValue(5.0);

        assertThatThrownBy(() -> bookingService.addAddOn("booking-uuid-001", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot add add-ons");
    }

    @Test
    void addAddOn_unknown_type_throws() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));

        AddOnRequest req = new AddOnRequest();
        req.setType("INSURANCE");
        req.setValue(1.0);

        assertThatThrownBy(() -> bookingService.addAddOn("booking-uuid-001", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unknown add-on type");
    }

    @Test
    void addAddOn_booking_not_found_throws() {
        when(bookingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        AddOnRequest req = new AddOnRequest();
        req.setType("LUGGAGE");
        req.setValue(5.0);

        assertThatThrownBy(() -> bookingService.addAddOn("nonexistent", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found");
    }

    // ── webCheckIn ────────────────────────────────────────────

    @Test
    void webCheckIn_success() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        CheckInRequest req = new CheckInRequest();
        req.setBookingId("booking-uuid-001");

        Booking result = bookingService.webCheckIn(req, 1);

        assertThat(result.isCheckedIn()).isTrue();
        assertThat(result.getCheckedInAt()).isNotNull();
    }

    @Test
    void webCheckIn_wrong_user_throws() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));

        CheckInRequest req = new CheckInRequest();
        req.setBookingId("booking-uuid-001");

        assertThatThrownBy(() -> bookingService.webCheckIn(req, 99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void webCheckIn_not_confirmed_throws() {
        confirmedBooking.setStatus(Booking.BookingStatus.PENDING);
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));

        CheckInRequest req = new CheckInRequest();
        req.setBookingId("booking-uuid-001");

        assertThatThrownBy(() -> bookingService.webCheckIn(req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only confirmed bookings");
    }

    @Test
    void webCheckIn_already_checked_in_throws() {
        confirmedBooking.setCheckedIn(true);
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));

        CheckInRequest req = new CheckInRequest();
        req.setBookingId("booking-uuid-001");

        assertThatThrownBy(() -> bookingService.webCheckIn(req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Already checked in");
    }

    @Test
    void webCheckIn_with_new_seats_calls_hold_and_confirm() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(flightClient.getFlightById(1)).thenReturn(mockFlight);

        CheckInRequest req = new CheckInRequest();
        req.setBookingId("booking-uuid-001");
        req.setNewSeatIds(List.of(301));

        bookingService.webCheckIn(req, 1);

        verify(seatClient).holdSeat(301, "1", 1);
        verify(seatClient).confirmSeat(301);
    }

    @Test
    void webCheckIn_booking_not_found_throws() {
        when(bookingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        CheckInRequest req = new CheckInRequest();
        req.setBookingId("nonexistent");

        assertThatThrownBy(() -> bookingService.webCheckIn(req, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found");
    }

    // ── updateStatus ──────────────────────────────────────────

    @Test
    void updateStatus_to_completed() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.updateStatus("booking-uuid-001", "COMPLETED");

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.COMPLETED);
    }

    @Test
    void updateStatus_to_no_show() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.updateStatus("booking-uuid-001", "NO_SHOW");

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.NO_SHOW);
    }

    @Test
    void updateStatus_invalid_throws() {
        when(bookingRepository.findById("booking-uuid-001")).thenReturn(Optional.of(confirmedBooking));

        assertThatThrownBy(() -> bookingService.updateStatus("booking-uuid-001", "FLYING"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateStatus_not_found_throws() {
        when(bookingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.updateStatus("nonexistent", "COMPLETED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found");
    }

    // ── analytics ─────────────────────────────────────────────

    @Test
    void countTotalBookings_returns_count() {
        when(bookingRepository.countTotalBookings()).thenReturn(42L);

        assertThat(bookingService.countTotalBookings()).isEqualTo(42L);
    }

    @Test
    void sumTotalRevenue_returns_value() {
        when(bookingRepository.sumTotalRevenue()).thenReturn(150000.0);

        assertThat(bookingService.sumTotalRevenue()).isEqualTo(150000.0);
    }

    @Test
    void sumTotalRevenue_null_from_repo_returns_zero() {
        when(bookingRepository.sumTotalRevenue()).thenReturn(null);

        assertThat(bookingService.sumTotalRevenue()).isEqualTo(0.0);
    }

    @Test
    void sumRevenueByFlight_returns_value() {
        when(bookingRepository.sumRevenueByFlight(1)).thenReturn(50000.0);

        assertThat(bookingService.sumRevenueByFlight(1)).isEqualTo(50000.0);
    }

    @Test
    void sumRevenueByFlight_null_returns_zero() {
        when(bookingRepository.sumRevenueByFlight(99)).thenReturn(null);

        assertThat(bookingService.sumRevenueByFlight(99)).isEqualTo(0.0);
    }

    @Test
    void countConfirmedByFlight_returns_count() {
        when(bookingRepository.countByFlightIdAndStatus(1, Booking.BookingStatus.CONFIRMED)).thenReturn(5);

        assertThat(bookingService.countConfirmedByFlight(1)).isEqualTo(5);
    }

    // ── getUpcomingBookings ───────────────────────────────────

    @Test
    void getUpcomingBookings_returns_list() {
        when(bookingRepository.findUpcomingByUser(eq(1), any(LocalDateTime.class)))
                .thenReturn(List.of(confirmedBooking));

        List<Booking> result = bookingService.getUpcomingBookings(1);

        assertThat(result).hasSize(1);
    }

    @Test
    void getUpcomingBookings_empty() {
        when(bookingRepository.findUpcomingByUser(eq(99), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<Booking> result = bookingService.getUpcomingBookings(99);

        assertThat(result).isEmpty();
    }

    // ── getAllBookings ─────────────────────────────────────────

    @Test
    void getAllBookings_returns_all() {
        when(bookingRepository.findAll()).thenReturn(List.of(pendingBooking, confirmedBooking));

        List<Booking> result = bookingService.getAllBookings();

        assertThat(result).hasSize(2);
    }

    // ── generatePnr ───────────────────────────────────────────

    @Test
    void generatePnr_is_6_chars_alphanumeric() {
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());

        String pnr = bookingService.generatePnr();

        assertThat(pnr).hasSize(6).matches("[A-Z0-9]{6}");
    }

    @Test
    void generatePnr_uniqueness_across_multiple_calls() {
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());

        Set<String> pnrs = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            pnrs.add(bookingService.generatePnr());
        }
        // Very high probability all 100 are unique
        assertThat(pnrs.size()).isGreaterThan(90);
    }
}