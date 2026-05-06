package com.aeronix.airline_service.data;

import com.aeronix.airline_service.entity.*;
import com.aeronix.airline_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;

    @Override
    public void run(String... args) {
        seedAirlines();
        seedAirports();
    }

    private void seedAirlines() {
        if (airlineRepository.count() > 0) return;

        airlineRepository.save(Airline.builder()
                .name("IndiGo").iataCode("6E").icaoCode("IGO")
                .country("India").contactEmail("support@goindigo.in")
                .website("https://www.goindigo.in").isActive(true).build());

        airlineRepository.save(Airline.builder()
                .name("Air India").iataCode("AI").icaoCode("AIC")
                .country("India").contactEmail("care@airindia.in")
                .website("https://www.airindia.in").isActive(true).build());

        airlineRepository.save(Airline.builder()
                .name("SpiceJet").iataCode("SG").icaoCode("SEJ")
                .country("India").contactEmail("customercare@spicejet.com")
                .website("https://www.spicejet.com").isActive(true).build());

        airlineRepository.save(Airline.builder()
                .name("Vistara").iataCode("UK").icaoCode("VTI")
                .country("India").contactEmail("customercare@airvistara.com")
                .website("https://www.airvistara.com").isActive(true).build());

        airlineRepository.save(Airline.builder()
                .name("Air Arabia").iataCode("G9").icaoCode("ABY")
                .country("UAE").contactEmail("info@airarabia.com")
                .website("https://www.airarabia.com").isActive(true).build());

        log.info("Airlines seeded successfully");
    }

    private void seedAirports() {
        if (airportRepository.count() > 0) return;

        airportRepository.save(Airport.builder()
                .name("Indira Gandhi International Airport")
                .iataCode("DEL").icaoCode("VIDP")
                .city("New Delhi").country("India")
                .latitude(28.5561).longitude(77.1000)
                .timezone("Asia/Kolkata").isActive(true).build());

        airportRepository.save(Airport.builder()
                .name("Chhatrapati Shivaji Maharaj International Airport")
                .iataCode("BOM").icaoCode("VABB")
                .city("Mumbai").country("India")
                .latitude(19.0896).longitude(72.8656)
                .timezone("Asia/Kolkata").isActive(true).build());

        airportRepository.save(Airport.builder()
                .name("Kempegowda International Airport")
                .iataCode("BLR").icaoCode("VOBL")
                .city("Bengaluru").country("India")
                .latitude(13.1986).longitude(77.7066)
                .timezone("Asia/Kolkata").isActive(true).build());

        airportRepository.save(Airport.builder()
                .name("Chennai International Airport")
                .iataCode("MAA").icaoCode("VOMM")
                .city("Chennai").country("India")
                .latitude(12.9900).longitude(80.1693)
                .timezone("Asia/Kolkata").isActive(true).build());

        airportRepository.save(Airport.builder()
                .name("Netaji Subhas Chandra Bose International Airport")
                .iataCode("CCU").icaoCode("VECC")
                .city("Kolkata").country("India")
                .latitude(22.6547).longitude(88.4467)
                .timezone("Asia/Kolkata").isActive(true).build());

        airportRepository.save(Airport.builder()
                .name("Rajiv Gandhi International Airport")
                .iataCode("HYD").icaoCode("VOHS")
                .city("Hyderabad").country("India")
                .latitude(17.2403).longitude(78.4294)
                .timezone("Asia/Kolkata").isActive(true).build());

        airportRepository.save(Airport.builder()
                .name("Sardar Vallabhbhai Patel International Airport")
                .iataCode("AMD").icaoCode("VAAH")
                .city("Ahmedabad").country("India")
                .latitude(23.0772).longitude(72.6347)
                .timezone("Asia/Kolkata").isActive(true).build());

        airportRepository.save(Airport.builder()
                .name("Cochin International Airport")
                .iataCode("COK").icaoCode("VOCI")
                .city("Kochi").country("India")
                .latitude(10.1520).longitude(76.4019)
                .timezone("Asia/Kolkata").isActive(true).build());

        airportRepository.save(Airport.builder()
                .name("Dubai International Airport")
                .iataCode("DXB").icaoCode("OMDB")
                .city("Dubai").country("UAE")
                .latitude(25.2532).longitude(55.3657)
                .timezone("Asia/Dubai").isActive(true).build());

        airportRepository.save(Airport.builder()
                .name("Heathrow Airport")
                .iataCode("LHR").icaoCode("EGLL")
                .city("London").country("United Kingdom")
                .latitude(51.4775).longitude(-0.4614)
                .timezone("Europe/London").isActive(true).build());

        log.info("Airports seeded successfully");
    }
}