package com.ecommerce.payment.kafka;

import com.ecommerce.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentEventConsumer(
            PaymentService paymentService,
            PaymentEventProducer paymentEventProducer) {

        this.paymentService = paymentService;
        this.paymentEventProducer = paymentEventProducer;
    }

    @KafkaListener(
            topics = "payment.process",
            groupId = "payment-service-group"
    )
    public void consume(String message) {

        System.out.println("=================================");
        System.out.println("Payment Service received event");
        System.out.println("Payment Event: " + message);
        System.out.println("=================================");

        Long orderId = extractLong(message, "orderId");
        Long customerId = extractLong(message, "customerId");

        boolean successful =
                paymentService.processPayment(
                        orderId,
                        customerId
                );

        if (successful) {

            System.out.println("Payment successful");

            paymentEventProducer.publishSuccess(
                    orderId,
                    customerId
            );

        } else {

            System.out.println("Payment failed");

            paymentEventProducer.publishFailed(
                    orderId,
                    customerId
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
}