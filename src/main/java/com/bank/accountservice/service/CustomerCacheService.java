package com.bank.accountservice.service;

import com.bank.accountservice.model.Customer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CustomerCacheService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String CUSTOMER_KEY_PREFIX = "Customer:"; // Sin espacio después de los dos puntos
    public CustomerCacheService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    public Mono<Void> saveCustomer(String id, Customer customer) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("Customer ID cannot be null"));
        }
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(customer))
                .flatMap(customerJson -> {
                    String key = CUSTOMER_KEY_PREFIX + id;
                    log.info("Saving customer to cache with key: {}", key);
                    return redisTemplate.opsForValue().set(key, customerJson);
                })
                .doOnSuccess(result -> log.info("Successfully cached customer with ID: {}", id))
                .doOnError(error -> log.error("Error caching customer: {}", error.getMessage()))
                .then();
    }
    public Mono<Customer> getCustomer(String id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("Customer ID cannot be null"));
        }
        String key = CUSTOMER_KEY_PREFIX + id;
        return redisTemplate.opsForValue().get(key)
                .doOnNext(value -> log.info("Retrieved from cache for key {}: {}", key, value))
                .flatMap(customerJson -> Mono.fromCallable(() ->
                        objectMapper.readValue(customerJson, Customer.class)))
                .doOnSuccess(customer -> log.info("Successfully retrieved customer: {}", customer))
                .doOnError(error -> log.error("Error retrieving customer: {}", error.getMessage()));
    }
}