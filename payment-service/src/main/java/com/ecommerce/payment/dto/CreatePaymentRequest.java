package com.ecommerce.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotNull
        Long orderId,

        @NotNull
        Long customerId,

        @NotNull
        @Positive
        BigDecimal amount

) {
}