package com.bank.accountservice.event;

import com.bank.accountservice.client.CustomerClientService;
import com.bank.accountservice.dto.bootcoin.KafkaValidationRequest;
import com.bank.accountservice.dto.bootcoin.KafkaValidationResponse;
import com.bank.accountservice.model.account.AccountType;
import com.bank.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootCoinAssociationConsumer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AccountRepository accountRepository;
    private final CustomerClientService customerClientService;
    @KafkaListener(topics = "bootcoin.bank.account.association", groupId = "account-service-group")
    public void validateYankiAssociation(KafkaValidationRequest request) {
        validateAccount(request.getDocumentNumber(), request.getBankAccountId())
                .subscribe(isValid -> {
                    KafkaValidationResponse response = new KafkaValidationResponse(
                            request.getEventId(),
                            isValid,
                            isValid ? null : "Account validation failed"
                    );
                    kafkaTemplate.send("bootcoin.validation.response", request.getEventId(), response);
                }, error -> {
                        log.error("Error validating account: {}", error.getMessage());
                        KafkaValidationResponse response = new KafkaValidationResponse(
                            request.getEventId(),
                            false,
                            "Error during validation: " + error.getMessage()
                        );
                        kafkaTemplate.send("bootcoin.validation.response", request.getEventId(), response);
                    });
    }
    public Mono<Boolean> validateAccount(String documentNumber, String accountId) {
        return customerClientService.getCustomerByDocumentNumber(documentNumber)
                .flatMap(customer -> {
                    String customerId = customer.getId();
                    log.info("Customer found with ID: {}, validating account", customerId);
                    return accountRepository.findByIdAndCustomerId(accountId, customerId)
                            .map(account -> {
                                if (account.getAccountType() == AccountType.CHECKING
                                    || account.getAccountType() == AccountType.SAVINGS) {
                                    log.info("Account found and validated successfully");
                                    return true;
                                } else {
                                    log.info("Account found was not checking or saving");
                                    return false;
                                }
                            })
                            .defaultIfEmpty(false);
                })
                .onErrorResume(error -> {
                    log.error("Error during customer validation: {}", error.getMessage());
                    return Mono.just(false);
                });
    }
}
