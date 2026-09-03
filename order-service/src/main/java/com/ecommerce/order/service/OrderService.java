package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.kafka.OrderEventProducer;
import com.ecommerce.order.repository.OrderRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    public OrderService(
            OrderRepository orderRepository,
            OrderEventProducer orderEventProducer) {

        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
    }

    public Order createOrder(CreateOrderRequest request) {

        // 1. Create order
        Order order = new Order();

        order.setCustomerId(request.customerId());
        order.setTotalAmount(request.totalAmount());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setProductId(request.productId());
        order.setQuantity(request.quantity());

        // 2. Save order
        Order savedOrder = orderRepository.save(order);

        // 3. Create Kafka event
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalAmount()
        );

        // 4. Publish event
        orderEventProducer.publishOrderCreated(event);

        return savedOrder;
    }

    public Order getOrder(Long id) {

        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Order not found: " + id));
    }
    
    public void confirmOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found: " + orderId));

        order.setStatus(OrderStatus.CONFIRMED);

        orderRepository.save(order);

        System.out.println("=================================");
        System.out.println("Order confirmed");
        System.out.println("Order ID: " + orderId);
        System.out.println("=================================");
    }
    
    public void cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found: " + orderId));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        System.out.println("=================================");
        System.out.println("Order cancelled");
        System.out.println("Order ID: " + orderId);
        System.out.println("=================================");
    }
}