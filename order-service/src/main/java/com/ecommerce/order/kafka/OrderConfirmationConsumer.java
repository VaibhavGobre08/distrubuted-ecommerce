package com.ecommerce.order.kafka;

import com.ecommerce.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConfirmationConsumer {

    private final OrderService orderService;

    public OrderConfirmationConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = "order.confirm",
            groupId = "order-service-confirm-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Order Service received order.confirm");
        System.out.println("Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");

        orderService.confirmOrder(orderId);
    }

    private Long extractLong(String json, String field) {

        String value = json
                .replaceAll(".*\"" + field + "\":\\s*", "")
                .split("[,}]")[0]
                .trim();

        return Long.parseLong(value);
    }
}