package com.bank.accountservice.service;

import com.bank.accountservice.client.CreditClientService;
import com.bank.accountservice.client.CustomerClientService;
import com.bank.accountservice.client.CustomerEligibilityClientService;
import com.bank.accountservice.event.AccountEventProducer;
import com.bank.accountservice.model.account.Account;
import com.bank.accountservice.model.account.AccountType;
import com.bank.accountservice.model.customer.Customer;
import com.bank.accountservice.model.customer.CustomerType;
import com.bank.accountservice.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class AccountService {
    @Value("${default-values.maintenanFee}")
    private BigDecimal maintenanFee;
    @Value("${default-values.minBalanceRequirement}")
    private BigDecimal minBalanceRequirement;
    @Value("${transaction-max.savings}")
    private Integer maxFreeTransactionSavings;
    @Value("${transaction-max.checking}")
    private Integer maxFreeTransactionChecking;
    @Value("${transaction-max.fixed-terms}")
    private Integer maxFreeTransactionFixedTerms;
    @Value("${transaction-cost.savings}")
    private BigDecimal costTransactionSavings;
    @Value("${transaction-cost.checking}")
    private BigDecimal costTransactionChecking;
    @Value("${transaction-cost.fixed-terms}")
    private BigDecimal costTransactionFixedTerms;
    private final AccountRepository accountRepository;
    private final CustomerCacheService customerCacheService;
    private final CustomerClientService customerClientService;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AccountEventProducer accountEventProducer;
    private final CreditClientService creditClientService;
    private CustomerEligibilityClientService customerEligibilityClientService;
    public AccountService(AccountRepository accountRepository,
                          CustomerCacheService customerCacheService,
                          CustomerClientService customerClientService,
                          ReactiveMongoTemplate mongoTemplate,
                          AccountEventProducer accountEventProducer,
                          CreditClientService creditClientService,
                          CustomerEligibilityClientService customerEligibilityClientService) {
        this.accountRepository = accountRepository;
        this.customerCacheService = customerCacheService;
        this.customerClientService = customerClientService;
        this.mongoTemplate = mongoTemplate;
        this.accountEventProducer = accountEventProducer;
        this.creditClientService = creditClientService;
        this.customerEligibilityClientService = customerEligibilityClientService;
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


    private Mono<Account> validateAccountRules(Account account, Customer customer) {
        Query query = new Query(Criteria.where("customerId").is(account.getCustomerId()));
        return mongoTemplate.find(query, Account.class)
                .collectList()
                .flatMap(existingAccounts -> {
                    if (customer.getCustomerType() == CustomerType.PERSONAL) {
                        return validatePersonalCustomerRules(account, existingAccounts, customer);
                    } else {
                        return validateBusinessCustomerRules(account, customer);
                    }
                });
    }
    private Mono<Account> validatePersonalCustomerRules(Account account,
                                                        List<Account> existingAccounts,
                                                        Customer customer) {
        boolean hasSavings = existingAccounts.stream().anyMatch(a -> a.getAccountType() == AccountType.SAVINGS);
        boolean hasChecking = existingAccounts.stream().anyMatch(a -> a.getAccountType() == AccountType.CHECKING);
        boolean hasFixed = existingAccounts.stream().anyMatch(a -> a.getAccountType() == AccountType.FIXED_TERM);

        if ((account.getAccountType() == AccountType.SAVINGS && hasSavings) ||
                (account.getAccountType() == AccountType.CHECKING && hasChecking) ||
                (account.getAccountType() == AccountType.FIXED_TERM && hasFixed)) {
            return Mono.error(new RuntimeException("Personal customers can have only one savings, one checking, " +
                "or fixed-term account."));
        }

        String customerNameNormalized = customer.getFullName().trim().toLowerCase();
        if (account.getHolders() != null && !account.getHolders().isEmpty()) {
            for (String holder : account.getHolders()) {
                if (!holder.trim().toLowerCase().equals(customerNameNormalized)) {
                    return Mono.error(new RuntimeException("Personal customers can only have themselves " +
                        "as account holders."));
                }
            }
        }
        if (AccountType.SAVINGS == account.getAccountType()) {
            account.setMaxFreeTransaction(maxFreeTransactionSavings);
            account.setTransactionCost(costTransactionSavings);
        }
        if (AccountType.CHECKING == account.getAccountType()) {
            account.setMaxFreeTransaction(maxFreeTransactionChecking);
            account.setTransactionCost(costTransactionChecking);
        }
        if (AccountType.FIXED_TERM == account.getAccountType()) {
            account.setMaxFreeTransaction(maxFreeTransactionFixedTerms);
            account.setTransactionCost(costTransactionFixedTerms);
        }
        if (account.getAccountType() == AccountType.SAVINGS) {
            return creditClientService.getCreditCardsByCustomer(account.getCustomerId())
                    .defaultIfEmpty(Collections.emptyList())
                    .flatMap(creditCards -> {
                        if (creditCards != null && !creditCards.isEmpty()) {
                            return customerClientService.updateVipPymStatus(account.getCustomerId(), true)
                                    .thenReturn(creditCards);
                        } else {
                            return Mono.just(creditCards);
                        }
                    })
                    .map(creditCards -> {
                        if (creditCards != null && !creditCards.isEmpty()) {
                            account.setMinBalanceRequirement(minBalanceRequirement);
                            account.setVipAccount(true);
                        } else {
                            account.setMinBalanceRequirement(null);
                            account.setVipAccount(false);
                        }
                        account.setHolders(Collections.singletonList(customer.getFullName()));
                        return account;
                    });
        }
        account.setHolders(Collections.singletonList(customer.getFullName()));
        return Mono.just(account);
    }

    private Mono<Account> validateBusinessCustomerRules(Account account, Customer customer) {
        if (account.getAccountType() == AccountType.SAVINGS || account.getAccountType() == AccountType.FIXED_TERM) {
            return Mono.error(new RuntimeException("Business customers can only have checking accounts"));
        }

        String customerNameNormalized = customer.getFullName().trim().toLowerCase();
        if (account.getHolders() == null || account.getHolders().isEmpty()) {
            account.setHolders(Collections.singletonList(customer.getFullName()));
        } else if (account.getHolders().stream().noneMatch(holder ->
            holder.trim().toLowerCase().equals(customerNameNormalized))) {
            account.getHolders().add(0, customer.getFullName());
        }
        if (AccountType.CHECKING == account.getAccountType()) {
            account.setMaxFreeTransaction(maxFreeTransactionChecking);
            account.setTransactionCost(costTransactionChecking);
        }
        if (account.getAccountType() == AccountType.CHECKING) {
            return creditClientService.getCreditCardsByCustomer(account.getCustomerId())
                    .defaultIfEmpty(Collections.emptyList())
                    .flatMap(creditCards -> {
                        if (creditCards != null && !creditCards.isEmpty()) {
                            return customerClientService.updateVipPymStatus(account.getCustomerId(), true)
                                    .thenReturn(creditCards);
                        } else {
                            return Mono.just(creditCards);
                        }
                    })
                    .map(creditCards -> {
                        if (creditCards != null && !creditCards.isEmpty()) {
                            customerClientService.updateVipPymStatus(account.getCustomerId(), true).subscribe();
                            account.setMaintenanFee(BigDecimal.valueOf(0));
                            account.setPymAccount(true);
                        } else {
                            account.setMaintenanFee(maintenanFee);
                            account.setPymAccount(false);
                        }
                        return account;
                    });
        }
        return Mono.just(account);
    }

    private Customer fromJson(String customerJson) {
        try {
            return new ObjectMapper().readValue(customerJson, Customer.class);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing customer", e);
        }
    }
    public Mono<Account> createAccount(Account account) {
        System.out.println("Received account: " + account);
        System.out.println("Customer ID: " + account.getCustomerId());
        return customerEligibilityClientService.hasOverdueDebt(account.getCustomerId())
                .flatMap(hasOverDueDebt -> {
                    if (hasOverDueDebt) {
                        return Mono.error(new RuntimeException("Customer has overdue " +
                            "debt and cannot create a new credit"));
                    }
                    if (account.getBalance() < 0) {
                        return Mono.error(new IllegalArgumentException("Account balance must be " +
                            "greater than or equal to 0"));
                    }
                    return validateCustomer(account.getCustomerId())
                            .flatMap(customerJson -> validateAccountRules(account, customerJson))
                            .flatMap(validAccount -> {
                                account.setCreatedAt(LocalDateTime.now());
                                account.setModifiedAt(null);
                                return accountRepository.save(account);
                            })
                            .doOnSuccess(accountEventProducer::publishAccountCreated);
                });
    }

    public Mono<Account> updateAccount(String accountId, Account updatedAccount) {
        return accountRepository.findById(accountId)
                .flatMap(existingAccount -> {
                    existingAccount.setModifiedAt(LocalDateTime.now());
                    existingAccount.setBalance(updatedAccount.getBalance());
                    existingAccount.setHolders(updatedAccount.getHolders());
                    existingAccount.setSigners(updatedAccount.getSigners());
                    return accountRepository.save(existingAccount);
                })
                .doOnSuccess(accountEventProducer::publishAccountUpdate);
    }
    public Mono<Void> deleteAccount(String accountId) {
        return accountRepository.findById(accountId)
                .flatMap(account -> {
                    return accountRepository.findByCustomerId(account.getCustomerId())
                            .filter(acc -> !acc.getId().equals(accountId))
                            .collectList()
                            .flatMap(accounts -> {
                                if (accounts.isEmpty()) {
                                    return customerClientService.updateVipPymStatus(account.getCustomerId(), false)
                                            .then(accountRepository.deleteById(accountId));
                                }
                                return customerClientService.getCustomerById(account.getCustomerId())
                                        .flatMap(customer -> {
                                            Mono<Customer> updateStatus = Mono.empty();
                                            if (customer.isPym()) {
                                                boolean hasCheckingAccount = accounts.stream()
                                                        .anyMatch(acc -> acc.getAccountType() == AccountType.CHECKING);
                                                if (!hasCheckingAccount) {
                                                    updateStatus = customerClientService.
                                                        updateVipPymStatus(account.getCustomerId(), false);
                                                }
                                            }
                                            if (customer.isVip()) {
                                                boolean hasSavingsAccount = accounts.stream()
                                                        .anyMatch(acc -> acc.getAccountType() == AccountType.SAVINGS);
                                                if (!hasSavingsAccount) {
                                                    updateStatus = customerClientService.
                                                        updateVipPymStatus(account.getCustomerId(), false);
                                                }
                                            }
                                            return updateStatus.then(accountRepository.deleteById(accountId));
                                        });
                            });
                });
    }
    public Flux<Account> findAllAccounts() {
        return accountRepository.findAll();
    }
    public Mono<Account> getAccountById(String accountId) {
        return accountRepository.findById(accountId);
    }
    public Flux<Account> getAccountsByCustomer(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }
    public Mono<Account> updateVipPymStatus(String accountId, boolean isVipPym, String type) {
        return accountRepository.findById(accountId)
                .flatMap(existingAccount -> {
                    if ("PYM".equals(type)) {
                        existingAccount.setPymAccount(isVipPym);
                        existingAccount.setMaintenanFee(isVipPym ? BigDecimal.valueOf(0) : maintenanFee);
                    } else if ("VIP".equals(type)) {
                        existingAccount.setVipAccount(isVipPym);
                        existingAccount.setMinBalanceRequirement(isVipPym ? minBalanceRequirement : null);
                    }
                    return accountRepository.save(existingAccount);
                });
    }

}
