package com.bank.accountservice.event;
import com.bank.accountservice.model.account.Account;
import com.bank.accountservice.model.account.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountEventProducerTest {
    @Mock
    private KafkaTemplate<String, Account> kafkaTemplate;
    private AccountEventProducer accountEventProducer;
    @BeforeEach
    void setUp() {
        accountEventProducer = new AccountEventProducer(kafkaTemplate);
    }
    @Test
    void publishAccountCreated_Success() {
        // Arrange
        Account account = createAccount("123");
        ListenableFuture<SendResult<String, Account>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("account-created", account.getId(), account))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, Account>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            successCallback.onSuccess(mock(SendResult.class));
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        accountEventProducer.publishAccountCreated(account);
        // Assert
        verify(kafkaTemplate).send("account-created", account.getId(), account);
    }
    @Test
    void publishAccountCreated_Error() {
        // Arrange
        Account account = createAccount("123");
        RuntimeException exception = new RuntimeException("Error sending message");
        ListenableFuture<SendResult<String, Account>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("account-created", account.getId(), account))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, Account>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            failureCallback.onFailure(exception);
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        accountEventProducer.publishAccountCreated(account);
        // Assert
        verify(kafkaTemplate).send("account-created", account.getId(), account);
    }
    @Test
    void publishAccountUpdate_Success() {
        // Arrange
        Account account = createAccount("123");
        ListenableFuture<SendResult<String, Account>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("account-updated", account.getId(), account))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, Account>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            successCallback.onSuccess(mock(SendResult.class));
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        accountEventProducer.publishAccountUpdate(account);
        // Assert
        verify(kafkaTemplate).send("account-updated", account.getId(), account);
    }
    @Test
    void publishAccountUpdate_Error() {
        // Arrange
        Account account = createAccount("123");
        RuntimeException exception = new RuntimeException("Error sending message");
        ListenableFuture<SendResult<String, Account>> future = mock(ListenableFuture.class);
        when(kafkaTemplate.send("account-updated", account.getId(), account))
                .thenReturn(future);
        doAnswer(invocation -> {
            SuccessCallback<SendResult<String, Account>> successCallback = invocation.getArgument(0);
            FailureCallback failureCallback = invocation.getArgument(1);
            failureCallback.onFailure(exception);
            return null;
        }).when(future).addCallback(any(SuccessCallback.class), any(FailureCallback.class));
        // Act
        accountEventProducer.publishAccountUpdate(account);
        // Assert
        verify(kafkaTemplate).send("account-updated", account.getId(), account);
    }
    private Account createAccount(String id) {
        Account account = new Account();
        account.setId(id);
        account.setBalance(1000.0);
        account.setAccountType(AccountType.SAVINGS);
        return account;
    }
}