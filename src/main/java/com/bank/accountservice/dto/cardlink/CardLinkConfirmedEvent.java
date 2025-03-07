package com.bank.accountservice.dto.cardlink;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardLinkConfirmedEvent {
    private String phoneNumber;
    private String cardNumber;
    private String documentNumber;
    private BigDecimal updateBalance;
}
