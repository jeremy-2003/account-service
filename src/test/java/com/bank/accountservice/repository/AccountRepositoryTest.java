package com.bank.accountservice.repository;

import com.bank.accountservice.model.account.Account;
import com.bank.accountservice.model.account.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;

@DataMongoTest
@AutoConfigureDataMongo
class AccountRepositoryTest {
    @Autowired
    private AccountRepository accountRepository;
    @BeforeEach
    void setUp() {
        accountRepository.deleteAll().block();
    }
    @Test
    void findByCustomerId_Success() {
        // Arrange
        String customerId = "123";
        Account account1 = createAccount("A1", customerId);
        Account account2 = createAccount("A2", customerId);
        Account otherAccount = createAccount("A3", "456");
        accountRepository.saveAll(Arrays.asList(account1, account2, otherAccount)).blockLast();
        // Act & Assert
        StepVerifier.create(accountRepository.findByCustomerId(customerId))
                .expectNextMatches(account -> account.getId().equals("A1"))
                .expectNextMatches(account -> account.getId().equals("A2"))
                .verifyComplete();
    }
    @Test
    void findByCustomerId_NoAccounts() {
        // Act & Assert
        StepVerifier.create(accountRepository.findByCustomerId("nonexistent"))
                .verifyComplete();
    }
    @Test
    void findByCustomerId_MultipleCustomers() {
        // Arrange
        Account account1 = createAccount("A1", "123");
        Account account2 = createAccount("A2", "456");
        Account account3 = createAccount("A3", "123");
        accountRepository.saveAll(Arrays.asList(account1, account2, account3)).blockLast();
        // Act & Assert
        StepVerifier.create(accountRepository.findByCustomerId("123"))
                .expectNextMatches(account -> account.getId().equals("A1"))
                .expectNextMatches(account -> account.getId().equals("A3"))
                .verifyComplete();
    }
    @Test
    void save_Success() {
        // Arrange
        Account account = createAccount("A1", "123");
        // Act & Assert
        StepVerifier.create(accountRepository.save(account))
                .expectNextMatches(savedAccount ->
                        savedAccount.getId().equals("A1") &&
                                savedAccount.getCustomerId().equals("123"))
                .verifyComplete();
    }
    @Test
    void deleteById_Success() {
        // Arrange
        Account account = createAccount("A1", "123");
        accountRepository.save(account).block();
        // Act & Assert
        StepVerifier.create(accountRepository.deleteById("A1"))
                .verifyComplete();
        StepVerifier.create(accountRepository.findById("A1"))
                .verifyComplete();
    }
    private Account createAccount(String id, String customerId) {
        Account account = new Account();
        account.setId(id);
        account.setCustomerId(customerId);
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(1000.0);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}
