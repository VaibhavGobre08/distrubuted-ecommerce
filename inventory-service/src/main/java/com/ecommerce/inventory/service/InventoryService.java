package com.ecommerce.inventory.service;

import com.ecommerce.inventory.entity.Product;
import com.ecommerce.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public boolean reserveStock(Long productId, Integer quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found: " + productId));

        System.out.println("Product: " + product.getName());
        System.out.println("Available stock: " + product.getStock());
        System.out.println("Requested quantity: " + quantity);

        if (product.getStock() < quantity) {

            System.out.println("❌ Insufficient stock");

            return false;
        }

        product.setStock(product.getStock() - quantity);

        productRepository.save(product);

        System.out.println("✅ Stock reserved successfully");
        System.out.println("Remaining stock: " + product.getStock());

        return true;
    }
}