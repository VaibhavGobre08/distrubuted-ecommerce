package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.exception.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;

    public OrderService(
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository) {

        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {

        // 1. Create Order
        Order order = new Order();

        order.setCustomerId(request.customerId());
        order.setProductId(request.productId());
        order.setQuantity(request.quantity());
        order.setTotalAmount(request.totalAmount());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        // 2. Save Order
        Order savedOrder = orderRepository.save(order);

        // 3. Create Kafka event payload
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalAmount()
        );

        String payload = """
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

        // 4. Save event in Outbox table
        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setEventType("OrderCreated");
        outboxEvent.setAggregateType("Order");
        outboxEvent.setAggregateId(
                savedOrder.getId().toString()
        );
        outboxEvent.setPayload(payload);
        outboxEvent.setStatus("PENDING");
        outboxEvent.setRetryCount(0);
        outboxEvent.setNextRetryAt(LocalDateTime.now());
        outboxEvent.setCreatedAt(LocalDateTime.now());

        outboxEventRepository.save(outboxEvent);

        System.out.println("=================================");
        System.out.println("Order + Outbox Event saved");
        System.out.println("Order ID: " + savedOrder.getId());
        System.out.println("Outbox Event: " + payload);
        System.out.println("=================================");

        return savedOrder;
    }

    public Order getOrder(Long id) {

        return orderRepository.findById(id)
                .orElseThrow(() ->
                new ResourceNotFoundException(
                        "Order not found: " + id
                ));
    }

    public void confirmOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                new ResourceNotFoundException(
                        "Order not found: " + orderId
                ));

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    public void cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                new ResourceNotFoundException(
                        "Order not found: " + orderId
                ));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}