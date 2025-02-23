package com.bank.accountservice.service;
import com.bank.accountservice.model.customer.Customer;
import com.bank.accountservice.model.customer.CustomerType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class CustomerCacheServiceTest {
    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOperations;
    private CustomerCacheService customerCacheService;
    private ObjectMapper objectMapper;
    @BeforeEach
    void setUp() {
        customerCacheService = new CustomerCacheService(redisTemplate);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    @Test
    void saveCustomer_Success() {
        // Arrange
        String customerId = "123";
        Customer customer = createCustomer(customerId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(eq("Customer:" + customerId), anyString()))
                .thenReturn(Mono.just(true));
        // Act & Assert
        StepVerifier.create(customerCacheService.saveCustomer(customerId, customer))
                .verifyComplete();
    }
    @Test
    void saveCustomer_NullId() {
        // Arrange
        Customer customer = new Customer();
        // Act & Assert
        StepVerifier.create(customerCacheService.saveCustomer(null, customer))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
    @Test
    void getCustomer_Success() {
        // Arrange
        String customerId = "123";
        Customer customer = createCustomer(customerId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        try {
            String customerJson = objectMapper.writeValueAsString(customer);
            when(valueOperations.get("Customer:" + customerId))
                    .thenReturn(Mono.just(customerJson));
            // Act & Assert
            StepVerifier.create(customerCacheService.getCustomer(customerId))
                    .expectNextMatches(c -> c.getId().equals(customerId))
                    .verifyComplete();
        } catch (JsonProcessingException e) {
            fail("Error serializing customer: " + e.getMessage());
        }
    }
    @Test
    void getCustomer_NotFound() {
        // Arrange
        String customerId = "123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("Customer:" + customerId))
                .thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(customerCacheService.getCustomer(customerId))
                .verifyComplete();
    }
    @Test
    void getCustomer_DeserializationError() {
        // Arrange
        String customerId = "123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("Customer:" + customerId))
                .thenReturn(Mono.just("invalid json"));
        // Act & Assert
        StepVerifier.create(customerCacheService.getCustomer(customerId))
                .verifyComplete();
    }
    private Customer createCustomer(String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName("John Doe");
        customer.setCustomerType(CustomerType.PERSONAL);
        customer.setCreatedAd(LocalDateTime.now());
        return customer;
    }
}
