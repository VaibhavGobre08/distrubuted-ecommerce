package com.ecommerce.order.kafka;

import com.ecommerce.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCancellationConsumer {

    private final OrderService orderService;

    public OrderCancellationConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = "order.cancel",
            groupId = "order-service-cancel-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Order Service received order.cancel");
        System.out.println("Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");

        orderService.cancelOrder(orderId);
    }

    private Long extractLong(String json, String field) {

        String value = json
                .replaceAll(".*\"" + field + "\":\\s*", "")
                .split("[,}]")[0]
                .trim();

        return Long.parseLong(value);
    }
}