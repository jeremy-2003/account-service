package com.bank.accountservice.controller;

import com.bank.accountservice.model.account.Account;
import com.bank.accountservice.model.account.AccountType;
import com.bank.accountservice.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class AccountControllerTest {
    @Mock
    private AccountService accountService;
    @InjectMocks
    private AccountController accountController;
    private Account testAccount;
    private List<Account> testAccounts;
    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId("1");
        testAccount.setCustomerId("customer1");
        testAccount.setAccountType(AccountType.SAVINGS);
        testAccount.setBalance(1000.0);
        testAccount.setVipAccount(false);
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setHolders(Arrays.asList("holder1"));
        testAccount.setMaxFreeTransaction(10);
        testAccount.setTransactionCost(BigDecimal.valueOf(2.0));
        testAccounts = Arrays.asList(testAccount);
    }
    @Test
    void createAccount_Success() {
        when(accountService.createAccount(any(Account.class)))
                .thenReturn(Mono.just(testAccount));
        StepVerifier.create(accountController.createAccount(testAccount))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.CREATED.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account created successfully", responseEntity.getBody().getMessage());
                    assertEquals(testAccount, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void createAccount_Error() {
        String errorMessage = "Error creating account";
        when(accountService.createAccount(any(Account.class)))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));
        StepVerifier.create(accountController.createAccount(testAccount))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getBody().getStatus());
                    assertEquals(errorMessage, responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getAccountById_Success() {
        when(accountService.getAccountById("1"))
                .thenReturn(Mono.just(testAccount));
        StepVerifier.create(accountController.getAccountById("1"))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account retrieved successfully", responseEntity.getBody().getMessage());
                    assertEquals(testAccount, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getAccountById_NotFound() {
        when(accountService.getAccountById("nonexistent"))
                .thenReturn(Mono.empty());
        StepVerifier.create(accountController.getAccountById("nonexistent"))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account not found", responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getAccountsByCustomer_Success() {
        when(accountService.getAccountsByCustomer("customer1"))
                .thenReturn(Flux.fromIterable(testAccounts));
        StepVerifier.create(accountController.getAccountsByCustomer("customer1"))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account retrieved successfully", responseEntity.getBody().getMessage());
                    assertEquals(testAccounts, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getAccountsByCustomer_NotFound() {
        when(accountService.getAccountsByCustomer("nonexistent"))
                .thenReturn(Flux.empty());
        StepVerifier.create(accountController.getAccountsByCustomer("nonexistent"))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getBody().getStatus());
                    assertEquals("No accounts found for the customer", responseEntity.getBody().getMessage());
                    assertEquals(Collections.emptyList(), responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void updateAccount_Success() {
        when(accountService.updateAccount(eq("1"), any(Account.class)))
                .thenReturn(Mono.just(testAccount));
        StepVerifier.create(accountController.updateAccount("1", testAccount))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account updated successfully", responseEntity.getBody().getMessage());
                    assertEquals(testAccount, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void updateAccount_Error() {
        String errorMessage = "Error updating account";
        when(accountService.updateAccount(eq("1"), any(Account.class)))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));
        StepVerifier.create(accountController.updateAccount("1", testAccount))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getBody().getStatus());
                    assertEquals(errorMessage, responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void updateVipPymStatus_Success() {
        when(accountService.updateVipPymStatus("1", true, "VIP"))
                .thenReturn(Mono.just(testAccount));
        StepVerifier.create(accountController.updateVipPymStatus("1", true, "VIP"))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account update successfully", responseEntity.getBody().getMessage());
                    assertEquals(testAccount, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void updateVipPymStatus_NotFound() {
        when(accountService.updateVipPymStatus("nonexistent", true, "VIP"))
                .thenReturn(Mono.empty());
        StepVerifier.create(accountController.updateVipPymStatus("nonexistent", true, "VIP"))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account not found", responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void deleteAccount_Success() {
        when(accountService.deleteAccount("1"))
                .thenReturn(Mono.empty());
        StepVerifier.create(accountController.deleteAccount("1"))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account deleted successfully", responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void deleteAccount_Error() {
        String errorMessage = "Error deleting account";
        when(accountService.deleteAccount("1"))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));
        StepVerifier.create(accountController.deleteAccount("1"))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getBody().getStatus());
                    assertEquals(errorMessage, responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void findAllAccounts_Success() {
        when(accountService.findAllAccounts())
                .thenReturn(Flux.fromIterable(testAccounts));
        StepVerifier.create(accountController.findAllAccounts())
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account retrieved successfully", responseEntity.getBody().getMessage());
                    assertEquals(testAccounts, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void findAllAccounts_NotFound() {
        when(accountService.findAllAccounts())
                .thenReturn(Flux.empty());
        StepVerifier.create(accountController.findAllAccounts())
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getBody().getStatus());
                    assertEquals("No accounts found", responseEntity.getBody().getMessage());
                    assertEquals(Collections.emptyList(), responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
}