package com.ecommerce.shipping.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ShippingEventConsumer {

    private final ShippingEventProducer shippingEventProducer;

    public ShippingEventConsumer(
            ShippingEventProducer shippingEventProducer) {

        this.shippingEventProducer = shippingEventProducer;
    }

    @KafkaListener(
            topics = "shipping.create",
            groupId = "shipping-service-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Shipping Service received event");
        System.out.println("Shipping Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");
        Long customerId = extractLong(message, "customerId");

        System.out.println("Creating shipment...");
        System.out.println("Order ID: " + orderId);
        System.out.println("Customer ID: " + customerId);

        shippingEventProducer.publishCreated(
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