package com.ecommerce.saga.event;

public class InventoryReserveEvent {

    private Long orderId;
    private Long customerId;
    private Long productId;
    private Integer quantity;

    public InventoryReserveEvent() {
    }

    public InventoryReserveEvent(
            Long orderId,
            Long customerId,
            Long productId,
            Integer quantity) {

        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}