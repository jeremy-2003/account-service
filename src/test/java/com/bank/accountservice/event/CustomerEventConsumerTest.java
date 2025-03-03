package com.bank.accountservice.event;
import com.bank.accountservice.model.customer.Customer;
import com.bank.accountservice.model.customer.CustomerType;
import com.bank.accountservice.service.CustomerCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class CustomerEventConsumerTest {
    @Mock
    private CustomerCacheService cacheService;
    private CustomerEventConsumer customerEventConsumer;
    @BeforeEach
    void setUp() {
        customerEventConsumer = new CustomerEventConsumer(cacheService);
    }
    @Test
    void consume_Success() {
        // Arrange
        Customer customer = createCustomer("123");
        when(cacheService.saveCustomer(customer.getId(), customer))
                .thenReturn(Mono.empty());
        // Act
        customerEventConsumer.consume(customer);
        // Assert
        verify(cacheService).saveCustomer(customer.getId(), customer);
    }
    @Test
    void consume_ErrorSavingCustomer() {
        // Arrange
        Customer customer = createCustomer("123");
        RuntimeException expectedError = new RuntimeException("Error saving customer");
        when(cacheService.saveCustomer(customer.getId(), customer))
                .thenReturn(Mono.error(expectedError));
        // Act
        customerEventConsumer.consume(customer);
        // Assert
        verify(cacheService).saveCustomer(customer.getId(), customer);
    }
    @Test
    void consume_NullCustomer() {
        // Act
        customerEventConsumer.consume(null);
        // Assert
        verify(cacheService, never()).saveCustomer(any(), any());
    }
    @Test
    void consume_CustomerWithNullId() {
        // Arrange
        Customer customer = createCustomer(null);
        when(cacheService.saveCustomer(null, customer))
                .thenReturn(Mono.error(new IllegalArgumentException("Customer ID cannot be null")));
        // Act
        customerEventConsumer.consume(customer);
        // Assert
        verify(cacheService).saveCustomer(null, customer);
    }
    @Test
    void consume_UnexpectedError() {
        // Arrange
        Customer customer = createCustomer("123");
        RuntimeException unexpectedError = new RuntimeException("Unexpected error");
        when(cacheService.saveCustomer(any(), any()))
                .thenThrow(unexpectedError);
        // Act
        customerEventConsumer.consume(customer);
        // Assert
        verify(cacheService).saveCustomer(customer.getId(), customer);
    }
    private Customer createCustomer(String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPhone("1234567890");
        customer.setCustomerType(CustomerType.PERSONAL);
        customer.setStatus("ACTIVE");
        customer.setCreatedAt(LocalDateTime.now());
        return customer;
    }
}