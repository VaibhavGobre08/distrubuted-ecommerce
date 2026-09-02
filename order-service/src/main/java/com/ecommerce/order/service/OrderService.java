package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder(CreateOrderRequest request) {

    	Order order = new Order();
    	order.setCustomerId(request.customerId());
    	order.setTotalAmount(request.totalAmount());
    	order.setStatus(OrderStatus.PENDING);
    	order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }
}