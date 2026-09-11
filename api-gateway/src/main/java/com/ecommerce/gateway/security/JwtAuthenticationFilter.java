package com.ecommerce.gateway.security;

import io.jsonwebtoken.Claims;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import reactor.core.publisher.Mono;
import org.springframework.http.HttpMethod;

@Component
public class JwtAuthenticationFilter
        implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        // Auth endpoints are public
        if (path.startsWith("/api/auth")) {
            return chain.filter(exchange);
        }

        String authorizationHeader =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);

        // JWT is missing
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }

        String token =
                authorizationHeader.substring(7);

        try {

        	Claims claims =
        	        jwtService.validateToken(token);

        	String username =
        	        claims.getSubject();

        	String role =
        	        claims.get("role", String.class);

        	System.out.println("=================================");
        	System.out.println("JWT VALID");
        	System.out.println("Username: " + username);
        	System.out.println("Role: " + role);
        	System.out.println("=================================");

        	// Admin-only product operations
        	if (path.startsWith("/api/products")
        	        && (exchange.getRequest().getMethod() == HttpMethod.POST
        	        || exchange.getRequest().getMethod() == HttpMethod.PUT
        	        || exchange.getRequest().getMethod() == HttpMethod.DELETE)) {

        	    if (!"ADMIN".equals(role)) {

        	        exchange.getResponse()
        	                .setStatusCode(HttpStatus.FORBIDDEN);

        	        return exchange.getResponse().setComplete();
        	    }
        	}

        	return chain.filter(exchange);
        } catch (Exception e) {

            System.out.println("Invalid JWT: " + e.getMessage());

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}