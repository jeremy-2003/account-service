package com.bank.accountservice.service;

import com.bank.accountservice.dto.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
@Slf4j
@Service
public class CustomerEligibilityClientService {
    private final WebClient webClient;
    public CustomerEligibilityClientService(WebClient.Builder builder, @Value("${credit-service.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public Mono<Boolean> hasOverdueDebt(String customerId) {
        log.info("Calling customer eligibility service to check if customer {} has overdue debt", customerId);
        return webClient.get()
                .uri("/api/customer-eligibility/has-overdue-debt/{customerId}", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Boolean>>() {})
                .map(response -> {
                    log.info("Received overdue debt status for customer {}: {}", customerId, response.getData());
                    return response.getData();
                })
                .onErrorResume(e -> {
                    log.error("Error checking customer debt status: {}", e.getMessage(), e);
                    return Mono.just(true);
                });
    }

    public Mono<Boolean> isEligibleForNewProduct(String customerId) {
        log.info("Calling customer eligibility service to check if customer {} is eligible for new products", customerId);
        return webClient.get()
                .uri("/api/customer-eligibility/is-eligible/{customerId}", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Boolean>>() {})
                .map(response -> {
                    log.info("Received eligibility status for customer {}: {}", customerId, response.getData());
                    return response.getData();
                })
                .onErrorResume(e -> {
                    log.error("Error checking customer eligibility: {}", e.getMessage(), e);
                    return Mono.just(false);
                });
    }

}

