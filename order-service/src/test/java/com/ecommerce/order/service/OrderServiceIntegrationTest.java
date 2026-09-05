package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OutboxEventRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldSaveOrderAndOutboxEvent() {

        CreateOrderRequest request =
                new CreateOrderRequest(
                        101L,
                        1L,
                        2,
                        new BigDecimal("159998")
                );

        Order order = orderService.createOrder(request);

        // Verify Order
        assertNotNull(order.getId());

        Order savedOrder =
                orderRepository.findById(order.getId())
                        .orElseThrow();

        assertEquals(101L, savedOrder.getCustomerId());
        assertEquals(1L, savedOrder.getProductId());
        assertEquals(2, savedOrder.getQuantity());

        // Verify Outbox Event
        List<OutboxEvent> events =
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc("PENDING");

        assertFalse(events.isEmpty());

        OutboxEvent event = events.stream()
                .filter(e ->
                        e.getAggregateId()
                                .equals(order.getId().toString()))
                .findFirst()
                .orElseThrow();

        assertEquals("OrderCreated", event.getEventType());
        assertEquals("Order", event.getAggregateType());
        assertEquals("PENDING", event.getStatus());

        assertTrue(
                event.getPayload().contains(
                        "\"orderId\": " + order.getId()
                )
        );
    }
}