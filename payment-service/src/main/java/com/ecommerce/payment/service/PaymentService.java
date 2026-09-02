package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.CreatePaymentRequest;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment createPayment(CreatePaymentRequest request) {

        Payment payment = new Payment();

        payment.setOrderId(request.orderId());
        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());

        // Initially payment is pending
        payment.setStatus(PaymentStatus.PENDING);

        payment.setCreatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment getPayment(Long id) {

        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found: " + id));
    }
}