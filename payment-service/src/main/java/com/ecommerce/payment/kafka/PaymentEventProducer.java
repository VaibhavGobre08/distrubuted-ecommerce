package com.ecommerce.payment.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentEventProducer(
            KafkaTemplate<String, String> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSuccess(
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
                "payment.success",
                orderId.toString(),
                message
        );

        System.out.println("=================================");
        System.out.println("Published payment.success");
        System.out.println("Event: " + message);
        System.out.println("=================================");
    }

    public void publishFailed(
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
                "payment.failed",
                orderId.toString(),
                message
        );

        System.out.println("=================================");
        System.out.println("Published payment.failed");
        System.out.println("Event: " + message);
        System.out.println("=================================");
    }
}