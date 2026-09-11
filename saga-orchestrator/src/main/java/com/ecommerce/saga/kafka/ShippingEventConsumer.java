package com.ecommerce.saga.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ShippingEventConsumer {

    private final SagaEventProducer sagaEventProducer;

    public ShippingEventConsumer(SagaEventProducer sagaEventProducer) {
        this.sagaEventProducer = sagaEventProducer;
    }

    @KafkaListener(
            topics = "shipping.created",
            groupId = "saga-orchestrator-shipping-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Saga Orchestrator received shipping.created");
        System.out.println("Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");

        sagaEventProducer.confirmOrder(orderId);
    }

    private Long extractLong(String json, String field) {

        String value = json
                .replaceAll(".*\"" + field + "\":\\s*", "")
                .split("[,}]")[0]
                .trim();

        return Long.parseLong(value);
    }
}