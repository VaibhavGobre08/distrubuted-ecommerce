package com.ecommerce.inventory.service;

import com.ecommerce.inventory.entity.Product;
import com.ecommerce.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void shouldReserveStockSuccessfully() {

        // Given
        Product product = new Product(
                1L,
                "Laptop",
                new BigDecimal("50000"),
                10
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // When
        boolean result =
                inventoryService.reserveStock(1L, 3);

        // Then
        assertTrue(result);

        assertEquals(7, product.getStock());

        verify(productRepository, times(1))
                .findById(1L);

        verify(productRepository, times(1))
                .save(product);
    }
    
    @Test
    void shouldFailWhenStockIsInsufficient() {

        // Given
        Product product = new Product(
                1L,
                "Laptop",
                new BigDecimal("50000"),
                2
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // When
        boolean result =
                inventoryService.reserveStock(1L, 5);

        // Then
        assertFalse(result);

        // Stock should remain unchanged
        assertEquals(2, product.getStock());

        // Database save should NOT happen
        verify(productRepository, never())
                .save(product);
    }
    
    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {

        // Given
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> inventoryService.reserveStock(99L, 2)
        );

        assertEquals(
                "Product not found: 99",
                exception.getMessage()
        );

        // Save should never happen
        verify(productRepository, never())
                .save(any(Product.class));
    }
}