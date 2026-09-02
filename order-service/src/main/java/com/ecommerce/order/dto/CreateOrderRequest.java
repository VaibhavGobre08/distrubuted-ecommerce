package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(

        @NotNull
        Long customerId,

        @NotNull
        @Positive
        BigDecimal totalAmount
) {
}