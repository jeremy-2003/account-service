package com.bank.accountservice.service;

import com.bank.accountservice.model.customer.Customer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class CustomerCacheService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String CUSTOMER_KEY_PREFIX = "Customer:";
    public CustomerCacheService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
        log.info("Attempting to retrieve customer from Redis with key: {}", key);
        return redisTemplate.opsForValue().get(key)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSubscribe(s -> log.info("Subscribe to get customer from Redis with key: {}", key))
                .doOnNext(value -> {
                    if (value == null) {
                        log.warn("Null value retrieved from Redis for key: {}", key);
                    } else {
                        log.info("Retrieved from cache for key {}: value length={}", key, value.length());
                    }
                })
                .flatMap(customerJson -> {
                    log.info("Processing JSON for customer: {} (length: {})", key, customerJson.length());
                    try {
                        Customer customer = objectMapper.readValue(customerJson, Customer.class);
                        log.info("Successfully deserialized customer: {}", customer.getId());
                        return Mono.just(customer);
                    } catch (Exception e) {
                        log.error("Error deserializing customer JSON: {}", e.getMessage(), e);
                        return Mono.empty();
                    }
                })
                .timeout(Duration.ofSeconds(5))  // Incrementé el timeout a 5 segundos
                .doOnError(TimeoutException.class, e ->
                        log.error("Redis operation timed out for key: {}", key))
                .doOnError(e -> {
                    if (!(e instanceof TimeoutException)) {
                        log.error("Error retrieving customer from cache: {}", e.getMessage());
                    }
                })
                .onErrorResume(ex -> {
                    log.error("Final error handling for retrieving customer: {}", ex.getMessage());
                    return Mono.empty();
                });
    }
}