package com.ecommerce.saga.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final SagaEventProducer sagaEventProducer;

    public PaymentEventConsumer(
            SagaEventProducer sagaEventProducer) {
        this.sagaEventProducer = sagaEventProducer;
    }

    @KafkaListener(
            topics = "payment.failed",
            groupId = "saga-orchestrator-payment-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Saga Orchestrator received payment.failed");
        System.out.println("Payment Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");
        Long customerId = extractLong(message, "customerId");

        // Start compensation
        sagaEventProducer.releaseInventory(
                orderId,
                customerId
        );
    }

    private Long extractLong(String json, String field) {

        String value = json
                .replaceAll(".*\"" + field + "\":\\s*", "")
                .split("[,}]")[0]
                .trim();

        return Long.parseLong(value);
    }
}