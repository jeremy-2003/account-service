package com.bank.accountservice.service;

import com.bank.accountservice.event.AccountEventProducer;
import com.bank.accountservice.model.account.Account;
import com.bank.accountservice.model.account.AccountType;
import com.bank.accountservice.model.creditcard.CreditCard;
import com.bank.accountservice.model.creditcard.CreditCardType;
import com.bank.accountservice.model.customer.Customer;
import com.bank.accountservice.model.customer.CustomerType;
import com.bank.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerCacheService customerCacheService;
    @Mock
    private CustomerClientService customerClientService;
    @Mock
    private ReactiveMongoTemplate mongoTemplate;
    @Mock
    private AccountEventProducer accountEventProducer;
    @Mock
    private CreditClientService creditClientService;
    @InjectMocks
    private AccountService accountService;
    private Customer personalCustomer;
    private Customer businessCustomer;
    private Account savingsAccount;
    private Account checkingAccount;
    private Account fixedTermAccount;
    private CreditCard creditCard;
    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                accountRepository,
                customerCacheService,
                customerClientService,
                mongoTemplate,
                accountEventProducer,
                creditClientService
        );
        ReflectionTestUtils.setField(accountService, "maintenanFee", new BigDecimal("100"));
        ReflectionTestUtils.setField(accountService, "minBalanceRequirement", new BigDecimal("60"));
        ReflectionTestUtils.setField(accountService, "maxFreeTransactionSavings", 5);
        ReflectionTestUtils.setField(accountService, "maxFreeTransactionChecking", 4);
        ReflectionTestUtils.setField(accountService, "maxFreeTransactionFixedTerms", 3);
        ReflectionTestUtils.setField(accountService, "costTransactionSavings", new BigDecimal("5.50"));
        ReflectionTestUtils.setField(accountService, "costTransactionChecking", new BigDecimal("4.20"));
        ReflectionTestUtils.setField(accountService, "costTransactionFixedTerms", new BigDecimal("8.50"));

        personalCustomer = new Customer();
        personalCustomer.setId("P001");
        personalCustomer.setFullName("John Doe");
        personalCustomer.setCustomerType(CustomerType.PERSONAL);
        businessCustomer = new Customer();
        businessCustomer.setId("B001");
        businessCustomer.setFullName("Business Corp");
        businessCustomer.setCustomerType(CustomerType.BUSINESS);

        savingsAccount = new Account();
        savingsAccount.setId("SA001");
        savingsAccount.setCustomerId("P001");
        savingsAccount.setAccountType(AccountType.SAVINGS);
        savingsAccount.setBalance(1000.0);

        checkingAccount = new Account();
        checkingAccount.setId("CA001");
        checkingAccount.setCustomerId("B001");
        checkingAccount.setAccountType(AccountType.CHECKING);
        checkingAccount.setBalance(2000.0);

        fixedTermAccount = new Account();
        fixedTermAccount.setId("FT001");
        fixedTermAccount.setCustomerId("P001");
        fixedTermAccount.setAccountType(AccountType.FIXED_TERM);
        fixedTermAccount.setBalance(3000.0);

        creditCard = new CreditCard();
        creditCard.setId("CC001");
        creditCard.setCustomerId("P001");
        creditCard.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
    }
    @Test
    void createAccount_PersonalCustomer_SavingsAccount_Success() {
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(personalCustomer));
        when(mongoTemplate.find(any(Query.class), eq(Account.class))).thenReturn(Flux.empty());
        when(creditClientService.getCreditCardsByCustomer(anyString()))
            .thenReturn(Mono.just(Collections.singletonList(creditCard)));
        when(customerClientService.updateVipPymStatus(anyString(), anyBoolean())).thenReturn(Mono.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(savingsAccount));
        StepVerifier.create(accountService.createAccount(savingsAccount))
                .expectNextMatches(account ->
                        account.getAccountType() == AccountType.SAVINGS &&
                                account.isVipAccount() &&
                                account.getMinBalanceRequirement().equals(new BigDecimal("60")) &&
                                account.getMaxFreeTransaction() == 5 &&
                                account.getTransactionCost().equals(new BigDecimal("5.50"))
                )
                .verifyComplete();
        verify(accountEventProducer).publishAccountCreated(any(Account.class));
    }
    @Test
    void createAccount_BusinessCustomer_CheckingAccount_Success() {
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(businessCustomer));
        when(mongoTemplate.find(any(Query.class), eq(Account.class))).thenReturn(Flux.empty());
        when(creditClientService.getCreditCardsByCustomer(anyString()))
            .thenReturn(Mono.just(Collections.singletonList(creditCard)));
        when(customerClientService.updateVipPymStatus(anyString(), anyBoolean())).thenReturn(Mono.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(checkingAccount));
        StepVerifier.create(accountService.createAccount(checkingAccount))
                .expectNextMatches(account ->
                        account.getAccountType() == AccountType.CHECKING &&
                                account.isPymAccount() &&
                                account.getMaintenanFee().equals(BigDecimal.ZERO) &&
                                account.getMaxFreeTransaction() == 4 &&
                                account.getTransactionCost().equals(new BigDecimal("4.20"))
                )
                .verifyComplete();
        verify(accountEventProducer).publishAccountCreated(any(Account.class));
    }
    @Test
    void createAccount_BusinessCustomer_SavingsAccount_Error() {
        Account invalidAccount = new Account();
        invalidAccount.setCustomerId("B001");
        invalidAccount.setAccountType(AccountType.SAVINGS);
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(businessCustomer));
        when(mongoTemplate.find(any(Query.class), eq(Account.class))).thenReturn(Flux.empty());
        StepVerifier.create(accountService.createAccount(invalidAccount))
                .expectError(RuntimeException.class)
                .verify();
    }
    @Test
    void createAccount_PersonalCustomer_DuplicateAccount_Error() {
        when(customerCacheService.getCustomer(anyString())).thenReturn(Mono.just(personalCustomer));
        when(mongoTemplate.find(any(Query.class), eq(Account.class)))
                .thenReturn(Flux.just(savingsAccount));
        StepVerifier.create(accountService.createAccount(savingsAccount))
                .expectError(RuntimeException.class)
                .verify();
    }
    @Test
    void updateAccount_Success() {
        Account existingAccount = new Account();
        existingAccount.setId("A001");
        existingAccount.setBalance(1000.0);
        Account updatedAccount = new Account();
        updatedAccount.setBalance(2000.0);
        updatedAccount.setHolders(Collections.singletonList("John Doe"));
        when(accountRepository.findById("A001")).thenReturn(Mono.just(existingAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(updatedAccount));
        StepVerifier.create(accountService.updateAccount("A001", updatedAccount))
                .expectNextMatches(account ->
                        account.getBalance() == 2000.0 &&
                                account.getHolders().contains("John Doe")
                )
                .verifyComplete();
        verify(accountEventProducer).publishAccountUpdate(any(Account.class));
    }
    @Test
    void deleteAccount_LastAccount_Success() {
        when(accountRepository.findById("A001")).thenReturn(Mono.just(savingsAccount));
        when(accountRepository.findByCustomerId(anyString())).thenReturn(Flux.empty());
        when(customerClientService.updateVipPymStatus(anyString(), anyBoolean())).thenReturn(Mono.empty());
        when(accountRepository.deleteById(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(accountService.deleteAccount("A001"))
                .verifyComplete();
        verify(customerClientService).updateVipPymStatus(anyString(), eq(false));
    }
    @Test
    void findAllAccounts_Success() {
        List<Account> accounts = Arrays.asList(savingsAccount, checkingAccount);
        when(accountRepository.findAll()).thenReturn(Flux.fromIterable(accounts));
        StepVerifier.create(accountService.findAllAccounts())
                .expectNextCount(2)
                .verifyComplete();
    }
    @Test
    void getAccountById_Success() {
        when(accountRepository.findById("A001")).thenReturn(Mono.just(savingsAccount));
        StepVerifier.create(accountService.getAccountById("A001"))
                .expectNext(savingsAccount)
                .verifyComplete();
    }
    @Test
    void getAccountsByCustomer_Success() {
        List<Account> customerAccounts = Arrays.asList(savingsAccount, fixedTermAccount);
        when(accountRepository.findByCustomerId("P001")).thenReturn(Flux.fromIterable(customerAccounts));
        StepVerifier.create(accountService.getAccountsByCustomer("P001"))
                .expectNextCount(2)
                .verifyComplete();
    }
    @Test
    void updateVipPymStatus_VIP_Success() {
        Account existingAccount = new Account();
        existingAccount.setId("A001");
        existingAccount.setAccountType(AccountType.SAVINGS);
        when(accountRepository.findById("A001")).thenReturn(Mono.just(existingAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(existingAccount));
        StepVerifier.create(accountService.updateVipPymStatus("A001", true, "VIP"))
                .expectNextMatches(account ->
                        account.isVipAccount() &&
                                account.getMinBalanceRequirement().equals(new BigDecimal("60"))
                )
                .verifyComplete();
    }
    @Test
    void updateVipPymStatus_PYM_Success() {
        Account existingAccount = new Account();
        existingAccount.setId("A001");
        existingAccount.setAccountType(AccountType.CHECKING);
        when(accountRepository.findById("A001")).thenReturn(Mono.just(existingAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(existingAccount));
        StepVerifier.create(accountService.updateVipPymStatus("A001", true, "PYM"))
                .expectNextMatches(account ->
                        account.isPymAccount() &&
                                account.getMaintenanFee().equals(BigDecimal.ZERO)
                )
                .verifyComplete();
    }
    @Test
    void validateCustomer_CacheHit_Success() {
        when(customerCacheService.getCustomer("P001")).thenReturn(Mono.just(personalCustomer));
        when(mongoTemplate.find(any(Query.class), eq(Account.class))).thenReturn(Flux.empty());
        when(creditClientService.getCreditCardsByCustomer(anyString()))
                .thenReturn(Mono.just(Collections.singletonList(creditCard)));
        when(customerClientService.updateVipPymStatus(anyString(), anyBoolean()))
                .thenReturn(Mono.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(savingsAccount));
        StepVerifier.create(accountService.createAccount(savingsAccount))
                .expectNextMatches(account -> {
                    return account.getCustomerId().equals("P001") &&
                            account.getAccountType() == AccountType.SAVINGS &&
                            account.isVipAccount() &&
                            account.getMaxFreeTransaction() == 5 &&
                            account.getTransactionCost().equals(new BigDecimal("5.50"));
                })
                .verifyComplete();
        verify(customerClientService, never()).getCustomerById(anyString());
    }
    @Test
    void validateCustomer_CacheMiss_Success() {
        when(customerCacheService.getCustomer("P001")).thenReturn(Mono.empty());
        when(customerClientService.getCustomerById("P001")).thenReturn(Mono.just(personalCustomer));
        when(customerCacheService.saveCustomer(anyString(), any(Customer.class))).thenReturn(Mono.empty());
        when(mongoTemplate.find(any(Query.class), eq(Account.class))).thenReturn(Flux.empty());
        when(creditClientService.getCreditCardsByCustomer(anyString()))
                .thenReturn(Mono.just(Collections.singletonList(creditCard)));
        when(customerClientService.updateVipPymStatus(anyString(), anyBoolean()))
                .thenReturn(Mono.empty());
        Account expectedSavedAccount = new Account();
        expectedSavedAccount.setId("SA001");
        expectedSavedAccount.setCustomerId("P001");
        expectedSavedAccount.setAccountType(AccountType.SAVINGS);
        expectedSavedAccount.setBalance(1000.0);
        expectedSavedAccount.setVipAccount(true);
        expectedSavedAccount.setMaxFreeTransaction(5);
        expectedSavedAccount.setTransactionCost(new BigDecimal("5.50"));
        expectedSavedAccount.setMinBalanceRequirement(new BigDecimal("60"));
        expectedSavedAccount.setHolders(Collections.singletonList("John Doe"));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(expectedSavedAccount));
        StepVerifier.create(accountService.createAccount(savingsAccount))
                .expectNextMatches(account -> {
                    return account.getCustomerId().equals("P001") &&
                            account.getAccountType() == AccountType.SAVINGS &&
                            account.isVipAccount() &&
                            account.getMaxFreeTransaction() == 5 &&
                            account.getTransactionCost().equals(new BigDecimal("5.50")) &&
                            account.getMinBalanceRequirement().equals(new BigDecimal("60")) &&
                            account.getHolders().contains("John Doe");
                })
                .verifyComplete();
        verify(customerClientService).getCustomerById("P001");
        verify(customerCacheService).saveCustomer(eq("P001"), any(Customer.class));
        verify(creditClientService).getCreditCardsByCustomer("P001");
        verify(customerClientService).updateVipPymStatus(eq("P001"), eq(true));
    }
    @Test
    void deleteAccount_PymCustomer_WithMultipleAccounts_RemovingLastCheckingAccount() {
        // Arrange
        Account accountToDelete = new Account();
        accountToDelete.setId("CH001");
        accountToDelete.setCustomerId("C001");
        accountToDelete.setAccountType(AccountType.CHECKING);
        Account savingsAccount = new Account();
        savingsAccount.setId("SA001");
        savingsAccount.setCustomerId("C001");
        savingsAccount.setAccountType(AccountType.SAVINGS);
        Customer pymCustomer = new Customer();
        pymCustomer.setId("C001");
        pymCustomer.setPym(true);

        when(accountRepository.findById("CH001")).thenReturn(Mono.just(accountToDelete));
        when(accountRepository.findByCustomerId("C001")).thenReturn(Flux.just(savingsAccount));
        when(customerClientService.getCustomerById("C001")).thenReturn(Mono.just(pymCustomer));
        when(customerClientService.updateVipPymStatus("C001", false)).thenReturn(Mono.empty());
        when(accountRepository.deleteById("CH001")).thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(accountService.deleteAccount("CH001"))
                .verifyComplete();
        verify(customerClientService).updateVipPymStatus("C001", false);
        verify(accountRepository).deleteById("CH001");
    }
    @Test
    void deleteAccount_PymCustomer_WithMultipleAccounts_KeepingCheckingAccount() {
        // Arrange
        Account accountToDelete = new Account();
        accountToDelete.setId("SA001");
        accountToDelete.setCustomerId("C001");
        accountToDelete.setAccountType(AccountType.SAVINGS);
        Account remainingCheckingAccount = new Account();
        remainingCheckingAccount.setId("CH001");
        remainingCheckingAccount.setCustomerId("C001");
        remainingCheckingAccount.setAccountType(AccountType.CHECKING);
        Customer pymCustomer = new Customer();
        pymCustomer.setId("C001");
        pymCustomer.setPym(true);

        when(accountRepository.findById("SA001")).thenReturn(Mono.just(accountToDelete));
        when(accountRepository.findByCustomerId("C001")).thenReturn(Flux.just(remainingCheckingAccount));
        when(customerClientService.getCustomerById("C001")).thenReturn(Mono.just(pymCustomer));
        when(accountRepository.deleteById("SA001")).thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(accountService.deleteAccount("SA001"))
                .verifyComplete();
        verify(customerClientService, never()).updateVipPymStatus(anyString(), anyBoolean());
        verify(accountRepository).deleteById("SA001");
    }
    @Test
    void deleteAccount_VipCustomer_WithMultipleAccounts_RemovingLastSavingsAccount() {
        // Arrange
        Account accountToDelete = new Account();
        accountToDelete.setId("SA001");
        accountToDelete.setCustomerId("C001");
        accountToDelete.setAccountType(AccountType.SAVINGS);
        Account checkingAccount = new Account();
        checkingAccount.setId("CH001");
        checkingAccount.setCustomerId("C001");
        checkingAccount.setAccountType(AccountType.CHECKING);
        Customer vipCustomer = new Customer();
        vipCustomer.setId("C001");
        vipCustomer.setVip(true);

        when(accountRepository.findById("SA001")).thenReturn(Mono.just(accountToDelete));
        when(accountRepository.findByCustomerId("C001")).thenReturn(Flux.just(checkingAccount));
        when(customerClientService.getCustomerById("C001")).thenReturn(Mono.just(vipCustomer));
        when(customerClientService.updateVipPymStatus("C001", false)).thenReturn(Mono.empty());
        when(accountRepository.deleteById("SA001")).thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(accountService.deleteAccount("SA001"))
                .verifyComplete();
        verify(customerClientService).updateVipPymStatus("C001", false);
        verify(accountRepository).deleteById("SA001");
    }
    @Test
    void deleteAccount_VipCustomer_WithMultipleAccounts_KeepingSavingsAccount() {
        // Arrange
        Account accountToDelete = new Account();
        accountToDelete.setId("CH001");
        accountToDelete.setCustomerId("C001");
        accountToDelete.setAccountType(AccountType.CHECKING);
        Account remainingSavingsAccount = new Account();
        remainingSavingsAccount.setId("SA001");
        remainingSavingsAccount.setCustomerId("C001");
        remainingSavingsAccount.setAccountType(AccountType.SAVINGS);
        Customer vipCustomer = new Customer();
        vipCustomer.setId("C001");
        vipCustomer.setVip(true);

        when(accountRepository.findById("CH001")).thenReturn(Mono.just(accountToDelete));
        when(accountRepository.findByCustomerId("C001")).thenReturn(Flux.just(remainingSavingsAccount));
        when(customerClientService.getCustomerById("C001")).thenReturn(Mono.just(vipCustomer));
        when(accountRepository.deleteById("CH001")).thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(accountService.deleteAccount("CH001"))
                .verifyComplete();
        verify(customerClientService, never()).updateVipPymStatus(anyString(), anyBoolean());
        verify(accountRepository).deleteById("CH001");
    }
    @Test
    void deleteAccount_VipAndPymCustomer_WithMultipleAccounts_RemovingBothTypes() {
        // Arrange
        Account accountToDelete = new Account();
        accountToDelete.setId("SA001");
        accountToDelete.setCustomerId("C001");
        accountToDelete.setAccountType(AccountType.SAVINGS);
        Account checkingAccount = new Account();
        checkingAccount.setId("CH001");
        checkingAccount.setCustomerId("C001");
        checkingAccount.setAccountType(AccountType.CHECKING);
        Customer vipAndPymCustomer = new Customer();
        vipAndPymCustomer.setId("C001");
        vipAndPymCustomer.setVip(true);
        vipAndPymCustomer.setPym(true);
        // Mock repository calls
        when(accountRepository.findById("SA001")).thenReturn(Mono.just(accountToDelete));
        when(accountRepository.findByCustomerId("C001")).thenReturn(Flux.just(checkingAccount));
        when(customerClientService.getCustomerById("C001")).thenReturn(Mono.just(vipAndPymCustomer));
        when(customerClientService.updateVipPymStatus("C001", false)).thenReturn(Mono.empty());
        when(accountRepository.deleteById("SA001")).thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(accountService.deleteAccount("SA001"))
                .verifyComplete();
        verify(customerClientService).updateVipPymStatus("C001", false);
        verify(accountRepository).deleteById("SA001");
    }
}
