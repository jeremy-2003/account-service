package com.bank.accountservice.dto;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssociateAccountRequest {
    private String accountId;
}
