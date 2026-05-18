package com.aeronix.flight_service.controller;

import com.aeronix.flight_service.dto.*;
import com.aeronix.flight_service.entity.Flight;
import com.aeronix.flight_service.service.FlightService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlightController.class)
class FlightControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private FlightService flightService;

    private ObjectMapper objectMapper;
    private Flight sampleFlight;
    private FlightRequest flightRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleFlight = Flight.builder()
                .flightId(1).flightNumber("AI101").airlineId(1)
                .originAirportCode("DEL").destinationAirportCode("BOM")
                .departureTime(LocalDateTime.of(2026, 6, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 6, 1, 12, 15))
                .totalSeats(180).availableSeats(180)
                .basePrice(5000.0).economyPrice(5000.0)
                .businessPrice(12500.0).firstClassPrice(20000.0)
                .stops(0).status(Flight.FlightStatus.ON_TIME)
                .build();

        flightRequest = new FlightRequest();
        flightRequest.setFlightNumber("AI101");
        flightRequest.setAirlineId(1);
        flightRequest.setOriginAirportCode("DEL");
        flightRequest.setDestinationAirportCode("BOM");
        flightRequest.setDepartureTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        flightRequest.setArrivalTime(LocalDateTime.of(2026, 6, 1, 12, 15));
        flightRequest.setTotalSeats(180);
        flightRequest.setBasePrice(5000.0);
    }

    @Test
    void searchFlights_returns_200() throws Exception {
        when(flightService.searchFlights(any())).thenReturn(List.of(sampleFlight));

        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BOM")
                        .param("date", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].flightNumber").value("AI101"));
    }

    @Test
    void searchFlights_with_all_filters_returns_200() throws Exception {
        when(flightService.searchFlights(any())).thenReturn(List.of(sampleFlight));

        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BOM")
                        .param("date", "2026-06-01")
                        .param("passengers", "2")
                        .param("minPrice", "3000")
                        .param("maxPrice", "8000")
                        .param("airlineId", "1")
                        .param("seatClass", "ECONOMY")
                        .param("maxStops", "0")
                        .param("sortBy", "price"))
                .andExpect(status().isOk());
    }

    @Test
    void searchFlights_dd_MM_yyyy_date_format_accepted() throws Exception {
        when(flightService.searchFlights(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BOM")
                        .param("date", "01-06-2026"))
                .andExpect(status().isOk());
    }

    @Test
    void searchRoundTrip_returns_200() throws Exception {
        RoundTripResponse response = new RoundTripResponse(List.of(sampleFlight), List.of());
        when(flightService.searchRoundTrip(any())).thenReturn(response);

        mockMvc.perform(get("/api/flights/round-trip")
                        .param("origin", "DEL")
                        .param("destination", "BOM")
                        .param("departureDate", "2026-06-01")
                        .param("returnDate", "2026-06-10"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_found_returns_200() throws Exception {
        when(flightService.getFlightById(1)).thenReturn(Optional.of(sampleFlight));

        mockMvc.perform(get("/api/flights/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightNumber").value("AI101"));
    }

    @Test
    void getById_not_found_returns_404() throws Exception {
        when(flightService.getFlightById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/flights/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByNumber_found_returns_200() throws Exception {
        when(flightService.getFlightByNumber("AI101")).thenReturn(Optional.of(sampleFlight));

        mockMvc.perform(get("/api/flights/number/AI101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(1));
    }

    @Test
    void getByNumber_not_found_returns_404() throws Exception {
        when(flightService.getFlightByNumber("XX999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/flights/number/XX999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returns_200() throws Exception {
        when(flightService.getAllFlights()).thenReturn(List.of(sampleFlight));

        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByStatus_returns_200() throws Exception {
        when(flightService.getFlightsByStatus("ON_TIME")).thenReturn(List.of(sampleFlight));

        mockMvc.perform(get("/api/flights/status/ON_TIME"))
                .andExpect(status().isOk());
    }

    @Test
    void addFlight_airline_staff_returns_201() throws Exception {
        when(flightService.addFlight(any())).thenReturn(sampleFlight);

        mockMvc.perform(post("/api/flights")
                        .header("X-User-Role", "AIRLINE_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flightRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightNumber").value("AI101"));
    }

    @Test
    void addFlight_admin_returns_201() throws Exception {
        when(flightService.addFlight(any())).thenReturn(sampleFlight);

        mockMvc.perform(post("/api/flights")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flightRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void addFlight_passenger_returns_403() throws Exception {
        mockMvc.perform(post("/api/flights")
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flightRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFlight_airline_staff_returns_200() throws Exception {
        when(flightService.updateFlight(eq(1), any())).thenReturn(sampleFlight);

        mockMvc.perform(put("/api/flights/1")
                        .header("X-User-Role", "AIRLINE_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flightRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void updateFlight_passenger_returns_403() throws Exception {
        mockMvc.perform(put("/api/flights/1")
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flightRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_airline_staff_returns_200() throws Exception {
        doNothing().when(flightService).updateStatus(eq(1), any());

        mockMvc.perform(put("/api/flights/1/status")
                        .header("X-User-Role", "AIRLINE_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELAYED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void updateStatus_passenger_returns_403() throws Exception {
        mockMvc.perform(put("/api/flights/1/status")
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELAYED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteFlight_admin_returns_200() throws Exception {
        doNothing().when(flightService).deleteFlight(1);

        mockMvc.perform(delete("/api/flights/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Flight deleted"));
    }

    @Test
    void deleteFlight_passenger_returns_403() throws Exception {
        mockMvc.perform(delete("/api/flights/1")
                        .header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByAirline_returns_200() throws Exception {
        when(flightService.getFlightsByAirline(1)).thenReturn(List.of(sampleFlight));

        mockMvc.perform(get("/api/flights/airline/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void countByAirline_returns_200() throws Exception {
        when(flightService.countByAirline(1)).thenReturn(5L);

        mockMvc.perform(get("/api/flights/airline/1/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void decrementSeats_returns_200() throws Exception {
        doNothing().when(flightService).decrementSeats(1, 2);

        mockMvc.perform(put("/api/flights/1/seats/decrement").param("count", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seats decremented"));
    }

    @Test
    void incrementSeats_returns_200() throws Exception {
        doNothing().when(flightService).incrementSeats(1, 2);

        mockMvc.perform(put("/api/flights/1/seats/increment").param("count", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seats incremented"));
    }
}