package com.bank.accountservice.repository;

import com.bank.accountservice.model.account.Account;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountRepository extends ReactiveMongoRepository<Account, String> {
    Flux<Account> findByCustomerId(String customerId);
    Mono<Account> findByIdAndCustomerId(String accountId, String customerId);
}
