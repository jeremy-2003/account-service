package com.bank.accountservice.service;

import com.bank.accountservice.client.CustomerEligibilityClientService;
import com.bank.accountservice.dto.BalancePrimaryAccount;
import com.bank.accountservice.model.debitcard.DebitCard;
import com.bank.accountservice.repository.AccountRepository;
import com.bank.accountservice.repository.DebitCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Service
public class DebitCardService {
    @Autowired
    private DebitCardRepository debitCardRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CustomerEligibilityClientService customerEligibilityClientService;

    public Mono<DebitCard> createDebitCard(String customerId, String primaryAccountId) {
        return customerEligibilityClientService.hasOverdueDebt(customerId)
                .flatMap(hasOverDueDebt -> {
                    if (hasOverDueDebt) {
                        return Mono.error(new RuntimeException(
                            "Customer has overdue debt and cannot create a new credit"));
                    }
                    return accountRepository.findById(primaryAccountId)
                            .filter(account -> account.getCustomerId().equals(customerId))
                            .switchIfEmpty(Mono.error(new RuntimeException("The main account does not " +
                                "belong to the client")))
                            .flatMap(account -> {
                                return generateCardNumber().flatMap(cardNumber -> {
                                    DebitCard newCard = new DebitCard();
                                    newCard.setCustomerId(customerId);
                                    newCard.setCardNumber(cardNumber);
                                    newCard.setStatus("ACTIVE");
                                    newCard.setPrimaryAccountId(primaryAccountId);
                                    newCard.setAssociatedAccountIds(new ArrayList<>(List.of(primaryAccountId)));
                                    newCard.setExpirationDate(LocalDateTime.now().plusYears(4));
                                    newCard.setCreatedAt(LocalDateTime.now());
                                    newCard.setModifiedAt(LocalDateTime.now());

                                    return debitCardRepository.save(newCard);
                                });
                            });
                });
    }

    public Mono<DebitCard> associateAccountToCard(String cardId, String accountId) {
        return Mono.zip(
                        debitCardRepository.findById(cardId),
                        accountRepository.findById(accountId)
                ).filter(tuple -> tuple.getT1().getCustomerId()
                    .equals(tuple.getT2().getCustomerId()))
                .switchIfEmpty(Mono.error(new RuntimeException("The card or account do " +
                    "not belong to the same customer")))
                .flatMap(tuple -> {
                    DebitCard card = tuple.getT1();
                    if (!card.getAssociatedAccountIds().contains(accountId)) {
                        card.getAssociatedAccountIds().add(accountId);
                        card.setModifiedAt(LocalDateTime.now());
                    }
                    return debitCardRepository.save(card);
                });
    }

    public Mono<DebitCard> changePrimaryAccount(String cardId, String newPrimaryAccountId) {
        return debitCardRepository.findById(cardId)
                .flatMap(card -> {
                    if (!card.getAssociatedAccountIds().contains(newPrimaryAccountId)) {
                        return Mono.error(new RuntimeException("The new main account must be" +
                            " associated with the card"));
                    }
                    card.setPrimaryAccountId(newPrimaryAccountId);
                    card.setModifiedAt(LocalDateTime.now());
                    return debitCardRepository.save(card);
                });
    }

    public Flux<DebitCard> getDebitCardsByCustomerId(String customerId) {
        return debitCardRepository.findByCustomerId(customerId);
    }

    public Mono<DebitCard> getDebitCardByCardNumber(String cardNumber) {
        return debitCardRepository.findByCardNumber(cardNumber);
    }
    public Flux<DebitCard> getDebitCardByPrimaryAccountId(String primaryAccountId) {
        return debitCardRepository.findByPrimaryAccountId(primaryAccountId);
    }
    public Mono<DebitCard> getDebitCardById(String cardId) {
        return debitCardRepository.findById(cardId);
    }
    public Flux<DebitCard> getDebitCardsByAccountId(String accountId) {
        return debitCardRepository.findByAssociatedAccountIdsContaining(accountId);
    }

    public Mono<DebitCard> updateCardStatus(String cardId, String newStatus) {
        return debitCardRepository.findById(cardId)
                .flatMap(card -> {
                    card.setStatus(newStatus);
                    card.setModifiedAt(LocalDateTime.now());
                    return debitCardRepository.save(card);
                });
    }

    public Mono<BalancePrimaryAccount> getBalancePrimaryAccount(String debitCardId) {
        return debitCardRepository.findById(debitCardId)
                .flatMap(debitCard -> accountRepository.findById(debitCard.getPrimaryAccountId())
                        .map(account -> BalancePrimaryAccount.builder()
                                .cardId(debitCard.getId())
                                .cardNumber(debitCard.getCardNumber())
                                .primaryAccountId(account.getId())
                                .balancePrimaryAccount(account.getBalance())
                                .build())
                );
    }

    public Mono<DebitCard> deleteDebitCard(String cardId) {
        return debitCardRepository.findById(cardId)
                .flatMap(card -> {
                    card.setStatus("DELETED");
                    card.setModifiedAt(LocalDateTime.now());
                    return debitCardRepository.save(card);
                });
    }

    private Mono<String> generateCardNumber() {
        StringBuilder numberBuilder = new StringBuilder("4");
        for (int i = 0; i < 14; i++) {
            numberBuilder.append((int) (Math.random() * 10));
        }
        String baseNumber = numberBuilder.toString();
        int checkDigit = calculateLuhnCheckDigit(baseNumber);
        String candidate = baseNumber + checkDigit;

        return debitCardRepository.findByCardNumber(candidate)
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        return generateCardNumber();
                    } else {
                        return Mono.just(candidate);
                    }
                });
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

}