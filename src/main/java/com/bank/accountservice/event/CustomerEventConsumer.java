package com.bank.accountservice.event;

import com.bank.accountservice.model.customer.Customer;
import com.bank.accountservice.service.CustomerCacheService;
import lombok.extern.slf4j.Slf4j;
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
        try {
            log.info("Reactive customer event: {}", customer);
            cacheService.saveCustomer(customer.getId(), customer)
                    .subscribe(
                        null,
                        throwable -> log.error("Error caching customer: {}", throwable.getMessage()),
                        () -> log.info("Customer cached successfully"));
        } catch (Exception e) {
            log.error("Error processing customer event", e);
        }
    }
}