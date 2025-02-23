package com.bank.accountservice.service;

import com.bank.accountservice.model.customer.Customer;
import com.bank.accountservice.model.customer.CustomerType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.bank.accountservice.dto.BaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class CustomerClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    private CustomerClientService customerClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        customerClientService = new CustomerClientService(webClientBuilder, "http://localhost:8080");
    }
    @Test
    void getCustomerById_Success() {
        // Arrange
        String customerId = "123";
        Customer expectedCustomer = createCustomer(customerId);
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/{id}", customerId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        BaseResponse<Customer> response = new BaseResponse<>();
        response.setData(expectedCustomer);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(customerClientService.getCustomerById(customerId))
                .expectNextMatches(customer -> customer.getId().equals(customerId))
                .verifyComplete();
    }
    @Test
    void updateVipPymStatus_Success() {
        // Arrange
        String customerId = "123";
        boolean isVipPym = true;
        Customer updatedCustomer = createCustomer(customerId);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        BaseResponse<Customer> response = new BaseResponse<>();
        response.setData(updatedCustomer);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(customerClientService.updateVipPymStatus(customerId, isVipPym))
                .expectNextMatches(customer -> customer.getId().equals(customerId))
                .verifyComplete();
    }
    private Customer createCustomer(String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName("John Doe");
        customer.setCustomerType(CustomerType.PERSONAL);
        return customer;
    }
}