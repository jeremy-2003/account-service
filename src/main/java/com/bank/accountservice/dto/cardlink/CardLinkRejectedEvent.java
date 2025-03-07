package com.bank.accountservice.dto.cardlink;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardLinkRejectedEvent {
    private String phoneNumber;
    private String reason;
}
