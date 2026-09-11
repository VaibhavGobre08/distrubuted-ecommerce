package com.ecommerce.auth_service.dto;

public record LoginResponse(
        String token,
        String username,
        String role
) {
}