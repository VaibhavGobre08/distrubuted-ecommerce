package com.ecommerce.saga.kafka;

import com.ecommerce.saga.event.InventoryReserveEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventProducer {

    private static final String TOPIC = "inventory.reserve";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public InventoryEventProducer(
            KafkaTemplate<String, String> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void reserveInventory(InventoryReserveEvent event) {

        String message = """
                {
                    "orderId": %d,
                    "customerId": %d,
                    "productId": %d,
                    "quantity": %d
                }
                """.formatted(
                event.getOrderId(),
                event.getCustomerId(),
                event.getProductId(),
                event.getQuantity()
        );

        kafkaTemplate.send(
                TOPIC,
                event.getOrderId().toString(),
                message
        );

        System.out.println("=================================");
        System.out.println("Published inventory.reserve event");
        System.out.println("Inventory Event: " + message);
        System.out.println("=================================");
    }
}