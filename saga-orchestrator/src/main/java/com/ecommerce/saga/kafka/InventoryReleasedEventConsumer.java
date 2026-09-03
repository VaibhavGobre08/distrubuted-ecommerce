package com.ecommerce.saga.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryReleasedEventConsumer {

    private final SagaEventProducer sagaEventProducer;

    public InventoryReleasedEventConsumer(
            SagaEventProducer sagaEventProducer) {
        this.sagaEventProducer = sagaEventProducer;
    }

    @KafkaListener(
            topics = "inventory.released",
            groupId = "saga-orchestrator-release-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Saga received inventory.released");
        System.out.println("Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");

        sagaEventProducer.cancelOrder(orderId);
    }

    private Long extractLong(String json, String field) {

        String value = json
                .replaceAll(".*\"" + field + "\":\\s*", "")
                .split("[,}]")[0]
                .trim();

        return Long.parseLong(value);
    }
}