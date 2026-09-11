package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.entity.Product;
import com.ecommerce.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class InventoryKafkaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("inventory_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("apache/kafka:3.9.0")
            );

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {

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
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop"
        );
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldConsumeReserveEventAndUpdateStock()
            throws Exception {

        // Given
        Product product = new Product(
                null,
                "Laptop",
                new BigDecimal("50000"),
                10
        );

        Product savedProduct =
                productRepository.save(product);

        String message = """
                {
                    "orderId": 1,
                    "customerId": 101,
                    "productId": %d,
                    "quantity": 3
                }
                """.formatted(savedProduct.getId());

        // When
        kafkaTemplate.send(
                "inventory.reserve",
                "1",
                message
        ).get(10, TimeUnit.SECONDS);

        // Wait for Kafka consumer
        Thread.sleep(Duration.ofSeconds(3));

        // Then
        Product updatedProduct =
                productRepository
                        .findById(savedProduct.getId())
                        .orElseThrow();

        assertEquals(
                7,
                updatedProduct.getStock()
        );

        assertTrue(
                updatedProduct.getStock() < 10
        );
    }
}