package com.ecommerce.payment.kafka;

import com.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @Test
    void shouldPublishSuccessEventWhenPaymentSucceeds() {

        // Given
        String message = """
                {
                    "orderId": 1,
                    "customerId": 101
                }
                """;

        when(paymentService.processPayment(1L, 101L))
                .thenReturn(true);

        // When
        paymentEventConsumer.consume(message);

        // Then
        verify(paymentService, times(1))
                .processPayment(1L, 101L);

        verify(paymentEventProducer, times(1))
                .publishSuccess(1L, 101L);

        verify(paymentEventProducer, never())
                .publishFailed(anyLong(), anyLong());
    }
    
    
    @Test
    void shouldPublishFailedEventWhenPaymentFails() {

        // Given
        String message = """
                {
                    "orderId": 2,
                    "customerId": 102
                }
                """;

        when(paymentService.processPayment(2L, 102L))
                .thenReturn(false);

        // When
        paymentEventConsumer.consume(message);

        // Then
        verify(paymentService, times(1))
                .processPayment(2L, 102L);

        verify(paymentEventProducer, times(1))
                .publishFailed(2L, 102L);

        verify(paymentEventProducer, never())
                .publishSuccess(anyLong(), anyLong());
    }
}