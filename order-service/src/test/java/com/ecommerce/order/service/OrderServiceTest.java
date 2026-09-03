package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.kafka.OrderEventProducer;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrderSuccessfully() {

        CreateOrderRequest request =
                new CreateOrderRequest(
                        101L,
                        1L,
                        2,
                        new BigDecimal("1500")
                );

        Order savedOrder = new Order();

        savedOrder.setId(1L);
        savedOrder.setCustomerId(101L);
        savedOrder.setProductId(1L);
        savedOrder.setQuantity(2);
        savedOrder.setTotalAmount(new BigDecimal("1500"));
        savedOrder.setStatus(OrderStatus.PENDING);

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        Order result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(101L, result.getCustomerId());
        assertEquals(1L, result.getProductId());
        assertEquals(2, result.getQuantity());
        assertEquals(OrderStatus.PENDING, result.getStatus());

        verify(orderRepository, times(1))
                .save(any(Order.class));

        verify(orderEventProducer, times(1))
                .publishOrderCreated(any(OrderCreatedEvent.class));
    }
}