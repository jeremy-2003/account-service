package com.bank.accountservice.dto;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateCardRequest {
    private String customerId;
    private String primaryAccountId;
}
