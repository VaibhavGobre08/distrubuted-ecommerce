package com.ecommerce.order.event;

import java.math.BigDecimal;

public class OrderCreatedEvent {

    private Long orderId;
    private Long customerId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(
            Long orderId,
            Long customerId,
            Long productId,
            Integer quantity,
            BigDecimal totalAmount) {

        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}