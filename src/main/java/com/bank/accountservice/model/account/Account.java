package com.bank.accountservice.model.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;
    private String customerId;
    private AccountType accountType;
    private double balance;
    private boolean isVipAccount; //Only for SAVINGS accounts
    private BigDecimal minBalanceRequirement; //Only if the account is VIP
    private boolean isPymAccount; //Only for CHECKING accounts
    private BigDecimal maintenanFee; //Is 0 only if the account is Pym
    private List<String> holders;
    private List<String> signers;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Integer maxFreeTransaction;
    private BigDecimal transactionCost;
}
