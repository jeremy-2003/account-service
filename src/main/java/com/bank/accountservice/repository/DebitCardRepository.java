package com.bank.accountservice.repository;

import com.bank.accountservice.model.debitcard.DebitCard;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DebitCardRepository extends ReactiveMongoRepository<DebitCard, String> {
    Flux<DebitCard> findByCustomerId(String customerId);
    Mono<DebitCard> findByCardNumber(String cardNumber);
    Flux<DebitCard> findByAssociatedAccountIdsContaining(String accountId);
    Flux<DebitCard> findByPrimaryAccountId(String accountId);
}
