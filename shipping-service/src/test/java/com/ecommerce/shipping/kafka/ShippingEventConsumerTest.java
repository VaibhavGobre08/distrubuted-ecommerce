package com.ecommerce.shipping.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingEventConsumerTest {

    @Mock
    private ShippingEventProducer shippingEventProducer;

    @InjectMocks
    private ShippingEventConsumer shippingEventConsumer;

    @Test
    void shouldPublishCreatedEventWhenShippingRequestIsReceived() {

        // Given
        String message = """
                {
                    "orderId": 1,
                    "customerId": 101
                }
                """;

        // When
        shippingEventConsumer.consume(message);

        // Then
        verify(shippingEventProducer, times(1))
                .publishCreated(1L, 101L);
    }
}