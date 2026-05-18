package com.aeronix.passenger_service.controller;

import com.aeronix.passenger_service.dto.*;
import com.aeronix.passenger_service.entity.Passenger;
import com.aeronix.passenger_service.service.PassengerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PassengerController.class)
class PassengerControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private PassengerService passengerService;

    private ObjectMapper objectMapper;
    private Passenger samplePassenger;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        samplePassenger = Passenger.builder()
                .passengerId(1).bookingId("booking-uuid-001")
                .title("Mr").firstName("John").lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .passengerType(Passenger.PassengerType.ADULT)
                .mealPreference(Passenger.MealPreference.VEG)
                .checkedIn(false).build();
    }

    @Test
    void addPassenger_returns_201() throws Exception {
        when(passengerService.addPassenger(any())).thenReturn(samplePassenger);

        Map<String, Object> req = Map.of(
                "bookingId", "booking-uuid-001",
                "firstName", "John", "lastName", "Doe",
                "dateOfBirth", "1990-05-15", "passengerType", "ADULT"
        );

        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void addPassengers_bulk_returns_201() throws Exception {
        when(passengerService.addPassengers(any())).thenReturn(List.of(samplePassenger));

        Map<String, Object> req = Map.of(
                "bookingId", "booking-uuid-001",
                "passengers", List.of(Map.of("firstName", "John", "lastName", "Doe"))
        );

        mockMvc.perform(post("/api/passengers/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getById_found_returns_200() throws Exception {
        when(passengerService.getPassengerById(1)).thenReturn(Optional.of(samplePassenger));

        mockMvc.perform(get("/api/passengers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void getById_not_found_returns_404() throws Exception {
        when(passengerService.getPassengerById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/passengers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByBooking_returns_200() throws Exception {
        when(passengerService.getPassengersByBooking("booking-uuid-001"))
                .thenReturn(List.of(samplePassenger));

        mockMvc.perform(get("/api/passengers/booking/booking-uuid-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCount_returns_200() throws Exception {
        when(passengerService.getPassengerCount("booking-uuid-001")).thenReturn(2);

        mockMvc.perform(get("/api/passengers/booking/booking-uuid-001/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void getByPassport_airline_staff_returns_200() throws Exception {
        when(passengerService.getByPassportNumber("P1234567"))
                .thenReturn(Optional.of(samplePassenger));

        mockMvc.perform(get("/api/passengers/passport/P1234567")
                        .header("X-User-Role", "AIRLINE_STAFF"))
                .andExpect(status().isOk());
    }

    @Test
    void getByPassport_admin_returns_200() throws Exception {
        when(passengerService.getByPassportNumber("P1234567"))
                .thenReturn(Optional.of(samplePassenger));

        mockMvc.perform(get("/api/passengers/passport/P1234567")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getByPassport_passenger_returns_403() throws Exception {
        mockMvc.perform(get("/api/passengers/passport/P1234567")
                        .header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByPassport_not_found_returns_404() throws Exception {
        when(passengerService.getByPassportNumber("ZZZZ"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/passengers/passport/ZZZZ")
                        .header("X-User-Role", "AIRLINE_STAFF"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByTicket_found_returns_200() throws Exception {
        when(passengerService.getByTicketNumber("SKB12345")).thenReturn(Optional.of(samplePassenger));

        mockMvc.perform(get("/api/passengers/ticket/SKB12345"))
                .andExpect(status().isOk());
    }

    @Test
    void getByTicket_not_found_returns_404() throws Exception {
        when(passengerService.getByTicketNumber("NOTFOUND")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/passengers/ticket/NOTFOUND"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePassenger_returns_200() throws Exception {
        when(passengerService.updatePassenger(eq(1), any())).thenReturn(samplePassenger);

        mockMvc.perform(put("/api/passengers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Johnny\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void assignSeat_returns_200() throws Exception {
        when(passengerService.assignSeat(eq(1), any())).thenReturn(samplePassenger);

        mockMvc.perform(put("/api/passengers/1/seat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatId\":101,\"seatNumber\":\"12A\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void checkIn_returns_200() throws Exception {
        samplePassenger.setCheckedIn(true);
        when(passengerService.checkInPassenger(1)).thenReturn(samplePassenger);

        mockMvc.perform(put("/api/passengers/1/checkin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedIn").value(true));
    }

    @Test
    void deletePassenger_returns_200() throws Exception {
        doNothing().when(passengerService).deletePassenger(1);

        mockMvc.perform(delete("/api/passengers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Passenger deleted"));
    }

    @Test
    void deleteByBooking_returns_200() throws Exception {
        doNothing().when(passengerService).deletePassengersByBooking("booking-uuid-001");

        mockMvc.perform(delete("/api/passengers/booking/booking-uuid-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "All passengers deleted for booking booking-uuid-001"));
    }

    @Test
    void validate_returns_200() throws Exception {
        ValidationResult result = new ValidationResult(true, Collections.emptyList());
        when(passengerService.validatePassengerData(any())).thenReturn(result);

        mockMvc.perform(post("/api/passengers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"John\",\"lastName\":\"Doe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void getManifest_airline_staff_returns_200() throws Exception {
        ManifestEntry entry = ManifestEntry.builder()
                .passengerId(1).bookingId("booking-uuid-001")
                .pnrCode("ABC123").fullName("Mr John Doe")
                .seatClass("ECONOMY").passengerType("ADULT")
                .mealPreference("VEG").checkedIn(false).build();

        when(passengerService.getFlightManifest(1)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/passengers/flight/1/manifest")
                        .header("X-User-Role", "AIRLINE_STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pnrCode").value("ABC123"));
    }

    @Test
    void getManifest_admin_returns_200() throws Exception {
        when(passengerService.getFlightManifest(1)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/passengers/flight/1/manifest")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getManifest_passenger_returns_403() throws Exception {
        mockMvc.perform(get("/api/passengers/flight/1/manifest")
                        .header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByFlight_airline_staff_returns_200() throws Exception {
        when(passengerService.getPassengersByFlight(1)).thenReturn(List.of(samplePassenger));

        mockMvc.perform(get("/api/passengers/flight/1")
                        .header("X-User-Role", "AIRLINE_STAFF"))
                .andExpect(status().isOk());
    }

    @Test
    void getByFlight_passenger_returns_403() throws Exception {
        mockMvc.perform(get("/api/passengers/flight/1")
                        .header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }
}