package com.ecommerce.inventory.service;

import com.ecommerce.inventory.entity.InventoryReservation;
import com.ecommerce.inventory.entity.Product;
import com.ecommerce.inventory.repository.InventoryReservationRepository;
import com.ecommerce.inventory.repository.ProductRepository;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

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

        // Check whether this order was already processed
        Optional<InventoryReservation> existingReservation =
                reservationRepository.findByOrderId(orderId);

        if (existingReservation.isPresent()) {

            InventoryReservation reservation =
                    existingReservation.get();

            System.out.println("=================================");
            System.out.println("Duplicate inventory reservation");
            System.out.println("Order ID: " + orderId);
            System.out.println("Existing status: "
                    + reservation.getStatus());
            System.out.println("=================================");

            return "RESERVED".equals(reservation.getStatus());
        }

        // Find product
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found: " + productId));

        // Check stock
        if (product.getStock() < quantity) {
            return false;
        }

        // Reduce stock
        product.setStock(
                product.getStock() - quantity
        );

        productRepository.save(product);

        // Create reservation
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
    

}