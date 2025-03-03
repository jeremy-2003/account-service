package com.bank.accountservice.service;
import com.bank.accountservice.client.CustomerEligibilityClientService;
import com.bank.accountservice.model.account.Account;
import com.bank.accountservice.model.debitcard.DebitCard;
import com.bank.accountservice.repository.AccountRepository;
import com.bank.accountservice.repository.DebitCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebitCardServiceTest {
    @Mock
    private DebitCardRepository debitCardRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerEligibilityClientService customerEligibilityClientService;
    @InjectMocks
    private DebitCardService debitCardService;
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(debitCardService, "debitCardRepository", debitCardRepository);
        ReflectionTestUtils.setField(debitCardService, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(debitCardService, "customerEligibilityClientService",
            customerEligibilityClientService);
    }
    @Test
    void createDebitCard_Success() {
        // Arrange
        String customerId = "customer123";
        String primaryAccountId = "account123";
        Account primaryAccount = new Account();
        primaryAccount.setId(primaryAccountId);
        primaryAccount.setCustomerId(customerId);
        primaryAccount.setBalance(new Double("1000.00"));
        DebitCard savedCard = new DebitCard();
        savedCard.setId("card123");
        savedCard.setCardNumber("4123456789012345");
        savedCard.setCustomerId(customerId);
        savedCard.setStatus("ACTIVE");
        savedCard.setPrimaryAccountId(primaryAccountId);
        savedCard.setAssociatedAccountIds(new ArrayList<>(List.of(primaryAccountId)));
        savedCard.setExpirationDate(LocalDateTime.now().plusYears(4));
        when(customerEligibilityClientService.hasOverdueDebt(customerId)).thenReturn(Mono.just(false));
        when(accountRepository.findById(primaryAccountId)).thenReturn(Mono.just(primaryAccount));
        when(debitCardRepository.findByCardNumber(anyString())).thenReturn(Mono.empty());
        when(debitCardRepository.save(any(DebitCard.class))).thenReturn(Mono.just(savedCard));
        // Act & Assert
        StepVerifier.create(debitCardService.createDebitCard(customerId, primaryAccountId))
                .expectNextMatches(card ->
                        card.getId().equals("card123") &&
                                card.getCustomerId().equals(customerId) &&
                                card.getPrimaryAccountId().equals(primaryAccountId) &&
                                card.getStatus().equals("ACTIVE") &&
                                card.getAssociatedAccountIds().contains(primaryAccountId))
                .verifyComplete();
        verify(customerEligibilityClientService).hasOverdueDebt(customerId);
        verify(accountRepository).findById(primaryAccountId);
        verify(debitCardRepository).save(any(DebitCard.class));
    }
    @Test
    void createDebitCard_CustomerHasOverdueDebt() {
        // Arrange
        String customerId = "customer123";
        String primaryAccountId = "account123";
        when(customerEligibilityClientService.hasOverdueDebt(customerId)).thenReturn(Mono.just(true));
        // Act & Assert
        StepVerifier.create(debitCardService.createDebitCard(customerId, primaryAccountId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Customer has overdue debt"))
                .verify();
        verify(customerEligibilityClientService).hasOverdueDebt(customerId);
        verify(accountRepository, never()).findById(anyString());
        verify(debitCardRepository, never()).save(any(DebitCard.class));
    }
    @Test
    void createDebitCard_AccountDoesNotBelongToCustomer() {
        // Arrange
        String customerId = "customer123";
        String primaryAccountId = "account123";
        Account primaryAccount = new Account();
        primaryAccount.setId(primaryAccountId);
        primaryAccount.setCustomerId("differentCustomer");
        primaryAccount.setBalance(new Double("1000.00"));
        when(customerEligibilityClientService.hasOverdueDebt(customerId)).thenReturn(Mono.just(false));
        when(accountRepository.findById(primaryAccountId)).thenReturn(Mono.just(primaryAccount));
        // Act & Assert
        StepVerifier.create(debitCardService.createDebitCard(customerId, primaryAccountId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("The main account does not belong to the client"))
                .verify();
        verify(customerEligibilityClientService).hasOverdueDebt(customerId);
        verify(accountRepository).findById(primaryAccountId);
        verify(debitCardRepository, never()).save(any(DebitCard.class));
    }
    @Test
    void associateAccountToCard_Success() {
        // Arrange
        String cardId = "card123";
        String accountId = "account456";
        String customerId = "customer123";
        DebitCard card = new DebitCard();
        card.setId(cardId);
        card.setCustomerId(customerId);
        card.setPrimaryAccountId("account123");
        card.setAssociatedAccountIds(new ArrayList<>(List.of("account123")));
        Account account = new Account();
        account.setId(accountId);
        account.setCustomerId(customerId);
        DebitCard updatedCard = new DebitCard();
        updatedCard.setId(cardId);
        updatedCard.setCustomerId(customerId);
        updatedCard.setPrimaryAccountId("account123");
        updatedCard.setAssociatedAccountIds(new ArrayList<>(List.of("account123", accountId)));
        when(debitCardRepository.findById(cardId)).thenReturn(Mono.just(card));
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(account));
        when(debitCardRepository.save(any(DebitCard.class))).thenReturn(Mono.just(updatedCard));
        // Act & Assert
        StepVerifier.create(debitCardService.associateAccountToCard(cardId, accountId))
                .expectNextMatches(updatedDebitCard ->
                        updatedDebitCard.getAssociatedAccountIds().contains(accountId) &&
                                updatedDebitCard.getAssociatedAccountIds().size() == 2)
                .verifyComplete();
        verify(debitCardRepository).findById(cardId);
        verify(accountRepository).findById(accountId);
        verify(debitCardRepository).save(any(DebitCard.class));
    }
    @Test
    void associateAccountToCard_DifferentCustomers() {
        // Arrange
        String cardId = "card123";
        String accountId = "account456";
        DebitCard card = new DebitCard();
        card.setId(cardId);
        card.setCustomerId("customer123");
        card.setPrimaryAccountId("account123");
        card.setAssociatedAccountIds(new ArrayList<>(List.of("account123")));
        Account account = new Account();
        account.setId(accountId);
        account.setCustomerId("differentCustomer");
        when(debitCardRepository.findById(cardId)).thenReturn(Mono.just(card));
        when(accountRepository.findById(accountId)).thenReturn(Mono.just(account));
        // Act & Assert
        StepVerifier.create(debitCardService.associateAccountToCard(cardId, accountId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("do not belong to the same customer"))
                .verify();
        verify(debitCardRepository).findById(cardId);
        verify(accountRepository).findById(accountId);
        verify(debitCardRepository, never()).save(any(DebitCard.class));
    }
    @Test
    void changePrimaryAccount_Success() {
        // Arrange
        String cardId = "card123";
        String newPrimaryAccountId = "account456";
        DebitCard card = new DebitCard();
        card.setId(cardId);
        card.setCustomerId("customer123");
        card.setPrimaryAccountId("account123");
        card.setAssociatedAccountIds(new ArrayList<>(List.of("account123", newPrimaryAccountId)));
        DebitCard updatedCard = new DebitCard();
        updatedCard.setId(cardId);
        updatedCard.setCustomerId("customer123");
        updatedCard.setPrimaryAccountId(newPrimaryAccountId);
        updatedCard.setAssociatedAccountIds(new ArrayList<>(List.of("account123", newPrimaryAccountId)));
        when(debitCardRepository.findById(cardId)).thenReturn(Mono.just(card));
        when(debitCardRepository.save(any(DebitCard.class))).thenReturn(Mono.just(updatedCard));
        // Act & Assert
        StepVerifier.create(debitCardService.changePrimaryAccount(cardId, newPrimaryAccountId))
                .expectNextMatches(result ->
                        result.getPrimaryAccountId().equals(newPrimaryAccountId))
                .verifyComplete();
        verify(debitCardRepository).findById(cardId);
        verify(debitCardRepository).save(any(DebitCard.class));
    }
    @Test
    void changePrimaryAccount_AccountNotAssociated() {
        // Arrange
        String cardId = "card123";
        String newPrimaryAccountId = "account456";
        DebitCard card = new DebitCard();
        card.setId(cardId);
        card.setCustomerId("customer123");
        card.setPrimaryAccountId("account123");
        card.setAssociatedAccountIds(new ArrayList<>(List.of("account123")));
        when(debitCardRepository.findById(cardId)).thenReturn(Mono.just(card));
        // Act & Assert
        StepVerifier.create(debitCardService.changePrimaryAccount(cardId, newPrimaryAccountId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("The new main account must be associated"))
                .verify();
        verify(debitCardRepository).findById(cardId);
        verify(debitCardRepository, never()).save(any(DebitCard.class));
    }
    @Test
    void getDebitCardsByCustomerId_Success() {
        // Arrange
        String customerId = "customer123";
        List<DebitCard> cards = Arrays.asList(
                createDebitCard("card1", customerId, "4111111111111111"),
                createDebitCard("card2", customerId, "4222222222222222")
        );
        when(debitCardRepository.findByCustomerId(customerId)).thenReturn(Flux.fromIterable(cards));
        // Act & Assert
        StepVerifier.create(debitCardService.getDebitCardsByCustomerId(customerId))
                .expectNextCount(2)
                .verifyComplete();
        verify(debitCardRepository).findByCustomerId(customerId);
    }
    @Test
    void getDebitCardByCardNumber_Success() {
        // Arrange
        String cardNumber = "4111111111111111";
        DebitCard card = createDebitCard("card1", "customer123", cardNumber);
        when(debitCardRepository.findByCardNumber(cardNumber)).thenReturn(Mono.just(card));
        // Act & Assert
        StepVerifier.create(debitCardService.getDebitCardByCardNumber(cardNumber))
                .expectNextMatches(result -> result.getCardNumber().equals(cardNumber))
                .verifyComplete();
        verify(debitCardRepository).findByCardNumber(cardNumber);
    }
    @Test
    void getDebitCardById_Success() {
        // Arrange
        String cardId = "card123";
        DebitCard card = createDebitCard(cardId, "customer123", "4111111111111111");
        when(debitCardRepository.findById(cardId)).thenReturn(Mono.just(card));
        // Act & Assert
        StepVerifier.create(debitCardService.getDebitCardById(cardId))
                .expectNextMatches(result -> result.getId().equals(cardId))
                .verifyComplete();
        verify(debitCardRepository).findById(cardId);
    }
    @Test
    void getDebitCardsByAccountId_Success() {
        // Arrange
        String accountId = "account123";
        List<DebitCard> cards = Arrays.asList(
                createDebitCard("card1", "customer123", "4111111111111111", accountId),
                createDebitCard("card2", "customer123", "4222222222222222", accountId)
        );
        when(debitCardRepository.findByAssociatedAccountIdsContaining(accountId)).thenReturn(Flux.fromIterable(cards));
        // Act & Assert
        StepVerifier.create(debitCardService.getDebitCardsByAccountId(accountId))
                .expectNextCount(2)
                .verifyComplete();
        verify(debitCardRepository).findByAssociatedAccountIdsContaining(accountId);
    }
    @Test
    void updateCardStatus_Success() {
        // Arrange
        String cardId = "card123";
        String newStatus = "BLOCKED";
        DebitCard card = createDebitCard(cardId, "customer123", "4111111111111111");
        card.setStatus("ACTIVE");
        DebitCard updatedCard = createDebitCard(cardId, "customer123", "4111111111111111");
        updatedCard.setStatus(newStatus);
        when(debitCardRepository.findById(cardId)).thenReturn(Mono.just(card));
        when(debitCardRepository.save(any(DebitCard.class))).thenReturn(Mono.just(updatedCard));
        // Act & Assert
        StepVerifier.create(debitCardService.updateCardStatus(cardId, newStatus))
                .expectNextMatches(result -> result.getStatus().equals(newStatus))
                .verifyComplete();
        verify(debitCardRepository).findById(cardId);
        verify(debitCardRepository).save(any(DebitCard.class));
    }
    @Test
    void deleteDebitCard_Success() {
        // Arrange
        String cardId = "card123";
        DebitCard card = createDebitCard(cardId, "customer123", "4111111111111111");
        card.setStatus("ACTIVE");
        DebitCard deletedCard = createDebitCard(cardId, "customer123", "4111111111111111");
        deletedCard.setStatus("DELETED");
        when(debitCardRepository.findById(cardId)).thenReturn(Mono.just(card));
        when(debitCardRepository.save(any(DebitCard.class))).thenReturn(Mono.just(deletedCard));
        // Act & Assert
        StepVerifier.create(debitCardService.deleteDebitCard(cardId))
                .expectNextMatches(result -> result.getStatus().equals("DELETED"))
                .verifyComplete();
        verify(debitCardRepository).findById(cardId);
        verify(debitCardRepository).save(any(DebitCard.class));
    }
    // Helper method to create a DebitCard instance
    private DebitCard createDebitCard(String id, String customerId, String cardNumber) {
        DebitCard card = new DebitCard();
        card.setId(id);
        card.setCustomerId(customerId);
        card.setCardNumber(cardNumber);
        card.setStatus("ACTIVE");
        card.setPrimaryAccountId("account123");
        card.setAssociatedAccountIds(new ArrayList<>(List.of("account123")));
        card.setExpirationDate(LocalDateTime.now().plusYears(4));
        card.setCreatedAt(LocalDateTime.now());
        card.setModifiedAt(LocalDateTime.now());
        return card;
    }
    @Test
    void getBalancePrimaryAccount_Success() {
        // Arrange
        String cardId = "card123";
        String primaryAccountId = "account123";
        DebitCard card = createDebitCard(cardId, "customer123", "4111111111111111");
        card.setPrimaryAccountId(primaryAccountId);
        Account account = new Account();
        account.setId(primaryAccountId);
        account.setBalance(new Double("1500.00"));
        when(debitCardRepository.findById(cardId)).thenReturn(Mono.just(card));
        when(accountRepository.findById(primaryAccountId)).thenReturn(Mono.just(account));
        // Act & Assert
        StepVerifier.create(debitCardService.getBalancePrimaryAccount(cardId))
                .expectNextMatches(result ->
                        result.getCardId().equals(cardId) &&
                                result.getCardNumber().equals("4111111111111111") &&
                                result.getPrimaryAccountId().equals(primaryAccountId) &&
                                String.format("%.2f", result.getBalancePrimaryAccount())
                                    .equals("1500.00"))
                .verifyComplete();
        verify(debitCardRepository).findById(cardId);
        verify(accountRepository).findById(primaryAccountId);
    }
    // Helper method to create a DebitCard instance with specific account
    private DebitCard createDebitCard(String id, String customerId, String cardNumber, String accountId) {
        DebitCard card = createDebitCard(id, customerId, cardNumber);
        card.setPrimaryAccountId(accountId);
        card.setAssociatedAccountIds(new ArrayList<>(List.of(accountId)));
        return card;
    }
}
