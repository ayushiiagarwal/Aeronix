package com.aeronix.airline_service.controller;

import com.aeronix.airline_service.dto.*;
import com.aeronix.airline_service.entity.Airline;
import com.aeronix.airline_service.entity.Airport;
import com.aeronix.airline_service.service.AirlineService;
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

@WebMvcTest(AirlineController.class)
class AirlineControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private AirlineService airlineService;

    private ObjectMapper objectMapper;
    private Airline sampleAirline;
    private Airport sampleAirport;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        sampleAirline = Airline.builder()
                .airlineId(1).name("IndiGo").iataCode("6E")
                .country("India").isActive(true).build();

        sampleAirport = Airport.builder()
                .airportId(1).name("Indira Gandhi International")
                .iataCode("DEL").city("New Delhi").country("India")
                .isActive(true).build();
    }

    // ── Airlines ──────────────────────────────────────────────

    @Test
    void getAllAirlines_returns_200() throws Exception {
        when(airlineService.getActiveAirlines()).thenReturn(List.of(sampleAirline));

        mockMvc.perform(get("/api/airlines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].iataCode").value("6E"));
    }

    @Test
    void getAllIncludingInactive_admin_returns_200() throws Exception {
        when(airlineService.getAllAirlines()).thenReturn(List.of(sampleAirline));

        mockMvc.perform(get("/api/airlines/all").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllIncludingInactive_non_admin_returns_403() throws Exception {
        mockMvc.perform(get("/api/airlines/all").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_found_returns_200() throws Exception {
        when(airlineService.getAirlineById(1)).thenReturn(Optional.of(sampleAirline));

        mockMvc.perform(get("/api/airlines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IndiGo"));
    }

    @Test
    void getById_not_found_returns_404() throws Exception {
        when(airlineService.getAirlineById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/airlines/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIata_found_returns_200() throws Exception {
        when(airlineService.getAirlineByIata("6E")).thenReturn(Optional.of(sampleAirline));

        mockMvc.perform(get("/api/airlines/iata/6E"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("6E"));
    }

    @Test
    void getByIata_not_found_returns_404() throws Exception {
        when(airlineService.getAirlineByIata("XX")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/airlines/iata/XX"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchAirlines_returns_200() throws Exception {
        when(airlineService.searchAirlines("indigo")).thenReturn(List.of(sampleAirline));

        mockMvc.perform(get("/api/airlines/search").param("query", "indigo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getStats_returns_200() throws Exception {
        AirlineResponse response = AirlineResponse.builder()
                .airlineId(1).name("IndiGo").iataCode("6E")
                .totalFlights(25L).isActive(true).build();

        when(airlineService.getAirlineStats(1)).thenReturn(response);

        mockMvc.perform(get("/api/airlines/1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFlights").value(25));
    }

    @Test
    void createAirline_admin_returns_201() throws Exception {
        when(airlineService.createAirline(any())).thenReturn(sampleAirline);

        Map<String, String> req = Map.of("name", "IndiGo", "iataCode", "6E", "country", "India");

        mockMvc.perform(post("/api/airlines")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("IndiGo"));
    }

    @Test
    void createAirline_non_admin_returns_403() throws Exception {
        mockMvc.perform(post("/api/airlines")
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAirline_admin_returns_200() throws Exception {
        when(airlineService.updateAirline(eq(1), any())).thenReturn(sampleAirline);

        mockMvc.perform(put("/api/airlines/1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"IndiGo Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateAirline_non_admin_returns_403() throws Exception {
        mockMvc.perform(put("/api/airlines/1")
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateAirline_admin_returns_200() throws Exception {
        doNothing().when(airlineService).deactivateAirline(1);

        mockMvc.perform(put("/api/airlines/1/deactivate").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Airline deactivated"));
    }

    @Test
    void deactivateAirline_non_admin_returns_403() throws Exception {
        mockMvc.perform(put("/api/airlines/1/deactivate").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void activateAirline_admin_returns_200() throws Exception {
        doNothing().when(airlineService).activateAirline(1);

        mockMvc.perform(put("/api/airlines/1/activate").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Airline activated"));
    }

    @Test
    void activateAirline_non_admin_returns_403() throws Exception {
        mockMvc.perform(put("/api/airlines/1/activate").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAirline_admin_returns_200() throws Exception {
        doNothing().when(airlineService).deleteAirline(1);

        mockMvc.perform(delete("/api/airlines/1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Airline deleted"));
    }

    @Test
    void deleteAirline_non_admin_returns_403() throws Exception {
        mockMvc.perform(delete("/api/airlines/1").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    // ── Airports ──────────────────────────────────────────────

    @Test
    void getAllAirports_returns_200() throws Exception {
        when(airlineService.getActiveAirports()).thenReturn(List.of(sampleAirport));

        mockMvc.perform(get("/api/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllAirportsIncludingInactive_admin_returns_200() throws Exception {
        when(airlineService.getAllAirports()).thenReturn(List.of(sampleAirport));

        mockMvc.perform(get("/api/airports/all").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllAirportsIncludingInactive_non_admin_returns_403() throws Exception {
        mockMvc.perform(get("/api/airports/all").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAirportById_found_returns_200() throws Exception {
        when(airlineService.getAirportById(1)).thenReturn(Optional.of(sampleAirport));

        mockMvc.perform(get("/api/airports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("DEL"));
    }

    @Test
    void getAirportById_not_found_returns_404() throws Exception {
        when(airlineService.getAirportById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/airports/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAirportByIata_found_returns_200() throws Exception {
        when(airlineService.getAirportByIata("DEL")).thenReturn(Optional.of(sampleAirport));

        mockMvc.perform(get("/api/airports/iata/DEL"))
                .andExpect(status().isOk());
    }

    @Test
    void getAirportByIata_not_found_returns_404() throws Exception {
        when(airlineService.getAirportByIata("ZZZ")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/airports/iata/ZZZ"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchAirports_autocomplete_returns_200() throws Exception {
        AirportSearchResponse response = AirportSearchResponse.builder()
                .airportId(1).iataCode("DEL").name("Indira Gandhi International")
                .city("New Delhi").country("India")
                .displayName("Indira Gandhi International (DEL), New Delhi, India").build();

        when(airlineService.searchAirportsForAutocomplete("del")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/airports/search").param("query", "del"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].iataCode").value("DEL"));
    }

    @Test
    void searchAirportsRaw_returns_200() throws Exception {
        when(airlineService.searchAirports("del")).thenReturn(List.of(sampleAirport));

        mockMvc.perform(get("/api/airports/search/raw").param("query", "del"))
                .andExpect(status().isOk());
    }

    @Test
    void getByCity_returns_200() throws Exception {
        when(airlineService.getAirportsByCity("Delhi")).thenReturn(List.of(sampleAirport));

        mockMvc.perform(get("/api/airports/city/Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByCountry_returns_200() throws Exception {
        when(airlineService.getAirportsByCountry("India")).thenReturn(List.of(sampleAirport));

        mockMvc.perform(get("/api/airports/country/India"))
                .andExpect(status().isOk());
    }

    @Test
    void createAirport_admin_returns_201() throws Exception {
        when(airlineService.createAirport(any())).thenReturn(sampleAirport);

        Map<String, Object> req = Map.of(
                "name", "Indira Gandhi International",
                "iataCode", "DEL", "city", "New Delhi", "country", "India"
        );

        mockMvc.perform(post("/api/airports")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void createAirport_non_admin_returns_403() throws Exception {
        mockMvc.perform(post("/api/airports")
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAirport_admin_returns_200() throws Exception {
        when(airlineService.updateAirport(eq(1), any())).thenReturn(sampleAirport);

        mockMvc.perform(put("/api/airports/1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Airport\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateAirport_non_admin_returns_403() throws Exception {
        mockMvc.perform(put("/api/airports/1")
                        .header("X-User-Role", "PASSENGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateAirport_admin_returns_200() throws Exception {
        doNothing().when(airlineService).deactivateAirport(1);

        mockMvc.perform(put("/api/airports/1/deactivate").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Airport deactivated"));
    }

    @Test
    void deactivateAirport_non_admin_returns_403() throws Exception {
        mockMvc.perform(put("/api/airports/1/deactivate").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void activateAirport_admin_returns_200() throws Exception {
        doNothing().when(airlineService).activateAirport(1);

        mockMvc.perform(put("/api/airports/1/activate").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Airport activated"));
    }

    @Test
    void activateAirport_non_admin_returns_403() throws Exception {
        mockMvc.perform(put("/api/airports/1/activate").header("X-User-Role", "PASSENGER"))
                .andExpect(status().isForbidden());
    }
}