package com.bank.accountservice.client;

import com.bank.accountservice.model.customer.Customer;
import com.bank.accountservice.model.customer.CustomerType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    private CircuitBreaker circuitBreaker;
    private CustomerClientService customerClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        // Mock para CircuitBreaker
        when(circuitBreakerRegistry.circuitBreaker("customerService")).thenReturn(circuitBreaker);
        when(circuitBreaker.getName()).thenReturn("customerService");
        when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        customerClientService = spy(new CustomerClientService(webClientBuilder,
                "http://localhost:8080",
                circuitBreakerRegistry));
    }
    @Test
    void getCustomerById_Success() {
        // Arrange
        String customerId = "123";
        Customer expectedCustomer = createCustomer(customerId);
        doReturn(Mono.just(expectedCustomer))
                .when(customerClientService)
                .getCustomerById(customerId);
        // Act & Assert
        StepVerifier.create(customerClientService.getCustomerById(customerId))
                .expectNextMatches(customer ->
                        customer.getId().equals(customerId) &&
                                customer.getFullName().equals("John Doe") &&
                                customer.getCustomerType() == CustomerType.PERSONAL)
                .verifyComplete();
    }
    @Test
    void getCustomerById_NotFound() {
        // Arrange
        String customerId = "nonexistent";
        RuntimeException expectedError = new RuntimeException(
                "Customer service is unavailable for retrieving customer information. " +
                        "Cannot continue with the operation.");
        doReturn(Mono.error(expectedError))
                .when(customerClientService)
                .getCustomerById(customerId);
        // Act & Assert
        StepVerifier.create(customerClientService.getCustomerById(customerId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Customer service is unavailable"))
                .verify();
    }
    @Test
    void updateVipPymStatus_Success() {
        // Arrange
        String customerId = "123";
        boolean isVipPym = true;
        Customer updatedCustomer = createCustomer(customerId);
        doReturn(Mono.just(updatedCustomer))
                .when(customerClientService)
                .updateVipPymStatus(customerId, isVipPym);
        // Act & Assert
        StepVerifier.create(customerClientService.updateVipPymStatus(customerId, isVipPym))
                .expectNextMatches(customer ->
                        customer.getId().equals(customerId) &&
                                customer.getFullName().equals("John Doe") &&
                                customer.getCustomerType() == CustomerType.PERSONAL)
                .verifyComplete();
    }
    @Test
    void updateVipPymStatus_Error() {
        // Arrange
        String customerId = "123";
        boolean isVipPym = true;
        RuntimeException expectedError = new RuntimeException(
                "Customer service is not available to update VIP/PYM status. " +
                        "Cannot continue with account creation.");
        doReturn(Mono.error(expectedError))
                .when(customerClientService)
                .updateVipPymStatus(customerId, isVipPym);
        // Act & Assert
        StepVerifier.create(customerClientService.updateVipPymStatus(customerId, isVipPym))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Customer service is not available"))
                .verify();
    }
    private Customer createCustomer(String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName("John Doe");
        customer.setCustomerType(CustomerType.PERSONAL);
        return customer;
    }
}