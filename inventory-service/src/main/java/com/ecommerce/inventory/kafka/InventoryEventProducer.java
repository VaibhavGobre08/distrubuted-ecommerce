package com.ecommerce.inventory.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public InventoryEventProducer(
            KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishReserved(
            Long orderId,
            Long customerId,
            Long productId,
            Integer quantity) {

        String message = """
                {
                    "orderId": %d,
                    "customerId": %d,
                    "productId": %d,
                    "quantity": %d
                }
                """.formatted(
                orderId,
                customerId,
                productId,
                quantity
        );

        kafkaTemplate.send(
                "inventory.reserved",
                orderId.toString(),
                message
        );

        System.out.println("=================================");
        System.out.println("Published inventory.reserved");
        System.out.println("Event: " + message);
        System.out.println("=================================");
    }

    public void publishFailed(
            Long orderId,
            Long customerId,
            Long productId,
            Integer quantity) {

        String message = """
                {
                    "orderId": %d,
                    "customerId": %d,
                    "productId": %d,
                    "quantity": %d
                }
                """.formatted(
                orderId,
                customerId,
                productId,
                quantity
        );

        kafkaTemplate.send(
                "inventory.failed",
                orderId.toString(),
                message
        );

        System.out.println("Published inventory.failed");
    }
    
    public void publishReleased(Long orderId) {

        String message = """
                {
                    "orderId": %d
                }
                """.formatted(orderId);

        kafkaTemplate.send(
                "inventory.released",
                orderId.toString(),
                message
        );

        System.out.println("=================================");
        System.out.println("Published inventory.released");
        System.out.println("Event: " + message);
        System.out.println("=================================");
    }
}