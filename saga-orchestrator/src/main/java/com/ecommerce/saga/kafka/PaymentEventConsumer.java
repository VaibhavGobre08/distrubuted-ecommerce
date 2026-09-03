package com.ecommerce.saga.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final ShippingEventProducer shippingEventProducer;

    public PaymentEventConsumer(
            ShippingEventProducer shippingEventProducer) {
        this.shippingEventProducer = shippingEventProducer;
    }

    @KafkaListener(
            topics = "payment.success",
            groupId = "saga-orchestrator-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Saga received payment.success");
        System.out.println("Payment Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");
        Long customerId = extractLong(message, "customerId");

        shippingEventProducer.createShipment(
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