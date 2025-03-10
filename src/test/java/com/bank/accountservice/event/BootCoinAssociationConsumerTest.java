package com.bank.accountservice.event;

import com.bank.accountservice.client.CustomerClientService;
import com.bank.accountservice.dto.bootcoin.KafkaValidationRequest;
import com.bank.accountservice.dto.bootcoin.KafkaValidationResponse;
import com.bank.accountservice.model.account.Account;
import com.bank.accountservice.model.account.AccountType;
import com.bank.accountservice.model.customer.Customer;
import com.bank.accountservice.model.customer.CustomerType;
import com.bank.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class BootCoinAssociationConsumerTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerClientService customerClientService;
    private BootCoinAssociationConsumer bootCoinAssociationConsumer;
    @BeforeEach
    void setUp() {
        bootCoinAssociationConsumer = new BootCoinAssociationConsumer(kafkaTemplate,
            accountRepository,
            customerClientService);
    }
    @Test
    void validateYankiAssociation_Success() {
        // Arrange
        KafkaValidationRequest request = createValidationRequest("12345678",
            "123456789",
            "account-001");
        Customer customer = createCustomer("customer-001");
        Account account = createAccount("account-001", "customer-001",
            AccountType.SAVINGS);

        when(customerClientService.getCustomerByDocumentNumber(request.getDocumentNumber()))
                .thenReturn(Mono.just(customer));
        when(accountRepository.findByIdAndCustomerId(request.getBankAccountId(), customer.getId()))
                .thenReturn(Mono.just(account));
        // Act
        bootCoinAssociationConsumer.validateYankiAssociation(request);
        ArgumentCaptor<KafkaValidationResponse> responseCaptor =
            ArgumentCaptor.forClass(KafkaValidationResponse.class);
        verify(kafkaTemplate, timeout(1000))
            .send(eq("bootcoin.validation.response"),
                eq(request.getEventId()), responseCaptor.capture());
        KafkaValidationResponse capturedResponse = responseCaptor.getValue();
        assertThat(capturedResponse.isSuccess()).isTrue();
    }
    @Test
    void validateYankiAssociation_AccountNotFound() {
        // Arrange
        KafkaValidationRequest request = createValidationRequest("12345678",
            "123456789", "account-001");
        Customer customer = createCustomer("customer-001");
        when(customerClientService.getCustomerByDocumentNumber(request.getDocumentNumber()))
                .thenReturn(Mono.just(customer));
        when(accountRepository.findByIdAndCustomerId(request.getBankAccountId(), customer.getId()))
                .thenReturn(Mono.empty());
        // Act
        bootCoinAssociationConsumer.validateYankiAssociation(request);
        // Assert
        ArgumentCaptor<KafkaValidationResponse> responseCaptor = ArgumentCaptor.forClass(KafkaValidationResponse.class);
        verify(kafkaTemplate, timeout(1000))
            .send(eq("bootcoin.validation.response"),
                eq(request.getEventId()), responseCaptor.capture());
        KafkaValidationResponse capturedResponse = responseCaptor.getValue();
        assertThat(capturedResponse.isSuccess()).isFalse();
    }
    @Test
    void validateYankiAssociation_InvalidAccountType() {
        // Arrange
        KafkaValidationRequest request = createValidationRequest("12345678",
            "123456789", "account-001");
        Customer customer = createCustomer("customer-001");
        Account account = createAccount("account-001", "customer-001",
             AccountType.FIXED_TERM);
        when(customerClientService.getCustomerByDocumentNumber(request.getDocumentNumber()))
                .thenReturn(Mono.just(customer));
        when(accountRepository.findByIdAndCustomerId(request.getBankAccountId(), customer.getId()))
                .thenReturn(Mono.just(account));
        // Act
        bootCoinAssociationConsumer.validateYankiAssociation(request);
        // Assert
        ArgumentCaptor<KafkaValidationResponse> responseCaptor =
            ArgumentCaptor.forClass(KafkaValidationResponse.class);
        verify(kafkaTemplate, timeout(1000))
            .send(eq("bootcoin.validation.response"),
                eq(request.getEventId()), responseCaptor.capture());
        KafkaValidationResponse capturedResponse = responseCaptor.getValue();
        assertThat(capturedResponse.isSuccess()).isFalse();
    }
    @Test
    void validateYankiAssociation_AccountBelongsToDifferentCustomer() {
        KafkaValidationRequest request = createValidationRequest("12345678", "123456789", "account-001");
        Customer customer = createCustomer("customer-001");
        Account account = createAccount("account-001", "customer-002", AccountType.SAVINGS);
        when(customerClientService.getCustomerByDocumentNumber(request.getDocumentNumber()))
                .thenReturn(Mono.just(customer));
        when(accountRepository.findByIdAndCustomerId(request.getBankAccountId(), customer.getId()))
                .thenReturn(Mono.empty());
        bootCoinAssociationConsumer.validateYankiAssociation(request);
        ArgumentCaptor<KafkaValidationResponse> responseCaptor = ArgumentCaptor.forClass(KafkaValidationResponse.class);
        verify(kafkaTemplate, timeout(1000))
            .send(eq("bootcoin.validation.response"),
                eq(request.getEventId()), responseCaptor.capture());
        KafkaValidationResponse capturedResponse = responseCaptor.getValue();
        assertThat(capturedResponse.isSuccess()).isFalse();
        assertThat(capturedResponse.getErrorMessage()).contains("Account validation failed");
    }
    private KafkaValidationRequest createValidationRequest(String documentNumber,
                                                           String phoneNumber,
                                                           String accountId) {
        return new KafkaValidationRequest(
            UUID.randomUUID().toString(),
            documentNumber,
            phoneNumber,
            accountId);
    }
    private Customer createCustomer(String id) {
        return new Customer(id,
            "John Doe",
            "12345678",
            CustomerType.PERSONAL,
            "john@example.com",
            "123456789",
            LocalDateTime.now(),
            null,
            "ACTIVE",
            false,
            false
        );
    }
    private Account createAccount(String id,
                                  String customerId,
                                  AccountType type) {
        return new Account(id,
            customerId,
            type,
            230.00,
            false,
            new BigDecimal("60"),
            false,
            new BigDecimal("49"),
            null,
            null,
            LocalDateTime.now(),
            null,
            4,
            new BigDecimal("3.24")
        );
    }
}