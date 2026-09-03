package com.ecommerce.payment.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final PaymentEventProducer paymentEventProducer;

    public PaymentEventConsumer(
            PaymentEventProducer paymentEventProducer) {
        this.paymentEventProducer = paymentEventProducer;
    }

    @KafkaListener(
            topics = "payment.process",
            groupId = "payment-service-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Payment Service received event");
        System.out.println("Payment Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");
        Long customerId = extractLong(message, "customerId");

        // For testing compensation flow,
        // intentionally fail the payment.
        boolean paymentSuccessful = false;

        if (paymentSuccessful) {

            paymentEventProducer.publishSuccess(
                    orderId,
                    customerId
            );

        } else {

            paymentEventProducer.publishFailed(
                    orderId,
                    customerId
            );
        }
    }

    private Long extractLong(String json, String field) {

        String value = json
                .replaceAll(".*\"" + field + "\":\\s*", "")
                .split("[,}]")[0]
                .trim();

        return Long.parseLong(value);
    }
}