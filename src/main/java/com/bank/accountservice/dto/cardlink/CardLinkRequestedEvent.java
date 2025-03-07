package com.bank.accountservice.dto.cardlink;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardLinkRequestedEvent {
    private String phoneNumber;
    private String cardNumber;
    private String documentNumber;
    private BigDecimal currentBalance;
}
