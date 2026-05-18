package com.aeronix.flight_service.service;

import com.aeronix.flight_service.dto.*;
import com.aeronix.flight_service.entity.Flight;
import com.aeronix.flight_service.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {

    @Mock private FlightRepository flightRepository;
    @InjectMocks private FlightServiceImpl flightService;

    private Flight sampleFlight;
    private FlightRequest flightRequest;

    @BeforeEach
    void setUp() {
        sampleFlight = Flight.builder()
                .flightId(1)
                .flightNumber("AI101")
                .airlineId(1)
                .originAirportCode("DEL")
                .destinationAirportCode("BOM")
                .departureTime(LocalDateTime.of(2026, 6, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 6, 1, 12, 15))
                .durationMinutes(135)
                .totalSeats(180)
                .availableSeats(180)
                .basePrice(5000.0)
                .economyPrice(5000.0)
                .businessPrice(12500.0)
                .firstClassPrice(20000.0)
                .stops(0)
                .status(Flight.FlightStatus.ON_TIME)
                .build();

        flightRequest = new FlightRequest();
        flightRequest.setFlightNumber("AI101");
        flightRequest.setAirlineId(1);
        flightRequest.setOriginAirportCode("del");
        flightRequest.setDestinationAirportCode("bom");
        flightRequest.setDepartureTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        flightRequest.setArrivalTime(LocalDateTime.of(2026, 6, 1, 12, 15));
        flightRequest.setTotalSeats(180);
        flightRequest.setBasePrice(5000.0);
    }

    // ── addFlight ─────────────────────────────────────────────

    @Test
    void addFlight_success() {
        when(flightRepository.findByFlightNumber("AI101")).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getFlightNumber()).isEqualTo("AI101");
        assertThat(result.getOriginAirportCode()).isEqualTo("DEL");
        assertThat(result.getDestinationAirportCode()).isEqualTo("BOM");
        assertThat(result.getStatus()).isEqualTo(Flight.FlightStatus.ON_TIME);
        assertThat(result.getAvailableSeats()).isEqualTo(180);
    }

    @Test
    void addFlight_uppercase_airport_codes() {
        when(flightRepository.findByFlightNumber(any())).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        flightRequest.setOriginAirportCode("del");
        flightRequest.setDestinationAirportCode("bom");

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getOriginAirportCode()).isEqualTo("DEL");
        assertThat(result.getDestinationAirportCode()).isEqualTo("BOM");
    }

    @Test
    void addFlight_duplicate_flight_number_throws() {
        when(flightRepository.findByFlightNumber("AI101")).thenReturn(Optional.of(sampleFlight));

        assertThatThrownBy(() -> flightService.addFlight(flightRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flight number already exists");
    }

    @Test
    void addFlight_calculates_duration_when_not_provided() {
        flightRequest.setDurationMinutes(null);
        when(flightRepository.findByFlightNumber(any())).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getDurationMinutes()).isEqualTo(135);
    }

    @Test
    void addFlight_uses_provided_duration() {
        flightRequest.setDurationMinutes(120);
        when(flightRepository.findByFlightNumber(any())).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getDurationMinutes()).isEqualTo(120);
    }

    @Test
    void addFlight_defaults_economy_price_to_base() {
        flightRequest.setEconomyPrice(null);
        when(flightRepository.findByFlightNumber(any())).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getEconomyPrice()).isEqualTo(5000.0);
    }

    @Test
    void addFlight_defaults_business_price_to_2_5x_base() {
        flightRequest.setBusinessPrice(null);
        when(flightRepository.findByFlightNumber(any())).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getBusinessPrice()).isEqualTo(12500.0);
    }

    @Test
    void addFlight_defaults_first_class_price_to_4x_base() {
        flightRequest.setFirstClassPrice(null);
        when(flightRepository.findByFlightNumber(any())).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getFirstClassPrice()).isEqualTo(20000.0);
    }

    @Test
    void addFlight_uses_provided_class_prices() {
        flightRequest.setEconomyPrice(4500.0);
        flightRequest.setBusinessPrice(11000.0);
        flightRequest.setFirstClassPrice(18000.0);
        when(flightRepository.findByFlightNumber(any())).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getEconomyPrice()).isEqualTo(4500.0);
        assertThat(result.getBusinessPrice()).isEqualTo(11000.0);
        assertThat(result.getFirstClassPrice()).isEqualTo(18000.0);
    }

    @Test
    void addFlight_defaults_stops_to_zero() {
        flightRequest.setStops(null);
        when(flightRepository.findByFlightNumber(any())).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getStops()).isEqualTo(0);
    }

    @Test
    void addFlight_with_stops() {
        flightRequest.setStops(1);
        when(flightRepository.findByFlightNumber(any())).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getStops()).isEqualTo(1);
    }

    @Test
    void addFlight_available_seats_equals_total_seats() {
        when(flightRepository.findByFlightNumber(any())).thenReturn(Optional.empty());
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Flight result = flightService.addFlight(flightRequest);

        assertThat(result.getAvailableSeats()).isEqualTo(result.getTotalSeats());
    }

    // ── getFlightById ─────────────────────────────────────────

    @Test
    void getFlightById_found() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));

        Optional<Flight> result = flightService.getFlightById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getFlightNumber()).isEqualTo("AI101");
    }

    @Test
    void getFlightById_not_found() {
        when(flightRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Flight> result = flightService.getFlightById(99);

        assertThat(result).isEmpty();
    }

    // ── getFlightByNumber ─────────────────────────────────────

    @Test
    void getFlightByNumber_found() {
        when(flightRepository.findByFlightNumber("AI101")).thenReturn(Optional.of(sampleFlight));

        Optional<Flight> result = flightService.getFlightByNumber("AI101");

        assertThat(result).isPresent();
    }

    @Test
    void getFlightByNumber_not_found() {
        when(flightRepository.findByFlightNumber("XX999")).thenReturn(Optional.empty());

        Optional<Flight> result = flightService.getFlightByNumber("XX999");

        assertThat(result).isEmpty();
    }

    // ── searchFlights ─────────────────────────────────────────

    @Test
    void searchFlights_returns_results() {
        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setPassengers(1);

        when(flightRepository.findAvailableFlights(eq("DEL"), eq("BOM"),
                any(), any(), eq(1))).thenReturn(List.of(sampleFlight));

        List<Flight> result = flightService.searchFlights(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFlightNumber()).isEqualTo("AI101");
    }

    @Test
    void searchFlights_no_results_returns_empty() {
        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("HYD");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setPassengers(1);

        when(flightRepository.findAvailableFlights(any(), any(), any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        List<Flight> result = flightService.searchFlights(req);

        assertThat(result).isEmpty();
    }

    @Test
    void searchFlights_filters_by_min_price() {
        Flight cheapFlight = Flight.builder().flightId(2).flightNumber("SG101")
                .economyPrice(3000.0).basePrice(3000.0).stops(0).build();

        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setPassengers(1);
        req.setSeatClass("ECONOMY");
        req.setMinPrice(4000.0);

        when(flightRepository.findAvailableFlights(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(sampleFlight, cheapFlight));

        List<Flight> result = flightService.searchFlights(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFlightNumber()).isEqualTo("AI101");
    }

    @Test
    void searchFlights_filters_by_max_price() {
        Flight expensiveFlight = Flight.builder().flightId(3).flightNumber("UK101")
                .economyPrice(8000.0).basePrice(8000.0).stops(0).build();

        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setPassengers(1);
        req.setSeatClass("ECONOMY");
        req.setMaxPrice(6000.0);

        when(flightRepository.findAvailableFlights(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(sampleFlight, expensiveFlight));

        List<Flight> result = flightService.searchFlights(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFlightNumber()).isEqualTo("AI101");
    }

    @Test
    void searchFlights_filters_by_airline() {
        Flight otherAirlineFlight = Flight.builder().flightId(4).flightNumber("6E101")
                .airlineId(2).economyPrice(5000.0).basePrice(5000.0).stops(0).build();

        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setPassengers(1);
        req.setAirlineId(1);

        when(flightRepository.findAvailableFlights(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(sampleFlight, otherAirlineFlight));

        List<Flight> result = flightService.searchFlights(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAirlineId()).isEqualTo(1);
    }

    @Test
    void searchFlights_filters_by_max_stops() {
        Flight connectingFlight = Flight.builder().flightId(5).flightNumber("AI201")
                .airlineId(1).economyPrice(5000.0).basePrice(5000.0).stops(2).build();

        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setPassengers(1);
        req.setMaxStops(0);

        when(flightRepository.findAvailableFlights(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(sampleFlight, connectingFlight));

        List<Flight> result = flightService.searchFlights(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStops()).isEqualTo(0);
    }

    @Test
    void searchFlights_sorts_by_price() {
        Flight cheapFlight = Flight.builder().flightId(2).flightNumber("SG101")
                .basePrice(3000.0).economyPrice(3000.0).stops(0)
                .departureTime(LocalDateTime.now().plusDays(1)).build();

        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setPassengers(1);
        req.setSortBy("price");

        when(flightRepository.findAvailableFlights(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(sampleFlight, cheapFlight));

        List<Flight> result = flightService.searchFlights(req);

        assertThat(result.get(0).getBasePrice()).isLessThanOrEqualTo(result.get(1).getBasePrice());
    }

    @Test
    void searchFlights_sorts_by_duration() {
        Flight shortFlight = Flight.builder().flightId(2).flightNumber("SG101")
                .basePrice(5000.0).economyPrice(5000.0).stops(0).durationMinutes(60).build();

        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setPassengers(1);
        req.setSortBy("duration");

        when(flightRepository.findAvailableFlights(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(sampleFlight, shortFlight));

        List<Flight> result = flightService.searchFlights(req);

        assertThat(result.get(0).getDurationMinutes())
                .isLessThanOrEqualTo(result.get(1).getDurationMinutes());
    }

    @Test
    void searchFlights_sorts_by_departure() {
        Flight laterFlight = Flight.builder().flightId(2).flightNumber("SG101")
                .basePrice(5000.0).economyPrice(5000.0).stops(0)
                .departureTime(LocalDateTime.of(2026, 6, 1, 15, 0)).build();

        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setPassengers(1);
        req.setSortBy("departure");

        when(flightRepository.findAvailableFlights(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(laterFlight, sampleFlight));

        List<Flight> result = flightService.searchFlights(req);

        assertThat(result.get(0).getDepartureTime())
                .isBeforeOrEqualTo(result.get(1).getDepartureTime());
    }

    @Test
    void searchFlights_no_sort_returns_as_is() {
        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setPassengers(1);
        req.setSortBy(null);

        when(flightRepository.findAvailableFlights(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(sampleFlight));

        List<Flight> result = flightService.searchFlights(req);

        assertThat(result).hasSize(1);
    }

    // ── searchRoundTrip ───────────────────────────────────────

    @Test
    void searchRoundTrip_returns_outbound_and_return() {
        Flight returnFlight = Flight.builder().flightId(2).flightNumber("AI102")
                .originAirportCode("BOM").destinationAirportCode("DEL")
                .basePrice(5000.0).economyPrice(5000.0).stops(0).build();

        RoundTripSearchRequest req = new RoundTripSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setReturnDate(LocalDate.of(2026, 6, 10));
        req.setPassengers(1);

        when(flightRepository.findAvailableFlights(eq("DEL"), eq("BOM"), any(), any(), eq(1)))
                .thenReturn(List.of(sampleFlight));
        when(flightRepository.findAvailableFlights(eq("BOM"), eq("DEL"), any(), any(), eq(1)))
                .thenReturn(List.of(returnFlight));

        RoundTripResponse result = flightService.searchRoundTrip(req);

        assertThat(result.getOutboundFlights()).hasSize(1);
        assertThat(result.getReturnFlights()).hasSize(1);
        assertThat(result.getOutboundFlights().get(0).getFlightNumber()).isEqualTo("AI101");
        assertThat(result.getReturnFlights().get(0).getFlightNumber()).isEqualTo("AI102");
    }

    @Test
    void searchRoundTrip_swaps_origin_destination_for_return() {
        RoundTripSearchRequest req = new RoundTripSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setReturnDate(LocalDate.of(2026, 6, 10));
        req.setPassengers(1);

        when(flightRepository.findAvailableFlights(any(), any(), any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        flightService.searchRoundTrip(req);

        verify(flightRepository).findAvailableFlights(eq("DEL"), eq("BOM"), any(), any(), eq(1));
        verify(flightRepository).findAvailableFlights(eq("BOM"), eq("DEL"), any(), any(), eq(1));
    }

    @Test
    void searchRoundTrip_no_return_flights_returns_empty_return_list() {
        RoundTripSearchRequest req = new RoundTripSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 6, 1));
        req.setReturnDate(LocalDate.of(2026, 6, 10));
        req.setPassengers(1);

        when(flightRepository.findAvailableFlights(eq("DEL"), eq("BOM"), any(), any(), eq(1)))
                .thenReturn(List.of(sampleFlight));
        when(flightRepository.findAvailableFlights(eq("BOM"), eq("DEL"), any(), any(), eq(1)))
                .thenReturn(Collections.emptyList());

        RoundTripResponse result = flightService.searchRoundTrip(req);

        assertThat(result.getOutboundFlights()).hasSize(1);
        assertThat(result.getReturnFlights()).isEmpty();
    }

    // ── updateFlight ──────────────────────────────────────────

    @Test
    void updateFlight_success() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        flightRequest.setFlightNumber("AI101-UPD");
        flightRequest.setBasePrice(6000.0);

        Flight result = flightService.updateFlight(1, flightRequest);

        assertThat(result.getFlightNumber()).isEqualTo("AI101-UPD");
        assertThat(result.getBasePrice()).isEqualTo(6000.0);
    }

    @Test
    void updateFlight_uppercase_airport_codes() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        flightRequest.setOriginAirportCode("del");
        flightRequest.setDestinationAirportCode("hyd");

        Flight result = flightService.updateFlight(1, flightRequest);

        assertThat(result.getOriginAirportCode()).isEqualTo("DEL");
        assertThat(result.getDestinationAirportCode()).isEqualTo("HYD");
    }

    @Test
    void updateFlight_not_found_throws() {
        when(flightRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.updateFlight(99, flightRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flight not found");
    }

    @Test
    void updateFlight_partial_update_preserves_nulls() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        flightRequest.setDurationMinutes(null);
        flightRequest.setAircraftType(null);
        flightRequest.setBusinessPrice(null);

        Flight result = flightService.updateFlight(1, flightRequest);

        assertThat(result.getDurationMinutes()).isEqualTo(135);
        assertThat(result.getBusinessPrice()).isEqualTo(12500.0);
    }

    // ── updateStatus ──────────────────────────────────────────

    @Test
    void updateStatus_to_delayed() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("DELAYED");

        flightService.updateStatus(1, req);

        assertThat(sampleFlight.getStatus()).isEqualTo(Flight.FlightStatus.DELAYED);
    }

    @Test
    void updateStatus_to_cancelled() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("CANCELLED");

        flightService.updateStatus(1, req);

        assertThat(sampleFlight.getStatus()).isEqualTo(Flight.FlightStatus.CANCELLED);
    }

    @Test
    void updateStatus_to_departed() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("DEPARTED");

        flightService.updateStatus(1, req);

        assertThat(sampleFlight.getStatus()).isEqualTo(Flight.FlightStatus.DEPARTED);
    }

    @Test
    void updateStatus_invalid_throws() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("FLYING");

        assertThatThrownBy(() -> flightService.updateStatus(1, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateStatus_not_found_throws() {
        when(flightRepository.findById(99)).thenReturn(Optional.empty());

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("DELAYED");

        assertThatThrownBy(() -> flightService.updateStatus(99, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flight not found");
    }

    // ── decrementSeats ────────────────────────────────────────

    @Test
    void decrementSeats_success() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        flightService.decrementSeats(1, 5);

        assertThat(sampleFlight.getAvailableSeats()).isEqualTo(175);
    }

    @Test
    void decrementSeats_exact_available_seats_success() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        flightService.decrementSeats(1, 180);

        assertThat(sampleFlight.getAvailableSeats()).isEqualTo(0);
    }

    @Test
    void decrementSeats_insufficient_throws() {
        sampleFlight.setAvailableSeats(3);
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));

        assertThatThrownBy(() -> flightService.decrementSeats(1, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not enough available seats");
    }

    @Test
    void decrementSeats_flight_not_found_throws() {
        when(flightRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.decrementSeats(99, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flight not found");
    }

    @Test
    void decrementSeats_by_one() {
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        flightService.decrementSeats(1, 1);

        assertThat(sampleFlight.getAvailableSeats()).isEqualTo(179);
    }

    // ── incrementSeats ────────────────────────────────────────

    @Test
    void incrementSeats_success() {
        sampleFlight.setAvailableSeats(170);
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        flightService.incrementSeats(1, 5);

        assertThat(sampleFlight.getAvailableSeats()).isEqualTo(175);
    }

    @Test
    void incrementSeats_caps_at_total_seats() {
        sampleFlight.setAvailableSeats(178);
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        flightService.incrementSeats(1, 10);

        assertThat(sampleFlight.getAvailableSeats()).isEqualTo(180);
    }

    @Test
    void incrementSeats_flight_not_found_throws() {
        when(flightRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.incrementSeats(99, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flight not found");
    }

    @Test
    void incrementSeats_by_one() {
        sampleFlight.setAvailableSeats(100);
        when(flightRepository.findById(1)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        flightService.incrementSeats(1, 1);

        assertThat(sampleFlight.getAvailableSeats()).isEqualTo(101);
    }

    // ── deleteFlight ──────────────────────────────────────────

    @Test
    void deleteFlight_calls_repository() {
        doNothing().when(flightRepository).deleteById(1);

        flightService.deleteFlight(1);

        verify(flightRepository).deleteById(1);
    }

    // ── getFlightsByAirline ───────────────────────────────────

    @Test
    void getFlightsByAirline_returns_list() {
        when(flightRepository.findByAirlineId(1)).thenReturn(List.of(sampleFlight));

        List<Flight> result = flightService.getFlightsByAirline(1);

        assertThat(result).hasSize(1);
    }

    @Test
    void getFlightsByAirline_no_flights_returns_empty() {
        when(flightRepository.findByAirlineId(99)).thenReturn(Collections.emptyList());

        List<Flight> result = flightService.getFlightsByAirline(99);

        assertThat(result).isEmpty();
    }

    // ── getFlightsByStatus ────────────────────────────────────

    @Test
    void getFlightsByStatus_on_time() {
        when(flightRepository.findByStatus(Flight.FlightStatus.ON_TIME))
                .thenReturn(List.of(sampleFlight));

        List<Flight> result = flightService.getFlightsByStatus("ON_TIME");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Flight.FlightStatus.ON_TIME);
    }

    @Test
    void getFlightsByStatus_delayed() {
        sampleFlight.setStatus(Flight.FlightStatus.DELAYED);
        when(flightRepository.findByStatus(Flight.FlightStatus.DELAYED))
                .thenReturn(List.of(sampleFlight));

        List<Flight> result = flightService.getFlightsByStatus("DELAYED");

        assertThat(result).hasSize(1);
    }

    @Test
    void getFlightsByStatus_invalid_throws() {
        assertThatThrownBy(() -> flightService.getFlightsByStatus("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getFlightsByStatus_cancelled() {
        when(flightRepository.findByStatus(Flight.FlightStatus.CANCELLED))
                .thenReturn(Collections.emptyList());

        List<Flight> result = flightService.getFlightsByStatus("CANCELLED");

        assertThat(result).isEmpty();
    }

    // ── getAllFlights ─────────────────────────────────────────

    @Test
    void getAllFlights_returns_all() {
        Flight f2 = Flight.builder().flightId(2).flightNumber("SG101").build();
        when(flightRepository.findAll()).thenReturn(List.of(sampleFlight, f2));

        List<Flight> result = flightService.getAllFlights();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllFlights_empty() {
        when(flightRepository.findAll()).thenReturn(Collections.emptyList());

        List<Flight> result = flightService.getAllFlights();

        assertThat(result).isEmpty();
    }

    // ── countByAirline ────────────────────────────────────────

    @Test
    void countByAirline_returns_count() {
        when(flightRepository.countByAirlineId(1)).thenReturn(5L);

        assertThat(flightService.countByAirline(1)).isEqualTo(5L);
    }

    @Test
    void countByAirline_zero_for_unknown_airline() {
        when(flightRepository.countByAirlineId(99)).thenReturn(0L);

        assertThat(flightService.countByAirline(99)).isEqualTo(0L);
    }
}