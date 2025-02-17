package com.bank.accountservice.service;

import com.bank.accountservice.model.Account;
import com.bank.accountservice.model.AccountType;
import com.bank.accountservice.model.Customer;
import com.bank.accountservice.model.CustomerType;
import com.bank.accountservice.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final CustomerCacheService customerCacheService;
    private final CustomerClientService customerClientService;
    private final ReactiveMongoTemplate mongoTemplate;

    public AccountService(AccountRepository accountRepository,
                          CustomerCacheService customerCacheService,
                          CustomerClientService customerClientService,
                          ReactiveMongoTemplate mongoTemplate){
        this.accountRepository = accountRepository;
        this.customerCacheService = customerCacheService;
        this.customerClientService = customerClientService;
        this.mongoTemplate = mongoTemplate;
    }
    private Mono<Customer> validateCustomer(String customerId){
        return customerCacheService.getCustomer(customerId)
                .switchIfEmpty(fetchCustomerFromService(customerId));
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
                        return validatePersonalCustomerRules(account, existingAccounts);
                    }else{
                        return validateBusinessCustomerRules(account);
                    }
                });
    }
    private Mono<Account> validatePersonalCustomerRules(Account account, List<Account> existingAccounts){
        boolean hasSavings = existingAccounts.stream().anyMatch(a->a.getAccountType() == AccountType.SAVINGS);
        boolean hasChecking = existingAccounts.stream().anyMatch(a->a.getAccountType() == AccountType.CHECKING);
        boolean hasFixed = existingAccounts.stream().anyMatch(a->a.getAccountType() == AccountType.FIXED_TERM);

        if((account.getAccountType() == AccountType.SAVINGS && hasSavings) ||
                (account.getAccountType() == AccountType.CHECKING && hasChecking)||
                (account.getAccountType() == AccountType.FIXED_TERM && hasFixed)){
            return Mono.error(new RuntimeException("Personal customers can have only one savings, one checking, or fixed-term account."));
        }
        return Mono.just(account);
    }
    private Mono<Account> validateBusinessCustomerRules(Account account){
        if(account.getAccountType() == AccountType.SAVINGS || account.getAccountType() == AccountType.FIXED_TERM){
            return Mono.error(new RuntimeException("Business customres can only have checking accounts"));
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
                });
                /*
                .doOnSuccess(this::publishAccountCreated)
                */
    }
    public Mono<Account> updateAccount(String accountId, Account updatedAccount){
        return accountRepository.findById(accountId)
                .flatMap(existingAccount -> {
                    existingAccount.setModifiedAd(LocalDateTime.now());
                    existingAccount.setAccountType(updatedAccount.getAccountType());
                    existingAccount.setHolders(updatedAccount.getHolders());
                    existingAccount.setSigners(updatedAccount.getSigners());
                    return accountRepository.save(existingAccount);
                });
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
