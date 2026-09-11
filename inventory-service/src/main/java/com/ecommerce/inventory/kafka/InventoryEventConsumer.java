package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;

    public InventoryEventConsumer(
            InventoryService inventoryService,
            InventoryEventProducer inventoryEventProducer) {

        this.inventoryService = inventoryService;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    @KafkaListener(
            topics = "inventory.reserve",
            groupId = "inventory-service-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Inventory Service received event");
        System.out.println("Inventory Event: " + message);
        System.out.println("=================================");

        Long productId = extractLong(message, "productId");
        Integer quantity = extractInt(message, "quantity");

        boolean reserved =
                inventoryService.reserveStock(
                        productId,
                        quantity
                );

        if (reserved) {

            System.out.println("Stock reserved successfully");

            inventoryEventProducer.publishReserved(
                    extractLong(message, "orderId"),
                    extractLong(message, "customerId"),
                    productId,
                    quantity
            );

        } else {

            System.out.println("Stock reservation failed");

            inventoryEventProducer.publishFailed(
                    extractLong(message, "orderId"),
                    extractLong(message, "customerId"),
                    productId,
                    quantity
            );
        }
    }

    private Long extractLong(String json, String field) {

        String value = json
                .replaceAll(".*\"" + field + "\":\\s*", "")
                .split("[,}]")[0]
                .trim();

        return Long.parseLong(value);
    }

    private Integer extractInt(String json, String field) {

        String value = json
                .replaceAll(".*\"" + field + "\":\\s*", "")
                .split("[,}]")[0]
                .trim();

        return Integer.parseInt(value);
    }
}