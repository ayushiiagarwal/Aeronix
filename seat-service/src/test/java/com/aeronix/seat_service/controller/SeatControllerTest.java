package com.aeronix.seat_service.controller;

import com.aeronix.seat_service.dto.*;
import com.aeronix.seat_service.entity.Seat;
import com.aeronix.seat_service.service.SeatService;
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

@WebMvcTest(SeatController.class)
class SeatControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private SeatService seatService;

    private ObjectMapper objectMapper;
    private Seat availableSeat;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        availableSeat = Seat.builder()
                .seatId(1).flightId(1).seatNumber("12A")
                .seatClass(Seat.SeatClass.ECONOMY).seatRow(12).seatColumn("A")
                .isWindow(true).isAisle(false).hasExtraLegroom(false)
                .priceMultiplier(1.1).status(Seat.SeatStatus.AVAILABLE).build();
    }

    @Test
    void addSeats_airline_staff_returns_201() throws Exception {
        when(seatService.addSeatsForFlight(any())).thenReturn(List.of(availableSeat));

        Map<String, Object> req = Map.of("flightId", 1, "economyRows", 5, "seatsPerRow", 6);

        mockMvc.perform(post("/api/seats")
                        .header("X-User-Role", "AIRLINE_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void addSeats_admin_returns_201() throws Exception {
        when(seatService.addSeatsForFlight(any())).thenReturn(List.of(availableSeat));

        Map<String, Object> req = Map.of("flightId", 1, "economyRows", 5, "seatsPerRow", 6);

        mockMvc.perform(post("/api/seats")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void addSeats_passenger_returns_403() throws Exception {
        mockMvc.perform(post("/api/seats")
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSeat_airline_staff_returns_200() throws Exception {
        when(seatService.updateSeat(eq(1), any())).thenReturn(availableSeat);

        Map<String, Object> req = Map.of("seatClass", "ECONOMY", "window", true,
                "aisle", false, "hasExtraLegroom", false);

        mockMvc.perform(put("/api/seats/1")
                        .header("X-User-Role", "AIRLINE_STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void updateSeat_passenger_returns_403() throws Exception {
        mockMvc.perform(put("/api/seats/1")
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void blockSeat_admin_returns_200() throws Exception {
        doNothing().when(seatService).blockSeat(1);

        mockMvc.perform(put("/api/seats/1/block").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seat blocked"));
    }

    @Test
    void blockSeat_passenger_returns_403() throws Exception {
        mockMvc.perform(put("/api/seats/1/block").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSeatsForFlight_airline_staff_returns_200() throws Exception {
        doNothing().when(seatService).deleteSeatsForFlight(1);

        mockMvc.perform(delete("/api/seats/flight/1").header("X-User-Role", "AIRLINE_STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteSeatsForFlight_passenger_returns_403() throws Exception {
        mockMvc.perform(delete("/api/seats/flight/1").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllByFlight_returns_200() throws Exception {
        when(seatService.getAllSeatsByFlight(1)).thenReturn(List.of(availableSeat));

        mockMvc.perform(get("/api/seats/flight/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].seatNumber").value("12A"));
    }

    @Test
    void getAvailable_returns_200() throws Exception {
        when(seatService.getAvailableSeats(1)).thenReturn(List.of(availableSeat));

        mockMvc.perform(get("/api/seats/flight/1/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAvailableByClass_returns_200() throws Exception {
        when(seatService.getAvailableByClass(1, "ECONOMY")).thenReturn(List.of(availableSeat));

        mockMvc.perform(get("/api/seats/flight/1/available/ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getSeatMap_returns_200() throws Exception {
        SeatMapResponse mapResponse = new SeatMapResponse(1, Map.of("ECONOMY",
                List.of(availableSeat)), 1, 1);
        when(seatService.getSeatMap(1)).thenReturn(mapResponse);

        mockMvc.perform(get("/api/seats/flight/1/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(1));
    }

    @Test
    void countAvailable_returns_200() throws Exception {
        when(seatService.countAvailableByClass(1, "ECONOMY")).thenReturn(50);

        mockMvc.perform(get("/api/seats/flight/1/count/ECONOMY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(50));
    }

    @Test
    void getById_found_returns_200() throws Exception {
        when(seatService.getSeatById(1)).thenReturn(Optional.of(availableSeat));

        mockMvc.perform(get("/api/seats/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatId").value(1));
    }

    @Test
    void getById_not_found_returns_404() throws Exception {
        when(seatService.getSeatById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/seats/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void holdSeat_returns_200() throws Exception {
        availableSeat.setStatus(Seat.SeatStatus.HELD);
        when(seatService.holdSeat(eq(1), any())).thenReturn(availableSeat);

        mockMvc.perform(put("/api/seats/1/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HELD"));
    }

    @Test
    void releaseSeat_returns_200() throws Exception {
        doNothing().when(seatService).releaseSeat(1);

        mockMvc.perform(put("/api/seats/1/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seat released"));
    }

    @Test
    void confirmSeat_returns_200() throws Exception {
        doNothing().when(seatService).confirmSeat(1);

        mockMvc.perform(put("/api/seats/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Seat confirmed"));
    }

    @Test
    void releaseUserHolds_returns_200() throws Exception {
        doNothing().when(seatService).releaseUserHoldsOnFlight(1, "1");

        mockMvc.perform(put("/api/seats/flight/1/release-user").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User seat holds released"));
    }
}