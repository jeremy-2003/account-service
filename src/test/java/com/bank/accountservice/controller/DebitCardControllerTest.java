package com.bank.accountservice.controller;
import com.bank.accountservice.dto.*;
import com.bank.accountservice.model.debitcard.DebitCard;
import com.bank.accountservice.service.DebitCardService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class DebitCardControllerTest {
    @Mock
    private DebitCardService debitCardService;
    @InjectMocks
    private DebitCardController debitCardController;
    private DebitCard testDebitCard;
    private List<DebitCard> testDebitCards;
    private BalancePrimaryAccount testBalancePrimaryAccount;
    @BeforeEach
    void setUp() {
        testDebitCard = new DebitCard();
        testDebitCard.setId("card123");
        testDebitCard.setCardNumber("4111111111111111");
        testDebitCard.setCustomerId("customer123");
        testDebitCard.setStatus("ACTIVE");
        testDebitCard.setPrimaryAccountId("account123");
        testDebitCard.setAssociatedAccountIds(new ArrayList<>(List.of("account123")));
        testDebitCard.setExpirationDate(LocalDateTime.now().plusYears(4));
        testDebitCard.setCreatedAt(LocalDateTime.now());
        testDebitCard.setModifiedAt(LocalDateTime.now());
        testDebitCards = Arrays.asList(testDebitCard);
        testBalancePrimaryAccount = BalancePrimaryAccount.builder()
                .cardId("card123")
                .cardNumber("4111111111111111")
                .primaryAccountId("account123")
                .balancePrimaryAccount(new Double("1500.00"))
                .build();
    }
    @Test
    void getDebitCardByCardNumber_ShouldReturnCard_WhenCardExists() {
        // Arrange
        String cardNumber = "4111111111111111";
        when(debitCardService.getDebitCardByCardNumber(cardNumber)).thenReturn(Mono.just(testDebitCard));
        // Act & Assert
        StepVerifier.create(debitCardController.getDebitCardByCardNumber(cardNumber))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo(OK.value());
                    assertThat(response.getBody().getMessage()).isEqualTo("Debit card successfully obtained");
                    assertThat(response.getBody().getData()).isEqualTo(testDebitCard);
                })
                .verifyComplete();
        verify(debitCardService).getDebitCardByCardNumber(cardNumber);
    }
    @Test
    void getDebitCardByCardNumber_ShouldReturnNotFound_WhenCardDoesNotExist() {
        // Arrange
        String cardNumber = "0000000000000000";
        when(debitCardService.getDebitCardByCardNumber(cardNumber)).thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(debitCardController.getDebitCardByCardNumber(cardNumber))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo(NOT_FOUND.value());
                    assertThat(response.getBody().getMessage()).isEqualTo("Debit card not found");
                    assertThat(response.getBody().getData()).isNull();
                })
                .verifyComplete();
        verify(debitCardService).getDebitCardByCardNumber(cardNumber);
    }
    @Test
    void getDebitCardByPrimaryAccountId_ShouldReturnList_WhenCardsExist() {
        // Arrange
        String primaryAccountId = "account123";
        when(debitCardService.getDebitCardByPrimaryAccountId(primaryAccountId))
            .thenReturn(Flux.fromIterable(testDebitCards));
        // Act & Assert
        StepVerifier.create(debitCardController.getDebitCardByPrimaryAccountId(primaryAccountId))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getStatus()).isEqualTo(OK.value());
                    assertThat(response.getBody().getMessage()).isEqualTo("Debit cards retrieved successfully");
                    assertThat(response.getBody().getData()).isEqualTo(testDebitCards);
                })
                .verifyComplete();
        verify(debitCardService).getDebitCardByPrimaryAccountId(primaryAccountId);
    }
    @Test
    void createDebitCard_Success() {
        // Arrange
        CreateCardRequest request = new CreateCardRequest();
        request.setCustomerId("customer123");
        request.setPrimaryAccountId("account123");
        when(debitCardService.createDebitCard("customer123",
            "account123"))
                .thenReturn(Mono.just(testDebitCard));
        // Act & Assert
        StepVerifier.create(debitCardController.createDebitCard(request))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Debit Card created successfully",
                        responseEntity.getBody().getMessage());
                    assertEquals(testDebitCard, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void createDebitCard_Error() {
        // Arrange
        CreateCardRequest request = new CreateCardRequest();
        request.setCustomerId("customer123");
        request.setPrimaryAccountId("account123");
        String errorMessage = "Customer has overdue debt and cannot create a new credit";
        when(debitCardService.createDebitCard("customer123", "account123"))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));
        // Act & Assert
        StepVerifier.create(debitCardController.createDebitCard(request))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getBody().getStatus());
                    assertEquals(errorMessage, responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getDebitCardsByCustomer_Success() {
        // Arrange
        String customerId = "customer123";
        when(debitCardService.getDebitCardsByCustomerId(customerId))
                .thenReturn(Flux.fromIterable(testDebitCards));
        // Act & Assert
        StepVerifier.create(debitCardController.getDebitCardsByCustomer(customerId))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account retrieved successfully",
                        responseEntity.getBody().getMessage());
                    assertEquals(testDebitCards, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getDebitCardsByCustomer_NotFound() {
        // Arrange
        String customerId = "nonexistent";
        when(debitCardService.getDebitCardsByCustomerId(customerId))
                .thenReturn(Flux.empty());
        // Act & Assert
        StepVerifier.create(debitCardController.getDebitCardsByCustomer(customerId))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getBody().getStatus());
                    assertEquals("No debit cards found for the customer", responseEntity.getBody().getMessage());
                    assertEquals(Collections.emptyList(), responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getDebitCardById_Success() {
        // Arrange
        String cardId = "card123";
        when(debitCardService.getDebitCardById(cardId))
                .thenReturn(Mono.just(testDebitCard));
        // Act & Assert
        StepVerifier.create(debitCardController.getDebitCardById(cardId))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Debit card successfully obtained", responseEntity.getBody().getMessage());
                    assertEquals(testDebitCard, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getDebitCardById_NotFound() {
        // Arrange
        String cardId = "nonexistent";
        when(debitCardService.getDebitCardById(cardId))
                .thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(debitCardController.getDebitCardById(cardId))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getBody().getStatus());
                    assertEquals("Debit card not found", responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getBalancePrimaryAccount_Success() {
        // Arrange
        String cardId = "card123";
        when(debitCardService.getBalancePrimaryAccount(cardId))
                .thenReturn(Mono.just(testBalancePrimaryAccount));
        // Act & Assert
        StepVerifier.create(debitCardController.getBalancePrimaryAccount(cardId))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Balance primary account of the" +
                        " debit card successfully obtained", responseEntity.getBody().getMessage());
                    assertEquals(testBalancePrimaryAccount, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getBalancePrimaryAccount_NotFound() {
        // Arrange
        String cardId = "nonexistent";
        when(debitCardService.getBalancePrimaryAccount(cardId))
                .thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(debitCardController.getBalancePrimaryAccount(cardId))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getBody().getStatus());
                    assertEquals("Debit card or primary account not found", responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void associateAccount_Success() {
        // Arrange
        String cardId = "card123";
        AssociateAccountRequest request = new AssociateAccountRequest();
        request.setAccountId("account456");
        DebitCard updatedCard = copyDebitCard(testDebitCard);
        updatedCard.getAssociatedAccountIds().add("account456");
        when(debitCardService.associateAccountToCard(cardId, "account456"))
                .thenReturn(Mono.just(updatedCard));
        // Act & Assert
        StepVerifier.create(debitCardController.associateAccount(cardId, request))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Account successfully associated" +
                        " with the card", responseEntity.getBody().getMessage());
                    assertTrue(responseEntity.getBody().getData().getAssociatedAccountIds().contains("account456"));
                })
                .verifyComplete();
    }
    @Test
    void associateAccount_Error() {
        // Arrange
        String cardId = "card123";
        AssociateAccountRequest request = new AssociateAccountRequest();
        request.setAccountId("account456");
        String errorMessage = "The card or account do not belong to the same customer";
        when(debitCardService.associateAccountToCard(cardId, "account456"))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));
        // Act & Assert
        StepVerifier.create(debitCardController.associateAccount(cardId, request))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getBody().getStatus());
                    assertEquals(errorMessage, responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void changePrimaryAccount_Success() {
        // Arrange
        String cardId = "card123";
        AssociateAccountRequest request = new AssociateAccountRequest();
        request.setAccountId("account456");
        DebitCard updatedCard = copyDebitCard(testDebitCard);
        updatedCard.setPrimaryAccountId("account456");
        updatedCard.getAssociatedAccountIds().add("account456");
        when(debitCardService.changePrimaryAccount(cardId, "account456"))
                .thenReturn(Mono.just(updatedCard));
        // Act & Assert
        StepVerifier.create(debitCardController.changePrimaryAccount(cardId, request))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Main account successfully updated", responseEntity.getBody().getMessage());
                    assertEquals("account456", responseEntity.getBody().getData().getPrimaryAccountId());
                })
                .verifyComplete();
    }
    @Test
    void changePrimaryAccount_Error() {
        // Arrange
        String cardId = "card123";
        AssociateAccountRequest request = new AssociateAccountRequest();
        request.setAccountId("account456");
        String errorMessage = "The new main account must be associated with the card";
        when(debitCardService.changePrimaryAccount(cardId, "account456"))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));
        // Act & Assert
        StepVerifier.create(debitCardController.changePrimaryAccount(cardId, request))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getBody().getStatus());
                    assertEquals(errorMessage, responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void updateCardStatus_Success() {
        // Arrange
        String cardId = "card123";
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("BLOCKED");
        DebitCard updatedCard = copyDebitCard(testDebitCard);
        updatedCard.setStatus("BLOCKED");
        when(debitCardService.updateCardStatus(cardId, "BLOCKED"))
                .thenReturn(Mono.just(updatedCard));
        // Act & Assert
        StepVerifier.create(debitCardController.updateCardStatus(cardId, request))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Card status updated successfully", responseEntity.getBody().getMessage());
                    assertEquals("BLOCKED", responseEntity.getBody().getData().getStatus());
                })
                .verifyComplete();
    }
    @Test
    void getDebitCardsByAccount_Success() {
        // Arrange
        String accountId = "account123";
        when(debitCardService.getDebitCardsByAccountId(accountId))
                .thenReturn(Flux.fromIterable(testDebitCards));
        // Act & Assert
        StepVerifier.create(debitCardController.getDebitCardsByAccount(accountId))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Debit cards retrieved successfully", responseEntity.getBody().getMessage());
                    assertEquals(testDebitCards, responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void getDebitCardsByAccount_NotFound() {
        // Arrange
        String accountId = "nonexistent";
        when(debitCardService.getDebitCardsByAccountId(accountId))
                .thenReturn(Flux.empty());
        // Act & Assert
        StepVerifier.create(debitCardController.getDebitCardsByAccount(accountId))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getBody().getStatus());
                    assertEquals("No debit cards found for the account", responseEntity.getBody().getMessage());
                    assertEquals(Collections.emptyList(), responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    @Test
    void deleteDebitCard_Success() {
        // Arrange
        String cardId = "card123";
        DebitCard deletedCard = copyDebitCard(testDebitCard);
        deletedCard.setStatus("DELETED");
        when(debitCardService.deleteDebitCard(cardId))
                .thenReturn(Mono.just(deletedCard));
        // Act & Assert
        StepVerifier.create(debitCardController.deleteDebitCard(cardId))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.OK.value(), responseEntity.getBody().getStatus());
                    assertEquals("Debit card successfully deleted", responseEntity.getBody().getMessage());
                    assertEquals("DELETED", responseEntity.getBody().getData().getStatus());
                })
                .verifyComplete();
    }
    @Test
    void deleteDebitCard_Error() {
        // Arrange
        String cardId = "nonexistent";
        String errorMessage = "Card not found";
        when(debitCardService.deleteDebitCard(cardId))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));
        // Act & Assert
        StepVerifier.create(debitCardController.deleteDebitCard(cardId))
                .assertNext(responseEntity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getBody().getStatus());
                    assertEquals(errorMessage, responseEntity.getBody().getMessage());
                    assertNull(responseEntity.getBody().getData());
                })
                .verifyComplete();
    }
    private DebitCard copyDebitCard(DebitCard original) {
        DebitCard copy = new DebitCard();
        copy.setId(original.getId());
        copy.setCardNumber(original.getCardNumber());
        copy.setCustomerId(original.getCustomerId());
        copy.setStatus(original.getStatus());
        copy.setPrimaryAccountId(original.getPrimaryAccountId());
        copy.setAssociatedAccountIds(new ArrayList<>(original.getAssociatedAccountIds()));
        copy.setExpirationDate(original.getExpirationDate());
        copy.setCreatedAt(original.getCreatedAt());
        copy.setModifiedAt(original.getModifiedAt());
        return copy;
    }
}