package com.ecommerce.saga.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SagaEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public SagaEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void confirmOrder(Long orderId) {

        String message = """
                {
                    "orderId": %d
                }
                """.formatted(orderId);

        kafkaTemplate.send(
                "order.confirm",
                orderId.toString(),
                message
        );

        System.out.println("=================================");
        System.out.println("Published order.confirm");
        System.out.println("Event: " + message);
        System.out.println("=================================");
    }
}