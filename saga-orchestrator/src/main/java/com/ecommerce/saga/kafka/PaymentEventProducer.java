package com.ecommerce.saga.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentEventProducer(
            KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void processPayment(
            Long orderId,
            Long customerId) {

        String message = """
                {
                    "orderId": %d,
                    "customerId": %d
                }
                """.formatted(
                orderId,
                customerId
        );

        kafkaTemplate.send(
                "payment.process",
                orderId.toString(),
                message
        );

        System.out.println("=================================");
        System.out.println("Published payment.process");
        System.out.println("Event: " + message);
        System.out.println("=================================");
    }
}