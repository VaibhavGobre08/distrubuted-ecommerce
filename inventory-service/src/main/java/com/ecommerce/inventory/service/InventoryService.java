package com.ecommerce.inventory.service;

import com.ecommerce.inventory.entity.InventoryReservation;
import com.ecommerce.inventory.entity.Product;
import com.ecommerce.inventory.repository.InventoryReservationRepository;
import com.ecommerce.inventory.repository.ProductRepository;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryReservationRepository reservationRepository;

//    public InventoryService(ProductRepository productRepository) {
//        this.productRepository = productRepository;
//    }
    
    public InventoryService(
            ProductRepository productRepository,
            InventoryReservationRepository reservationRepository) {

        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public boolean reserveStock(
            Long orderId,
            Long productId,
            Integer quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found: " + productId));

        if (product.getStock() < quantity) {
            return false;
        }

        product.setStock(
                product.getStock() - quantity
        );

        productRepository.save(product);

        InventoryReservation reservation =
                new InventoryReservation();

        reservation.setOrderId(orderId);
        reservation.setProductId(productId);
        reservation.setQuantity(quantity);
        reservation.setStatus("RESERVED");
        reservation.setCreatedAt(LocalDateTime.now());

        reservationRepository.save(reservation);

        return true;
    }
    
    @Transactional
    public boolean releaseStock(Long orderId) {

        InventoryReservation reservation =
                reservationRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found for order: " + orderId));

        // Idempotency:
        // If compensation is received twice, don't restore stock twice.
        if ("RELEASED".equals(reservation.getStatus())) {
            return true;
        }

        // Only RESERVED stock can be released.
        if (!"RESERVED".equals(reservation.getStatus())) {
            return false;
        }

        Product product = productRepository.findById(
                reservation.getProductId()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Product not found: " + reservation.getProductId()));

        // Restore the reserved quantity
        product.setStock(
                product.getStock() + reservation.getQuantity()
        );

        productRepository.save(product);

        // Mark reservation as released
        reservation.setStatus("RELEASED");
        reservationRepository.save(reservation);

        System.out.println("=================================");
        System.out.println("Inventory compensation completed");
        System.out.println("Order ID: " + orderId);
        System.out.println("Product ID: " + reservation.getProductId());
        System.out.println("Quantity restored: " + reservation.getQuantity());
        System.out.println("=================================");

        return true;
    }
}