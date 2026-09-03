package com.ecommerce.payment.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    private final PaymentService paymentService = new PaymentService();

    @Test
    void shouldProcessPaymentSuccessfully() {

        // When
        boolean result =
                paymentService.processPayment(1L, 101L);

        // Then
        assertTrue(result);
    }
}