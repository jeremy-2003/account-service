package com.bank.accountservice.dto;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BalancePrimaryAccount {
    private String cardId;
    private String cardNumber;
    private String primaryAccountId;
    private double balancePrimaryAccount;
}
