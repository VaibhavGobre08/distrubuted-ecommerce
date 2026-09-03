package com.ecommerce.saga.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.saga.event.InventoryReserveEvent;
import com.ecommerce.saga.kafka.InventoryEventProducer;

@Component
public class OrderEventConsumer {

    private final InventoryEventProducer inventoryEventProducer;

    public OrderEventConsumer(
            InventoryEventProducer inventoryEventProducer) {

        this.inventoryEventProducer = inventoryEventProducer;
    }

    @KafkaListener(
            topics = "order.created",
            groupId = "saga-orchestrator-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Saga Orchestrator received event");
        System.out.println("Order Event: " + message);
        System.out.println("=================================");

        // Temporary parsing
        Long orderId = extractLong(message, "orderId");
        Long customerId = extractLong(message, "customerId");
        Long productId = extractLong(message, "productId");
        Integer quantity = extractInt(message, "quantity");

        InventoryReserveEvent event =
                new InventoryReserveEvent(
                        orderId,
                        customerId,
                        productId,
                        quantity
                );

        inventoryEventProducer.reserveInventory(event);
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