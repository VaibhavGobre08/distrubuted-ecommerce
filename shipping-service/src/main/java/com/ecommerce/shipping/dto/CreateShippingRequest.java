package com.ecommerce.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateShippingRequest(

        @NotNull
        Long orderId,

        @NotNull
        Long customerId,

        @NotBlank
        String address

) {
}