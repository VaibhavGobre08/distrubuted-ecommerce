package com.ecommerce.inventory.service;

import com.ecommerce.inventory.entity.Product;
import com.ecommerce.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class InventoryServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("inventory_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop"
        );
    }

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldReserveStockUsingRealDatabase() {

        // Given
        Product product = new Product(
                null,
                "Laptop",
                new BigDecimal("50000"),
                10
        );

        Product savedProduct =
                productRepository.save(product);

        // When
        boolean result =
                inventoryService.reserveStock(
                        savedProduct.getId(),
                        3
                );

        // Then
        assertTrue(result);

        Product updatedProduct =
                productRepository
                        .findById(savedProduct.getId())
                        .orElseThrow();

        assertEquals(
                7,
                updatedProduct.getStock()
        );
    }
}