package com.aeronix.booking_service.controller;

import com.aeronix.booking_service.dto.*;
import com.aeronix.booking_service.entity.Booking;
import com.aeronix.booking_service.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private BookingService bookingService;

    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        sampleBooking = Booking.builder()
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
                .seatClass("ECONOMY")
                .tripType(Booking.TripType.ONE_WAY)
                .build();
    }

    @Test
    void createBooking_returns_201() throws Exception {
        PassengerRequest p = new PassengerRequest();
        p.setFirstName("John");
        p.setLastName("Doe");

        BookingRequest req = new BookingRequest();
        req.setFlightId(1);
        req.setSeatClass("ECONOMY");
        req.setPassengers(List.of(p));
        req.setContactEmail("john@example.com");

        when(bookingService.createBooking(any(), eq(1))).thenReturn(sampleBooking);

        mockMvc.perform(post("/api/bookings")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value("booking-uuid-001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getById_found_returns_200() throws Exception {
        when(bookingService.getBookingById("booking-uuid-001")).thenReturn(Optional.of(sampleBooking));

        mockMvc.perform(get("/api/bookings/booking-uuid-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnrCode").value("ABC123"));
    }

    @Test
    void getById_not_found_returns_404() throws Exception {
        when(bookingService.getBookingById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/bookings/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByPnr_found_returns_200() throws Exception {
        when(bookingService.getBookingByPnr("ABC123")).thenReturn(Optional.of(sampleBooking));

        mockMvc.perform(get("/api/bookings/pnr/ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value("booking-uuid-001"));
    }

    @Test
    void getByPnr_not_found_returns_404() throws Exception {
        when(bookingService.getBookingByPnr("ZZZ999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/bookings/pnr/ZZZ999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyBookings_returns_200() throws Exception {
        when(bookingService.getBookingsByUser(1)).thenReturn(List.of(sampleBooking));

        mockMvc.perform(get("/api/bookings/my").header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getUpcoming_returns_200() throws Exception {
        when(bookingService.getUpcomingBookings(1)).thenReturn(List.of(sampleBooking));

        mockMvc.perform(get("/api/bookings/my/upcoming").header("X-User-Id", 1))
                .andExpect(status().isOk());
    }

    @Test
    void cancelBooking_admin_returns_200() throws Exception {
        sampleBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingService.cancelBooking(eq("booking-uuid-001"), any(), isNull()))
                .thenReturn(sampleBooking);

        CancelRequest req = new CancelRequest();
        req.setReason("Admin cancel");

        mockMvc.perform(put("/api/bookings/booking-uuid-001/cancel")
                        .header("X-User-Id", 1)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void cancelBooking_passenger_passes_userId() throws Exception {
        sampleBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingService.cancelBooking(eq("booking-uuid-001"), any(), eq(1)))
                .thenReturn(sampleBooking);

        CancelRequest req = new CancelRequest();
        req.setReason("My cancel");

        mockMvc.perform(put("/api/bookings/booking-uuid-001/cancel")
                        .header("X-User-Id", 1)
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void calculateFare_returns_200() throws Exception {
        FareSummary fare = FareSummary.builder()
                .baseFare(5000.0).totalFare(5350.0).totalTax(350.0)
                .gstAmount(250.0).fuelSurcharge(100.0).luggageCharge(0.0)
                .passengerCount(1).seatClass("ECONOMY").build();

        when(bookingService.calculateFare(1, "ECONOMY", 1, null)).thenReturn(fare);

        mockMvc.perform(get("/api/bookings/fare")
                        .param("flightId", "1")
                        .param("seatClass", "ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseFare").value(5000.0))
                .andExpect(jsonPath("$.totalFare").value(5350.0));
    }

    @Test
    void getAllBookings_admin_returns_200() throws Exception {
        when(bookingService.getAllBookings()).thenReturn(List.of(sampleBooking));

        mockMvc.perform(get("/api/bookings").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllBookings_non_admin_returns_403() throws Exception {
        mockMvc.perform(get("/api/bookings").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByFlight_airline_staff_returns_200() throws Exception {
        when(bookingService.getBookingsByFlight(1)).thenReturn(List.of(sampleBooking));

        mockMvc.perform(get("/api/bookings/flight/1").header("X-User-Role", "AIRLINE_STAFF"))
                .andExpect(status().isOk());
    }

    @Test
    void getByFlight_passenger_returns_403() throws Exception {
        mockMvc.perform(get("/api/bookings/flight/1").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFlightRevenue_admin_returns_200() throws Exception {
        when(bookingService.sumRevenueByFlight(1)).thenReturn(50000.0);
        when(bookingService.countConfirmedByFlight(1)).thenReturn(10);

        mockMvc.perform(get("/api/bookings/flight/1/revenue").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(50000.0))
                .andExpect(jsonPath("$.confirmedBookings").value(10));
    }

    @Test
    void getFlightRevenue_passenger_returns_403() throws Exception {
        mockMvc.perform(get("/api/bookings/flight/1/revenue").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAnalytics_admin_returns_200() throws Exception {
        when(bookingService.countTotalBookings()).thenReturn(100L);
        when(bookingService.sumTotalRevenue()).thenReturn(500000.0);

        mockMvc.perform(get("/api/bookings/analytics").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(100))
                .andExpect(jsonPath("$.totalRevenue").value(500000.0));
    }

    @Test
    void getAnalytics_non_admin_returns_403() throws Exception {
        mockMvc.perform(get("/api/bookings/analytics").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmAfterPayment_returns_200() throws Exception {
        sampleBooking.setStatus(Booking.BookingStatus.CONFIRMED);
        when(bookingService.confirmBookingAfterPayment("booking-uuid-001", "pay-001"))
                .thenReturn(sampleBooking);

        mockMvc.perform(put("/api/bookings/booking-uuid-001/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"pay-001\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_returns_200() throws Exception {
        sampleBooking.setStatus(Booking.BookingStatus.COMPLETED);
        when(bookingService.updateStatus("booking-uuid-001", "COMPLETED")).thenReturn(sampleBooking);

        mockMvc.perform(put("/api/bookings/booking-uuid-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void getByStatus_admin_returns_200() throws Exception {
        when(bookingService.getBookingsByStatus("PENDING")).thenReturn(List.of(sampleBooking));

        mockMvc.perform(get("/api/bookings/status/PENDING").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getByStatus_non_admin_returns_403() throws Exception {
        mockMvc.perform(get("/api/bookings/status/PENDING").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void webCheckIn_returns_200() throws Exception {
        confirmedBooking();
        CheckInRequest req = new CheckInRequest();
        req.setBookingId("booking-uuid-001");

        sampleBooking.setCheckedIn(true);
        when(bookingService.webCheckIn(any(), eq(1))).thenReturn(sampleBooking);

        mockMvc.perform(post("/api/bookings/checkin")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void addAddOn_returns_200() throws Exception {
        AddOnRequest req = new AddOnRequest();
        req.setType("LUGGAGE");
        req.setValue(10.0);

        when(bookingService.addAddOn(eq("booking-uuid-001"), any())).thenReturn(sampleBooking);

        mockMvc.perform(post("/api/bookings/booking-uuid-001/addons")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private void confirmedBooking() {
        sampleBooking.setStatus(Booking.BookingStatus.CONFIRMED);
    }
}