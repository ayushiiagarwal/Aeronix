package com.aeronix.airline_service.service;

import com.aeronix.airline_service.dto.*;
import com.aeronix.airline_service.entity.Airline;
import com.aeronix.airline_service.entity.Airport;
import com.aeronix.airline_service.repository.AirlineRepository;
import com.aeronix.airline_service.repository.AirportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirlineServiceImplTest {

    @Mock private AirlineRepository airlineRepository;
    @Mock private AirportRepository airportRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private AirlineServiceImpl airlineService;

    private Airline sampleAirline;
    private Airport sampleAirport;
    private AirlineRequest airlineRequest;
    private AirportRequest airportRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(airlineService, "flightServiceUrl", "http://flight-service");

        sampleAirline = Airline.builder()
                .airlineId(1).name("IndiGo").iataCode("6E").icaoCode("IGO")
                .country("India").contactEmail("support@indigo.in")
                .contactPhone("1800123456").website("https://indigo.in")
                .description("Low cost carrier").isActive(true).build();

        sampleAirport = Airport.builder()
                .airportId(1).name("Indira Gandhi International Airport")
                .iataCode("DEL").icaoCode("VIDP").city("New Delhi")
                .country("India").latitude(28.5665).longitude(77.1031)
                .timezone("Asia/Kolkata").terminal("T3").isActive(true).build();

        airlineRequest = new AirlineRequest();
        airlineRequest.setName("IndiGo");
        airlineRequest.setIataCode("6e");
        airlineRequest.setIcaoCode("igo");
        airlineRequest.setCountry("India");
        airlineRequest.setContactEmail("support@indigo.in");

        airportRequest = new AirportRequest();
        airportRequest.setName("Indira Gandhi International Airport");
        airportRequest.setIataCode("del");
        airportRequest.setCity("New Delhi");
        airportRequest.setCountry("India");
        airportRequest.setLatitude(28.5665);
        airportRequest.setLongitude(77.1031);
    }

    // ── createAirline ─────────────────────────────────────────

    @Test
    void createAirline_success() {
        when(airlineRepository.existsByIataCode("6E")).thenReturn(false);
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Airline result = airlineService.createAirline(airlineRequest);

        assertThat(result.getName()).isEqualTo("IndiGo");
        assertThat(result.getIataCode()).isEqualTo("6E");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createAirline_uppercase_iata_code() {
        when(airlineRepository.existsByIataCode("6E")).thenReturn(false);
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Airline result = airlineService.createAirline(airlineRequest);

        assertThat(result.getIataCode()).isEqualTo("6E");
    }

    @Test
    void createAirline_uppercase_icao_code() {
        when(airlineRepository.existsByIataCode("6E")).thenReturn(false);
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Airline result = airlineService.createAirline(airlineRequest);

        assertThat(result.getIcaoCode()).isEqualTo("IGO");
    }

    @Test
    void createAirline_null_icao_stays_null() {
        airlineRequest.setIcaoCode(null);
        when(airlineRepository.existsByIataCode("6E")).thenReturn(false);
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Airline result = airlineService.createAirline(airlineRequest);

        assertThat(result.getIcaoCode()).isNull();
    }

    @Test
    void createAirline_duplicate_iata_throws() {
        when(airlineRepository.existsByIataCode("6E")).thenReturn(true);

        assertThatThrownBy(() -> airlineService.createAirline(airlineRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("IATA code already exists");
    }

    @Test
    void createAirline_sets_active_true() {
        when(airlineRepository.existsByIataCode("6E")).thenReturn(false);
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Airline result = airlineService.createAirline(airlineRequest);

        assertThat(result.isActive()).isTrue();
    }

    // ── getAirlineById ────────────────────────────────────────

    @Test
    void getAirlineById_found() {
        when(airlineRepository.findById(1)).thenReturn(Optional.of(sampleAirline));

        Optional<Airline> result = airlineService.getAirlineById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("IndiGo");
    }

    @Test
    void getAirlineById_not_found() {
        when(airlineRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Airline> result = airlineService.getAirlineById(99);

        assertThat(result).isEmpty();
    }

    // ── getAirlineByIata ──────────────────────────────────────

    @Test
    void getAirlineByIata_found_uppercase() {
        when(airlineRepository.findByIataCode("6E")).thenReturn(Optional.of(sampleAirline));

        Optional<Airline> result = airlineService.getAirlineByIata("6e");

        assertThat(result).isPresent();
        verify(airlineRepository).findByIataCode("6E");
    }

    @Test
    void getAirlineByIata_not_found() {
        when(airlineRepository.findByIataCode("XX")).thenReturn(Optional.empty());

        Optional<Airline> result = airlineService.getAirlineByIata("xx");

        assertThat(result).isEmpty();
    }

    // ── getAllAirlines ────────────────────────────────────────

    @Test
    void getAllAirlines_returns_all() {
        when(airlineRepository.findAll()).thenReturn(List.of(sampleAirline));

        List<Airline> result = airlineService.getAllAirlines();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllAirlines_empty() {
        when(airlineRepository.findAll()).thenReturn(Collections.emptyList());

        List<Airline> result = airlineService.getAllAirlines();

        assertThat(result).isEmpty();
    }

    // ── getActiveAirlines ─────────────────────────────────────

    @Test
    void getActiveAirlines_returns_active_only() {
        when(airlineRepository.findByIsActive(true)).thenReturn(List.of(sampleAirline));

        List<Airline> result = airlineService.getActiveAirlines();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();
    }

    // ── updateAirline ─────────────────────────────────────────

    @Test
    void updateAirline_success() {
        when(airlineRepository.findById(1)).thenReturn(Optional.of(sampleAirline));
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        airlineRequest.setName("IndiGo Updated");
        airlineRequest.setContactEmail("new@indigo.in");

        Airline result = airlineService.updateAirline(1, airlineRequest);

        assertThat(result.getName()).isEqualTo("IndiGo Updated");
        assertThat(result.getContactEmail()).isEqualTo("new@indigo.in");
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateAirline_partial_update_preserves_nulls() {
        when(airlineRepository.findById(1)).thenReturn(Optional.of(sampleAirline));
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AirlineRequest partial = new AirlineRequest();
        partial.setName("New Name");

        Airline result = airlineService.updateAirline(1, partial);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getCountry()).isEqualTo("India");
        assertThat(result.getContactEmail()).isEqualTo("support@indigo.in");
    }

    @Test
    void updateAirline_not_found_throws() {
        when(airlineRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.updateAirline(99, airlineRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Airline not found");
    }

    @Test
    void updateAirline_icao_code_uppercased() {
        when(airlineRepository.findById(1)).thenReturn(Optional.of(sampleAirline));
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        airlineRequest.setIcaoCode("abc");

        Airline result = airlineService.updateAirline(1, airlineRequest);

        assertThat(result.getIcaoCode()).isEqualTo("ABC");
    }

    // ── deactivateAirline ─────────────────────────────────────

    @Test
    void deactivateAirline_success() {
        when(airlineRepository.findById(1)).thenReturn(Optional.of(sampleAirline));
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        airlineService.deactivateAirline(1);

        assertThat(sampleAirline.isActive()).isFalse();
        assertThat(sampleAirline.getUpdatedAt()).isNotNull();
    }

    @Test
    void deactivateAirline_not_found_throws() {
        when(airlineRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.deactivateAirline(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Airline not found");
    }

    // ── activateAirline ───────────────────────────────────────

    @Test
    void activateAirline_success() {
        sampleAirline.setActive(false);
        when(airlineRepository.findById(1)).thenReturn(Optional.of(sampleAirline));
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        airlineService.activateAirline(1);

        assertThat(sampleAirline.isActive()).isTrue();
    }

    @Test
    void activateAirline_not_found_throws() {
        when(airlineRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.activateAirline(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Airline not found");
    }

    // ── deleteAirline ─────────────────────────────────────────

    @Test
    void deleteAirline_calls_repository() {
        doNothing().when(airlineRepository).deleteById(1);

        airlineService.deleteAirline(1);

        verify(airlineRepository).deleteById(1);
    }

    // ── searchAirlines ────────────────────────────────────────

    @Test
    void searchAirlines_returns_results() {
        when(airlineRepository.searchAirlines("indigo")).thenReturn(List.of(sampleAirline));

        List<Airline> result = airlineService.searchAirlines("indigo");

        assertThat(result).hasSize(1);
    }

    @Test
    void searchAirlines_no_results() {
        when(airlineRepository.searchAirlines("xyz")).thenReturn(Collections.emptyList());

        List<Airline> result = airlineService.searchAirlines("xyz");

        assertThat(result).isEmpty();
    }

    // ── getAirlineStats ───────────────────────────────────────

    @Test
    void getAirlineStats_with_flight_count() {
        when(airlineRepository.findById(1)).thenReturn(Optional.of(sampleAirline));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("count", 25));

        AirlineResponse result = airlineService.getAirlineStats(1);

        assertThat(result.getTotalFlights()).isEqualTo(25L);
        assertThat(result.getName()).isEqualTo("IndiGo");
        assertThat(result.getIataCode()).isEqualTo("6E");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void getAirlineStats_flight_service_down_returns_zero_count() {
        when(airlineRepository.findById(1)).thenReturn(Optional.of(sampleAirline));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        AirlineResponse result = airlineService.getAirlineStats(1);

        assertThat(result.getTotalFlights()).isEqualTo(0L);
    }

    @Test
    void getAirlineStats_null_flight_response_returns_zero() {
        when(airlineRepository.findById(1)).thenReturn(Optional.of(sampleAirline));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        AirlineResponse result = airlineService.getAirlineStats(1);

        assertThat(result.getTotalFlights()).isEqualTo(0L);
    }

    @Test
    void getAirlineStats_not_found_throws() {
        when(airlineRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.getAirlineStats(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Airline not found");
    }

    // ── createAirport ─────────────────────────────────────────

    @Test
    void createAirport_success() {
        when(airportRepository.existsByIataCode("DEL")).thenReturn(false);
        when(airportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Airport result = airlineService.createAirport(airportRequest);

        assertThat(result.getName()).isEqualTo("Indira Gandhi International Airport");
        assertThat(result.getIataCode()).isEqualTo("DEL");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createAirport_uppercase_iata() {
        when(airportRepository.existsByIataCode("DEL")).thenReturn(false);
        when(airportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Airport result = airlineService.createAirport(airportRequest);

        assertThat(result.getIataCode()).isEqualTo("DEL");
    }

    @Test
    void createAirport_duplicate_iata_throws() {
        when(airportRepository.existsByIataCode("DEL")).thenReturn(true);

        assertThatThrownBy(() -> airlineService.createAirport(airportRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("IATA code already exists");
    }

    @Test
    void createAirport_null_icao_stays_null() {
        airportRequest.setIcaoCode(null);
        when(airportRepository.existsByIataCode("DEL")).thenReturn(false);
        when(airportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Airport result = airlineService.createAirport(airportRequest);

        assertThat(result.getIcaoCode()).isNull();
    }

    // ── getAirportById ────────────────────────────────────────

    @Test
    void getAirportById_found() {
        when(airportRepository.findById(1)).thenReturn(Optional.of(sampleAirport));

        Optional<Airport> result = airlineService.getAirportById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getIataCode()).isEqualTo("DEL");
    }

    @Test
    void getAirportById_not_found() {
        when(airportRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Airport> result = airlineService.getAirportById(99);

        assertThat(result).isEmpty();
    }

    // ── getAirportByIata ──────────────────────────────────────

    @Test
    void getAirportByIata_found_uppercase() {
        when(airportRepository.findByIataCode("DEL")).thenReturn(Optional.of(sampleAirport));

        Optional<Airport> result = airlineService.getAirportByIata("del");

        assertThat(result).isPresent();
        verify(airportRepository).findByIataCode("DEL");
    }

    @Test
    void getAirportByIata_not_found() {
        when(airportRepository.findByIataCode("ZZZ")).thenReturn(Optional.empty());

        Optional<Airport> result = airlineService.getAirportByIata("ZZZ");

        assertThat(result).isEmpty();
    }

    // ── searchAirports ────────────────────────────────────────

    @Test
    void searchAirports_returns_results() {
        when(airportRepository.searchAirports("delhi")).thenReturn(List.of(sampleAirport));

        List<Airport> result = airlineService.searchAirports("delhi");

        assertThat(result).hasSize(1);
    }

    // ── getAirportsByCity ─────────────────────────────────────

    @Test
    void getAirportsByCity_returns_results() {
        when(airportRepository.findByCityContaining("Delhi")).thenReturn(List.of(sampleAirport));

        List<Airport> result = airlineService.getAirportsByCity("Delhi");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCity()).isEqualTo("New Delhi");
    }

    @Test
    void getAirportsByCity_no_results() {
        when(airportRepository.findByCityContaining("XYZ")).thenReturn(Collections.emptyList());

        List<Airport> result = airlineService.getAirportsByCity("XYZ");

        assertThat(result).isEmpty();
    }

    // ── getAirportsByCountry ──────────────────────────────────

    @Test
    void getAirportsByCountry_returns_results() {
        when(airportRepository.findActiveByCountry("India")).thenReturn(List.of(sampleAirport));

        List<Airport> result = airlineService.getAirportsByCountry("India");

        assertThat(result).hasSize(1);
    }

    // ── getAllAirports ────────────────────────────────────────

    @Test
    void getAllAirports_returns_all() {
        when(airportRepository.findAll()).thenReturn(List.of(sampleAirport));

        List<Airport> result = airlineService.getAllAirports();

        assertThat(result).hasSize(1);
    }

    // ── getActiveAirports ─────────────────────────────────────

    @Test
    void getActiveAirports_returns_active_only() {
        when(airportRepository.findByIsActive(true)).thenReturn(List.of(sampleAirport));

        List<Airport> result = airlineService.getActiveAirports();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();
    }

    // ── updateAirport ─────────────────────────────────────────

    @Test
    void updateAirport_success() {
        when(airportRepository.findById(1)).thenReturn(Optional.of(sampleAirport));
        when(airportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        airportRequest.setName("Updated Airport");
        airportRequest.setCity("Delhi NCR");

        Airport result = airlineService.updateAirport(1, airportRequest);

        assertThat(result.getName()).isEqualTo("Updated Airport");
        assertThat(result.getCity()).isEqualTo("Delhi NCR");
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateAirport_partial_update_preserves_nulls() {
        when(airportRepository.findById(1)).thenReturn(Optional.of(sampleAirport));
        when(airportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AirportRequest partial = new AirportRequest();
        partial.setName("New Name");

        Airport result = airlineService.updateAirport(1, partial);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getCity()).isEqualTo("New Delhi");
        assertThat(result.getCountry()).isEqualTo("India");
    }

    @Test
    void updateAirport_not_found_throws() {
        when(airportRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.updateAirport(99, airportRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Airport not found");
    }

    @Test
    void updateAirport_icao_code_uppercased() {
        when(airportRepository.findById(1)).thenReturn(Optional.of(sampleAirport));
        when(airportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        airportRequest.setIcaoCode("vidp");

        Airport result = airlineService.updateAirport(1, airportRequest);

        assertThat(result.getIcaoCode()).isEqualTo("VIDP");
    }

    // ── deactivateAirport ─────────────────────────────────────

    @Test
    void deactivateAirport_success() {
        when(airportRepository.findById(1)).thenReturn(Optional.of(sampleAirport));
        when(airportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        airlineService.deactivateAirport(1);

        assertThat(sampleAirport.isActive()).isFalse();
        assertThat(sampleAirport.getUpdatedAt()).isNotNull();
    }

    @Test
    void deactivateAirport_not_found_throws() {
        when(airportRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.deactivateAirport(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Airport not found");
    }

    // ── activateAirport ───────────────────────────────────────

    @Test
    void activateAirport_success() {
        sampleAirport.setActive(false);
        when(airportRepository.findById(1)).thenReturn(Optional.of(sampleAirport));
        when(airportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        airlineService.activateAirport(1);

        assertThat(sampleAirport.isActive()).isTrue();
    }

    @Test
    void activateAirport_not_found_throws() {
        when(airportRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.activateAirport(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Airport not found");
    }

    // ── searchAirportsForAutocomplete ─────────────────────────

    @Test
    void autocomplete_returns_formatted_results() {
        when(airportRepository.searchAirports("del")).thenReturn(List.of(sampleAirport));

        List<AirportSearchResponse> result =
                airlineService.searchAirportsForAutocomplete("del");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIataCode()).isEqualTo("DEL");
        assertThat(result.get(0).getDisplayName())
                .contains("Indira Gandhi International Airport")
                .contains("DEL")
                .contains("New Delhi")
                .contains("India");
    }

    @Test
    void autocomplete_query_less_than_2_chars_returns_empty() {
        List<AirportSearchResponse> result =
                airlineService.searchAirportsForAutocomplete("d");

        assertThat(result).isEmpty();
        verify(airportRepository, never()).searchAirports(any());
    }

    @Test
    void autocomplete_null_query_returns_empty() {
        List<AirportSearchResponse> result =
                airlineService.searchAirportsForAutocomplete(null);

        assertThat(result).isEmpty();
    }

    @Test
    void autocomplete_filters_inactive_airports() {
        sampleAirport.setActive(false);
        Airport activeAirport = Airport.builder()
                .airportId(2).name("Chhatrapati Shivaji International")
                .iataCode("BOM").city("Mumbai").country("India")
                .isActive(true).build();

        when(airportRepository.searchAirports("del"))
                .thenReturn(List.of(sampleAirport, activeAirport));

        List<AirportSearchResponse> result =
                airlineService.searchAirportsForAutocomplete("del");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIataCode()).isEqualTo("BOM");
    }

    @Test
    void autocomplete_limits_to_10_results() {
        List<Airport> manyAirports = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            manyAirports.add(Airport.builder()
                    .airportId(i + 1).name("Airport " + i)
                    .iataCode("A" + String.format("%02d", i))
                    .city("City").country("India").isActive(true).build());
        }
        when(airportRepository.searchAirports("air")).thenReturn(manyAirports);

        List<AirportSearchResponse> result =
                airlineService.searchAirportsForAutocomplete("air");

        assertThat(result).hasSize(10);
    }

    @Test
    void autocomplete_trims_whitespace_in_query() {
        when(airportRepository.searchAirports("del")).thenReturn(List.of(sampleAirport));

        List<AirportSearchResponse> result =
                airlineService.searchAirportsForAutocomplete("  del  ");

        assertThat(result).hasSize(1);
        verify(airportRepository).searchAirports("del");
    }
}