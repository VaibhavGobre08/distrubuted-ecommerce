package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @InjectMocks
    private InventoryEventConsumer inventoryEventConsumer;

    @Test
    void shouldPublishReservedEventWhenStockIsAvailable() {

        // Given
        String message = """
                {
                    "orderId": 1,
                    "customerId": 101,
                    "productId": 10,
                    "quantity": 2
                }
                """;

        when(inventoryService.reserveStock(10L, 2))
                .thenReturn(true);

        // When
        inventoryEventConsumer.consume(message);

        // Then
        verify(inventoryService, times(1))
                .reserveStock(10L, 2);

        verify(inventoryEventProducer, times(1))
                .publishReserved(
                        1L,
                        101L,
                        10L,
                        2
                );

        verify(inventoryEventProducer, never())
                .publishFailed(
                        anyLong(),
                        anyLong(),
                        anyLong(),
                        anyInt()
                );
    }
    
    
    @Test
    void shouldPublishFailedEventWhenStockIsInsufficient() {

        // Given
        String message = """
                {
                    "orderId": 2,
                    "customerId": 102,
                    "productId": 20,
                    "quantity": 10
                }
                """;

        when(inventoryService.reserveStock(20L, 10))
                .thenReturn(false);

        // When
        inventoryEventConsumer.consume(message);

        // Then
        verify(inventoryService, times(1))
                .reserveStock(20L, 10);

        verify(inventoryEventProducer, times(1))
                .publishFailed(
                        2L,
                        102L,
                        20L,
                        10
                );

        verify(inventoryEventProducer, never())
                .publishReserved(
                        anyLong(),
                        anyLong(),
                        anyLong(),
                        anyInt()
                );
    }
}