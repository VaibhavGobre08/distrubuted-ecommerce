package com.ecommerce.auth_service.dto;

public record UserResponse(
        Long id,
        String username,
        String role
) {
}