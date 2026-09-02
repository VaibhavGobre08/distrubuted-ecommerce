package com.ecommerce.inventory.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventConsumer {

    @KafkaListener(
            topics = "inventory.reserve",
            groupId = "inventory-service-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Inventory Service received event");
        System.out.println("Inventory Event: " + message);
        System.out.println("=================================");
    }
}