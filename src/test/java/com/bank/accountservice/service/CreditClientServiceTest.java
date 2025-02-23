package com.bank.accountservice.service;

import com.bank.accountservice.dto.BaseResponse;
import com.bank.accountservice.model.creditcard.CreditCard;
import com.bank.accountservice.model.creditcard.CreditCardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class CreditClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    private CreditClientService creditClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        creditClientService = new CreditClientService(webClientBuilder, "http://localhost:8080");
    }
    @Test
    void getCreditCardsByCustomer_Success() {
        // Arrange
        String customerId = "123";
        List<CreditCard> expectedCards = Arrays.asList(createCreditCard("1"), createCreditCard("2"));
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/credit-cards/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        BaseResponse<List<CreditCard>> response = new BaseResponse<>();
        response.setData(expectedCards);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .expectNextMatches(cards -> cards.size() == 2)
                .verifyComplete();
    }
    @Test
    void getCreditCardsByCustomer_EmptyResponse() {
        // Arrange
        String customerId = "123";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/credit-cards/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        BaseResponse<List<CreditCard>> response = new BaseResponse<>();
        response.setStatus(400);
        response.setMessage("This customer doesnt have credit cards");
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .verifyComplete();
    }
    @Test
    void getCreditCardsByCustomer_BadRequest() {
        // Arrange
        String customerId = "123";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/credit-cards/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<? extends Throwable>> errorHandler = invocation.getArgument(1);
            when(clientResponse.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);
            when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just("Invalid customer ID"));
            errorHandler.apply(clientResponse);
            return responseSpec;
        });
        // Mock
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .verifyComplete();
    }
    @Test
    void getCreditCardsByCustomer_OtherClientError() {
        // Arrange
        String customerId = "123";
        WebClientResponseException exception = new WebClientResponseException(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                null, null, null);
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/credit-cards/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(exception));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .expectError(WebClientResponseException.class)
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