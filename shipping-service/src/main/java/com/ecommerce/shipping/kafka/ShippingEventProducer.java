package com.ecommerce.shipping.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShippingEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ShippingEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCreated(Long orderId, Long customerId) {

        String message = """
                {
                    "orderId": %d,
                    "customerId": %d
                }
                """.formatted(
                orderId,
                customerId
        );

        kafkaTemplate.send(
                "shipping.created",
                orderId.toString(),
                message
        );

        System.out.println("=================================");
        System.out.println("Published shipping.created");
        System.out.println("Event: " + message);
        System.out.println("=================================");
    }
}