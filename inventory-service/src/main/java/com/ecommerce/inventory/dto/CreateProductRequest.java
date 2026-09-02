package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank
        String name,

        @NotNull
        @Positive
        BigDecimal price,

        @NotNull
        @PositiveOrZero
        Integer stock
) {
}