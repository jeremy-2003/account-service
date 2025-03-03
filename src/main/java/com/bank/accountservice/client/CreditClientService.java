package com.bank.accountservice.client;

import com.bank.accountservice.dto.BaseResponse;
import com.bank.accountservice.model.creditcard.CreditCard;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class CreditClientService {
    private final WebClient webClient;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
    public CreditClientService(WebClient.Builder builder,
                               @Value("${credit-service.base-url}") String baseUrl,
                               CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("creditService");
        log.info("Circuit breaker '{}' initialized with state: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
    }
    public Mono<List<CreditCard>> getCreditCardsByCustomer(String customerId) {
        return webClient.get()
                .uri("/credit-cards/customer/{customerId}", customerId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> {
                    if (clientResponse.statusCode() == HttpStatus.BAD_REQUEST) {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorMessage -> {
                                    log.warn("No credit cards found for customer {}: {}", customerId, errorMessage);
                                    return Mono.empty();
                                });
                    }
                    return clientResponse.createException().flatMap(Mono::error);
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<CreditCard>>>() { })
                .flatMap(baseResponse -> {
                    if (baseResponse.getStatus() == 400 && "This customer doesnt have credit cards"
                            .equals(baseResponse.getMessage())) {
                        log.warn("No credit cards found for customer {}: {}", customerId, baseResponse.getMessage());
                        return Mono.empty();
                    }
                    return Mono.justOrEmpty(baseResponse.getData());
                })
                .doOnNext(result -> log.info("Credit API response: {}", result))
                .doOnError(error -> log.error("Error fetching credit cards for customer {}: {}",
                        customerId, error.getMessage()))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to get credit cards for customer {}. Reason: {}",
                            customerId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Credit service is unavailable for retrieving credit card information. " +
                                    "Cannot continue with the operation."));
                });
    }
}