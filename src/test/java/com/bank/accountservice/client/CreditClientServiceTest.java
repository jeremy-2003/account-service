package com.bank.accountservice.client;

import com.bank.accountservice.model.creditcard.CreditCard;
import com.bank.accountservice.model.creditcard.CreditCardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    private CircuitBreaker circuitBreaker;
    private CreditClientService creditClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        // Mock para CircuitBreaker
        when(circuitBreakerRegistry.circuitBreaker("creditService")).thenReturn(circuitBreaker);
        when(circuitBreaker.getName()).thenReturn("creditService");
        when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        creditClientService = spy(new CreditClientService(webClientBuilder,
                "http://localhost:8080",
                circuitBreakerRegistry));
    }
    @Test
    void getCreditCardsByCustomer_Success() {
        // Arrange
        String customerId = "123";
        List<CreditCard> expectedCards = Arrays.asList(createCreditCard("1"), createCreditCard("2"));
        doReturn(Mono.just(expectedCards))
                .when(creditClientService)
                .getCreditCardsByCustomer(customerId);
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .expectNextMatches(cards -> cards.size() == 2 &&
                        cards.get(0).getId().equals("1") &&
                        cards.get(1).getId().equals("2"))
                .verifyComplete();
    }
    @Test
    void getCreditCardsByCustomer_EmptyResponse() {
        // Arrange
        String customerId = "123";
        doReturn(Mono.empty())
                .when(creditClientService)
                .getCreditCardsByCustomer(customerId);
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .verifyComplete();
    }
    @Test
    void getCreditCardsByCustomer_BadRequest() {
        // Arrange
        String customerId = "123";
        doReturn(Mono.empty())
                .when(creditClientService)
                .getCreditCardsByCustomer(customerId);
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .verifyComplete();
    }
    @Test
    void getCreditCardsByCustomer_OtherClientError() {
        // Arrange
        String customerId = "123";
        RuntimeException expectedError = new RuntimeException(
                "Credit service is unavailable for retrieving credit card information. " +
                        "Cannot continue with the operation.");
        doReturn(Mono.error(expectedError))
                .when(creditClientService)
                .getCreditCardsByCustomer(customerId);
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Credit service is unavailable"))
                .verify();
    }
    @Test
    void getCreditCardsByCustomer_ServerError() {
        // Arrange
        String customerId = "123";
        RuntimeException expectedError = new RuntimeException(
                "Credit service is unavailable for retrieving credit card information. " +
                        "Cannot continue with the operation.");
        doReturn(Mono.error(expectedError))
                .when(creditClientService)
                .getCreditCardsByCustomer(customerId);
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Credit service is unavailable"))
                .verify();
    }
    private CreditCard createCreditCard(String id) {
        CreditCard card = new CreditCard();
        card.setId(id);
        card.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
        card.setCreditLimit(new BigDecimal("1000"));
        return card;
    }
}