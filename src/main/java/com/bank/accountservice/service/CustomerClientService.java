package com.bank.accountservice.service;

import com.bank.accountservice.dto.BaseResponse;
import com.bank.accountservice.model.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CustomerClientService {
    private final WebClient webClient;
    private final String customerServiceUrl; // Guardamos manualmente la URL base
    public CustomerClientService(WebClient.Builder webClientBuilder,
                                 @Value("${customer-service.base-url}") String customerServiceUrl) {
        this.customerServiceUrl = customerServiceUrl; // Guardamos la URL base
        this.webClient = webClientBuilder.baseUrl(customerServiceUrl).build();
    }
    public Mono<Customer> getCustomerById(String customerId) {
        String fullUrl = customerServiceUrl + "/" + customerId;
        log.info("Sending request to Customer Service API: {}", fullUrl);
        return webClient.get()
                .uri("/{id}", customerId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        Mono.error(new RuntimeException("Client error: " + response.statusCode()))
                )
                .onStatus(HttpStatus::is5xxServerError, response ->
                        Mono.error(new RuntimeException("Server error: " + response.statusCode()))
                )
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Customer>>() {})
                .flatMap(response -> {
                    if(response.getData() != null){
                        return Mono.just(response.getData());
                    } else {
                        return Mono.empty();
                    }
                })
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(e -> log.error("Error while fetching customer: {}", e.getMessage()))
                .doOnTerminate(() -> log.info("Request to Customer API completed"));
    }
}