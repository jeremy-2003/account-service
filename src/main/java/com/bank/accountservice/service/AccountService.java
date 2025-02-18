package com.bank.accountservice.service;

import com.bank.accountservice.model.Account;
import com.bank.accountservice.model.AccountType;
import com.bank.accountservice.model.Customer;
import com.bank.accountservice.model.CustomerType;
import com.bank.accountservice.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final CustomerCacheService customerCacheService;
    private final CustomerClientService customerClientService;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AccountEventProducer accountEventProducer;
    public AccountService(AccountRepository accountRepository,
                          CustomerCacheService customerCacheService,
                          CustomerClientService customerClientService,
                          ReactiveMongoTemplate mongoTemplate,
                          AccountEventProducer accountEventProducer){
        this.accountRepository = accountRepository;
        this.customerCacheService = customerCacheService;
        this.customerClientService = customerClientService;
        this.mongoTemplate = mongoTemplate;
        this.accountEventProducer = accountEventProducer;
    }
    private Mono<Customer> validateCustomer(String customerId) {
        log.info("Validating customer with ID: {}", customerId);
        return customerCacheService.getCustomer(customerId)
                .doOnNext(customer -> log.info("Customer found in cache: {}", customer.getId()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Customer not found in cache, fetching from service: {}", customerId);
                    return fetchCustomerFromService(customerId);
                }))
                .doOnError(e -> log.error("Error in customer validation: {}", e.getMessage()))
                .onErrorResume(ex -> {
                    log.error("Final error handling in validateCustomer: {}", ex.getMessage());
                    return fetchCustomerFromService(customerId);
                });
    }

    private Mono<Customer> fetchCustomerFromService(String customerId) {
        return customerClientService.getCustomerById(customerId)
                .flatMap(customer -> {
                    try {
                        return customerCacheService.saveCustomer(customerId, customer)
                                .then(Mono.just(customer));
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Error serializing customer", e));
                    }
                })
                .onErrorResume(e -> Mono.empty());
    }


    private Mono<Account> validateAccountRules(Account account, Customer customer){
        Query query = new Query(Criteria.where("customerId").is(account.getCustomerId()));
        return mongoTemplate.find(query, Account.class)
                .collectList()
                .flatMap(existingAccounts ->{
                    if(customer.getCustomerType() == CustomerType.PERSONAL){
                        return validatePersonalCustomerRules(account, existingAccounts, customer);
                    }else{
                        return validateBusinessCustomerRules(account, customer);
                    }
                });
    }
    private Mono<Account> validatePersonalCustomerRules(Account account, List<Account> existingAccounts, Customer customer){
        boolean hasSavings = existingAccounts.stream().anyMatch(a->a.getAccountType() == AccountType.SAVINGS);
        boolean hasChecking = existingAccounts.stream().anyMatch(a->a.getAccountType() == AccountType.CHECKING);
        boolean hasFixed = existingAccounts.stream().anyMatch(a->a.getAccountType() == AccountType.FIXED_TERM);

        if((account.getAccountType() == AccountType.SAVINGS && hasSavings) ||
                (account.getAccountType() == AccountType.CHECKING && hasChecking)||
                (account.getAccountType() == AccountType.FIXED_TERM && hasFixed)){
            return Mono.error(new RuntimeException("Personal customers can have only one savings, one checking, or fixed-term account."));
        }
        String customerNameNormalized = customer.getFullName().trim().toLowerCase();
        if(account.getHolders() != null && !account.getHolders().isEmpty()){
            for (String holder: account.getHolders()){
                if(!holder.trim().toLowerCase().equals(customerNameNormalized)){
                    return Mono.error(new RuntimeException("Personal customers can only have themselves as account holders."));
                }
            }
        }
        account.setHolders(Collections.singletonList(customer.getFullName()));
        return Mono.just(account);
    }
    private Mono<Account> validateBusinessCustomerRules(Account account, Customer customer){
        if(account.getAccountType() == AccountType.SAVINGS || account.getAccountType() == AccountType.FIXED_TERM){
            return Mono.error(new RuntimeException("Business customres can only have checking accounts"));
        }
        String customerNameNormalized = customer.getFullName().trim().toLowerCase();
        if(account.getHolders()== null || account.getHolders().isEmpty()){
            account.setHolders(Collections.singletonList(customer.getFullName()));
        }
        else if (account.getHolders().stream().noneMatch(holder->holder.trim().toLowerCase().equals(customerNameNormalized))){
            account.getHolders().add(0,customer.getFullName());
        }
        return Mono.just(account);
    }
    private Customer fromJson(String customerJson){
        try {
            return new ObjectMapper().readValue(customerJson, Customer.class);
        } catch (Exception e){
            throw new RuntimeException("Error deserializing customer", e);
        }
    }
    public Mono<Account> createAccount(Account account){
        System.out.println("Received account: " + account);
        System.out.println("Customer ID: " + account.getCustomerId());
        return validateCustomer(account.getCustomerId())
                .flatMap(customerJson -> {
                    return validateAccountRules(account, customerJson);
                })
                .flatMap(validAccount ->{
                    account.setCreatedAd(LocalDateTime.now());
                    account.setModifiedAd(null);
                    return accountRepository.save(account);
                })
                .doOnSuccess(accountEventProducer::publishAccountCreated);
    }
    public Mono<Account> updateAccount(String accountId, Account updatedAccount){
        return accountRepository.findById(accountId)
                .flatMap(existingAccount -> {
                    existingAccount.setModifiedAd(LocalDateTime.now());
                    existingAccount.setBalance(updatedAccount.getBalance());
                    existingAccount.setHolders(updatedAccount.getHolders());
                    existingAccount.setSigners(updatedAccount.getSigners());
                    return accountRepository.save(existingAccount);
                })
                .doOnSuccess(accountEventProducer::publishAccountUpdate);
    }
    public Mono<Void> deleteAccount(String accountId){
        return accountRepository.deleteById(accountId);
    }
    public Flux<Account> findAllAccounts(){
        return accountRepository.findAll();
    }
    public Mono<Account> getAccountById(String accountId){
        return accountRepository.findById(accountId);
    }
    public Flux<Account> getAccountsByCustomer(String customerId){
        return accountRepository.findByCustomerId(customerId);
    }

}
