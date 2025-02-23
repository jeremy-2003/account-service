package com.bank.accountservice.event;

import com.bank.accountservice.model.account.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccountEventProducer {
    private final KafkaTemplate<String, Account> kafkaTemplate;

    public AccountEventProducer(KafkaTemplate<String, Account> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void publishAccountCreated(Account account) {
        kafkaTemplate.send("account-created", account.getId(), account)
                .addCallback(
                    result -> log.info("Account sent successfully"),
                    ex -> log.error("Failed to send message", ex));
    }
    public void publishAccountUpdate(Account account) {
        kafkaTemplate.send("account-updated", account.getId(), account)
                .addCallback(
                    result -> log.info("Account update successfully"),
                    ex -> log.error("Failed to send message", ex));
    }
}
