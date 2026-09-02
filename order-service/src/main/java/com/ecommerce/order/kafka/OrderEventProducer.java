package com.ecommerce.order.kafka;

import com.ecommerce.order.event.OrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private static final String TOPIC = "order.created";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {

    	String message = """
    	        {
    	            "orderId": %d,
    	            "customerId": %d,
    	            "productId": %d,
    	            "quantity": %d,
    	            "totalAmount": %s
    	        }
    	        """.formatted(
    	        event.getOrderId(),
    	        event.getCustomerId(),
    	        event.getProductId(),
    	        event.getQuantity(),
    	        event.getTotalAmount()
    	);

        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), message);

        System.out.println("Published order.created event: " + message);
    }
}