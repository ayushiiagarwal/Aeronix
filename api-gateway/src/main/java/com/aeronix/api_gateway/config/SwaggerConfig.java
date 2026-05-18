package com.aeronix.api_gateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class SwaggerConfig {

    @Bean
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();

        Set<SwaggerUrl> urls = new HashSet<>();

        urls.add(createSwaggerUrl("Auth Service",        "/auth-service/v3/api-docs"));
        urls.add(createSwaggerUrl("Flight Service",      "/flight-service/v3/api-docs"));
        urls.add(createSwaggerUrl("Seat Service",        "/seat-service/v3/api-docs"));
        urls.add(createSwaggerUrl("Booking Service",     "/booking-service/v3/api-docs"));
        urls.add(createSwaggerUrl("Passenger Service",   "/passenger-service/v3/api-docs"));
        urls.add(createSwaggerUrl("Payment Service",     "/payment-service/v3/api-docs"));
        urls.add(createSwaggerUrl("Notification Service","/notification-service/v3/api-docs"));
        urls.add(createSwaggerUrl("Airline Service",     "/airline-service/v3/api-docs"));

        config.setUrls(urls);
        return config;
    }

    private SwaggerUrl createSwaggerUrl(String name, String url) {
        SwaggerUrl swaggerUrl = new SwaggerUrl();
        swaggerUrl.setName(name);
        swaggerUrl.setUrl(url);
        return swaggerUrl;
    }
}
