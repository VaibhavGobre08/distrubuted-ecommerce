package com.ecommerce.saga.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventConsumer {

    private final PaymentEventProducer paymentEventProducer;

    public InventoryEventConsumer(
            PaymentEventProducer paymentEventProducer) {
        this.paymentEventProducer = paymentEventProducer;
    }

    @KafkaListener(
            topics = "inventory.reserved",
            groupId = "saga-orchestrator-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Saga received inventory.reserved");
        System.out.println("Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");
        Long customerId = extractLong(message, "customerId");

        paymentEventProducer.processPayment(
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