package com.bank.accountservice.service;

import com.bank.accountservice.dto.BaseResponse;
import com.bank.accountservice.model.creditcard.CreditCard;
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
    private final String baseUrl;
    public CreditClientService(WebClient.Builder builder, @Value("${credit-service.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = builder.baseUrl(baseUrl).build();
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
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<CreditCard>>>() { })
                .flatMap(baseResponse -> {
                    if (baseResponse.getStatus() == 400 && "This customer doesnt have credit cards"
                        .equals(baseResponse.getMessage())) {
                        log.warn("No credit cards found for customer {}: {}", customerId, baseResponse.getMessage());
                        return Mono.empty();
                    }
                    return Mono.justOrEmpty(baseResponse.getData());
                })
                .doOnError(error -> log.error("Error fetching credit cards for customer {}: {}",
                    customerId, error.getMessage()));
    }

}
