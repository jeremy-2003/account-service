package com.bank.accountservice.event;

import com.bank.accountservice.client.CustomerClientService;
import com.bank.accountservice.dto.cardlink.CardLinkConfirmedEvent;
import com.bank.accountservice.dto.cardlink.CardLinkRejectedEvent;
import com.bank.accountservice.dto.cardlink.CardLinkRequestedEvent;
import com.bank.accountservice.repository.AccountRepository;
import com.bank.accountservice.repository.DebitCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardLinkConsumer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DebitCardRepository debitCardRepository;
    private final CustomerClientService customerClientService;
    private final AccountRepository accountRepository;

    @KafkaListener(topics = "yanki.card.link.requested", groupId = "account-service-group")
    public void processCardLinkRequest(CardLinkRequestedEvent event) {
        log.info("Processing card link request: {}", event);
        customerClientService.getCustomerByDocumentNumber(event.getDocumentNumber())
                .switchIfEmpty(Mono.error(new RuntimeException("Customer not found")))
                .flatMap(customer -> debitCardRepository.findByCardNumber(event.getCardNumber())
                                .switchIfEmpty(Mono.error(new RuntimeException("Debit card not found")))
                                .flatMap(debitCard -> accountRepository
                                    .findById(debitCard.getPrimaryAccountId())
                                                .switchIfEmpty(Mono.error(new RuntimeException("Primary account" +
                                                    " not found")))
                                                .flatMap(account -> {
                                                    if (!debitCard.getCardNumber().equals(event.getCardNumber())) {
                                                        CardLinkRejectedEvent rejectedEvent = new CardLinkRejectedEvent(
                                                                event.getPhoneNumber(), "Card does not " +
                                                                    "belong to the user"
                                                        );
                                                        return sendEvent("yanki.card.link.rejected", rejectedEvent);
                                                    }
                                                    BigDecimal updatedBalance = BigDecimal.valueOf(
                                                        account.getBalance()).add(event.getCurrentBalance());
                                                    double updatedBalanceAsDouble = updatedBalance.doubleValue();
                                                    account.setBalance(updatedBalanceAsDouble);
                                                    return accountRepository.save(account)
                                                            .flatMap(savedAccount -> {
                                                                CardLinkConfirmedEvent confirmedEvent =
                                                                    new CardLinkConfirmedEvent(
                                                                        event.getPhoneNumber(), event.getCardNumber(),
                                                                        event.getDocumentNumber(), updatedBalance
                                                                );
                                                                return sendEvent("yanki.card.link.confirmed",
                                                                        confirmedEvent)
                                                                        .doOnSuccess(unused -> log.info("Association" +
                                                                            " confirmed: {}", confirmedEvent));
                                                            });
                                                })
                                )
                )
                .doOnError(error -> log.error("Error processing card link request: {}", error.getMessage()))
                .subscribe();
    }

    private <T> Mono<Void> sendEvent(String topic, T event) {
        return Mono.fromRunnable(() -> kafkaTemplate.send(topic, event));
    }
}
