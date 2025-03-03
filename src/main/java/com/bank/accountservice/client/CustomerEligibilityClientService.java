package com.bank.accountservice.client;

import com.bank.accountservice.dto.BaseResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
@Slf4j
@Service
public class CustomerEligibilityClientService {
    private final WebClient webClient;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
    public CustomerEligibilityClientService(WebClient.Builder builder,
                                            @Value("${credit-service.base-url}") String baseUrl,
                                            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("customerEligibilityService");
        log.info("Circuit breaker '{}' initialized with state: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
    }
    public Mono<Boolean> hasOverdueDebt(String customerId) {
        log.info("Calling customer eligibility service to check if customer {} has overdue debt", customerId);
        return webClient.get()
                .uri("/customer-eligibility/has-overdue-debt/{customerId}", customerId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Boolean>>() { })
                .map(response -> {
                    log.info("Received overdue debt status for customer {}: {}", customerId, response.getData());
                    return response.getData();
                })
                .doOnError(e -> log.error("Error checking customer debt status: {}", e.getMessage()))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to check debt status for customer {}. Reason: {}",
                            customerId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    log.warn("Assuming customer has overdue debt due to service unavailability");
                    return Mono.just(true);
                });
    }
}