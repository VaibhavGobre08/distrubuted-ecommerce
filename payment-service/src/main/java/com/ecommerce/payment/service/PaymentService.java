package com.ecommerce.payment.service;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public boolean processPayment(
            Long orderId,
            Long customerId) {

        System.out.println("Processing payment...");
        System.out.println("Order ID: " + orderId);
        System.out.println("Customer ID: " + customerId);

        // For now, payment always succeeds
        return true;
    }
}