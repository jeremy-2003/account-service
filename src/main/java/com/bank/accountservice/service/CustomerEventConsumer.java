package com.bank.accountservice.service;

import com.bank.accountservice.model.Customer;
import com.bank.accountservice.model.CustomerType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.function.Try;
import org.springframework.data.geo.CustomMetric;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomerEventConsumer {
    private final CustomerCacheService cacheService;
    public CustomerEventConsumer(CustomerCacheService cacheService) {
        this.cacheService = cacheService;
    }
    @KafkaListener(topics = "customer-created", groupId = "account-service-group")
    public void consume(Customer customer) {
        try{
            log.info("Reactive customer event: {}", customer);
            cacheService.saveCustomer(customer.getId(), customer)
                    .subscribe(
                            null,
                            throwable -> log.error("Error caching customer: {}", throwable.getMessage()),
                            () -> log.info("Customer cached successfully")
                    );
        } catch (Exception e){
            log.error("Error processing customer event", e);
        }
    }
}