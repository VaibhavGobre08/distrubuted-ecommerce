package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class ProductRepositoryIntegrationTest {

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
                "spring.datasource.driver-class-name",
                postgres::getDriverClassName
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop"
        );
    }

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldSaveAndFindProduct() {

        // Given
        Product product = new Product(
                null,
                "Laptop",
                new BigDecimal("50000"),
                10
        );

        // When
        Product savedProduct =
                productRepository.save(product);

        Optional<Product> foundProduct =
                productRepository.findById(savedProduct.getId());

        // Then
        assertTrue(foundProduct.isPresent());

        assertEquals(
                "Laptop",
                foundProduct.get().getName()
        );

        assertEquals(
                10,
                foundProduct.get().getStock()
        );
    }
}