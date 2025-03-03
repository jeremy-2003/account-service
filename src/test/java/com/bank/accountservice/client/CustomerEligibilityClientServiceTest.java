package com.bank.accountservice.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.util.function.Function;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerEligibilityClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    private CircuitBreaker circuitBreaker;
    private CustomerEligibilityClientService eligibilityService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        // Mock para CircuitBreaker
        when(circuitBreakerRegistry.circuitBreaker("customerEligibilityService")).thenReturn(circuitBreaker);
        when(circuitBreaker.getName()).thenReturn("customerEligibilityService");
        when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        eligibilityService = spy(new CustomerEligibilityClientService(webClientBuilder,
                "http://localhost:8080",
                circuitBreakerRegistry));
    }
    @Test
    void hasOverdueDebt_CustomerHasDebt() {
        // Arrange
        String customerId = "123";
        doReturn(Mono.just(true))
                .when(eligibilityService)
                .hasOverdueDebt(customerId);
        // Act & Assert
        StepVerifier.create(eligibilityService.hasOverdueDebt(customerId))
                .expectNext(true)
                .verifyComplete();
    }
    @Test
    void hasOverdueDebt_CustomerHasNoDebt() {
        // Arrange
        String customerId = "456";
        doReturn(Mono.just(false))
                .when(eligibilityService)
                .hasOverdueDebt(customerId);
        // Act & Assert
        StepVerifier.create(eligibilityService.hasOverdueDebt(customerId))
                .expectNext(false)
                .verifyComplete();
    }
    @Test
    void hasOverdueDebt_ClientError() {
        // Arrange
        String customerId = "invalid";
        doReturn(Mono.just(true))
                .when(eligibilityService)
                .hasOverdueDebt(customerId);
        StepVerifier.create(eligibilityService.hasOverdueDebt(customerId))
                .expectNext(true)
                .verifyComplete();
    }
    @Test
    void hasOverdueDebt_ServerError() {
        // Arrange
        String customerId = "123";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(customerId))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Server error")));
        CustomerEligibilityClientService realService = new CustomerEligibilityClientService(
                webClientBuilder, "http://localhost:8080", circuitBreakerRegistry);
        StepVerifier.create(realService.hasOverdueDebt(customerId))
                .expectNext(true)
                .verifyComplete();
    }
}