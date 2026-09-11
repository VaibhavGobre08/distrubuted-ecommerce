package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryReleaseConsumer {

    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;

    public InventoryReleaseConsumer(
            InventoryService inventoryService,
            InventoryEventProducer inventoryEventProducer) {

        this.inventoryService = inventoryService;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    @KafkaListener(
            topics = "inventory.release",
            groupId = "inventory-release-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Inventory Service received inventory.release");
        System.out.println("Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");

        boolean released =
                inventoryService.releaseStock(orderId);

        if (released) {
            inventoryEventProducer.publishReleased(orderId);
        }
    }

    private Long extractLong(String json, String field) {

        String value = json
                .replaceAll(".*\"" + field + "\":\\s*", "")
                .split("[,}]")[0]
                .trim();

        return Long.parseLong(value);
    }
}