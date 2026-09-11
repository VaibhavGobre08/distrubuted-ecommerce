package com.ecommerce.notification.kafka;

import com.ecommerce.notification.service.NotificationService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelledConsumer {

    private final NotificationService notificationService;

    public OrderCancelledConsumer(
            NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "order.cancel",
            groupId = "notification-cancel-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Notification Service received");
        System.out.println("Topic: order.cancel");
        System.out.println("Message: " + message);
        System.out.println("=================================");

        Long orderId =
                extractLong(message, "orderId");

        notificationService.createNotification(
                orderId,
                "customer",
                "Your order " + orderId
                        + " has been cancelled."
        );
    }

    private Long extractLong(
            String message,
            String field) {

        String search =
                "\"" + field + "\":";

        int start =
                message.indexOf(search)
                        + search.length();

        int end =
                message.indexOf(",", start);

        if (end == -1) {
            end = message.indexOf("}", start);
        }

        return Long.parseLong(
                message.substring(start, end).trim()
        );
    }
}