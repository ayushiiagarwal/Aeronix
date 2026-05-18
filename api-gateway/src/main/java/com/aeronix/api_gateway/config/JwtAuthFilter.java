package com.aeronix.api_gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/flights/search",
            "/api/flights/round-trip",
            "/api/bookings/pnr",
            "/api/airlines/search",
            "/api/airlines/iata/",
            "/api/airports/search",
            "/api/airports/iata/",
            "/api/airports/city/",
            "/api/airports/country/",
            "/swagger-ui",
            "/v3/api-docs",
            "/auth-service/v3/api-docs",
            "/flight-service/v3/api-docs",
            "/seat-service/v3/api-docs",
            "/booking-service/v3/api-docs",
            "/passenger-service/v3/api-docs",
            "/payment-service/v3/api-docs",
            "/notification-service/v3/api-docs",
            "/airline-service/v3/api-docs"
    );

    private static final Set<String> PUBLIC_EXACT = Set.of(
            "/api/airlines",
            "/api/airports"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        if(HttpMethod.OPTIONS.matches(method)) return chain.filter(exchange);

        boolean isPublicExact = "GET".equals(method) && PUBLIC_EXACT.contains(path);
        boolean isPublicPrefix = PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);

        if (isPublicExact || isPublicPrefix) return chain.filter(exchange);

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();

            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.headers(h -> {
                        h.add("X-User-Id", claims.getSubject());
                        h.add("X-User-Role", claims.get("role", String.class));
                        h.add("X-User-Email", claims.get("email", String.class));
                    }))
                    .build();
            return chain.filter(mutated);
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}