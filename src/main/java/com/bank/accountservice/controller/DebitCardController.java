package com.bank.accountservice.controller;

import com.bank.accountservice.dto.*;
import com.bank.accountservice.model.debitcard.DebitCard;
import com.bank.accountservice.service.DebitCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/debit-cards")
public class DebitCardController {
    @Autowired
    private DebitCardService debitCardService;

    @PostMapping
    public Mono<ResponseEntity<BaseResponse<DebitCard>>> createDebitCard(@RequestBody CreateCardRequest request) {
        return debitCardService.createDebitCard(request.getCustomerId(), request.getPrimaryAccountId())
                .map(card -> ResponseEntity.ok(
                        BaseResponse.<DebitCard>builder()
                                .status(HttpStatus.OK.value())
                                .message("Debit Card created successfully")
                                .data(card)
                                .build()))
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.<DebitCard>builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .message(e.getMessage())
                                .data(null)
                                .build())));
    }
    @GetMapping("/customer/{customerId}")
    public Mono<ResponseEntity<BaseResponse<List<DebitCard>>>> getDebitCardsByCustomer(
            @PathVariable String customerId) {
        return debitCardService.getDebitCardsByCustomerId(customerId)
                .collectList()
                .map(debitCards -> {
                    if (debitCards.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(BaseResponse.<List<DebitCard>>builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message("No debit cards found for the customer")
                            .data(Collections.emptyList())
                            .build());
                    } else {
                        return ResponseEntity.ok(
                            BaseResponse.<List<DebitCard>>builder()
                            .status(HttpStatus.OK.value())
                            .message("Account retrieved successfully")
                            .data(debitCards)
                            .build());
                    }
                });
    }
    @GetMapping("/{cardId}")
    public Mono<ResponseEntity<BaseResponse<DebitCard>>> getDebitCardById(@PathVariable String cardId) {
        return debitCardService.getDebitCardById(cardId)
                .map(card -> ResponseEntity.ok(
                        new BaseResponse<>(
                                HttpStatus.OK.value(),
                                "Debit card successfully obtained",
                                card
                        )))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new BaseResponse<>(
                                HttpStatus.NOT_FOUND.value(),
                                "Debit card not found",
                                null
                        ))));
    }
    @GetMapping("/account/primary-balance/{cardId}")
    public Mono<ResponseEntity<BaseResponse<BalancePrimaryAccount>>> getBalancePrimaryAccount(
            @PathVariable String cardId) {
        return debitCardService.getBalancePrimaryAccount(cardId)
                .map(card -> ResponseEntity.ok(
                        new BaseResponse<>(
                                HttpStatus.OK.value(),
                                "Balance primary account of the debit card successfully obtained",
                                card
                        )))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new BaseResponse<>(
                                HttpStatus.NOT_FOUND.value(),
                                "Debit card or primary account not found",
                                null
                        ))));
    }
    @PostMapping("/{cardId}/accounts")
    public Mono<ResponseEntity<BaseResponse<DebitCard>>> associateAccount(
            @PathVariable String cardId,
            @RequestBody AssociateAccountRequest request) {
        return debitCardService.associateAccountToCard(cardId, request.getAccountId())
                .map(card -> ResponseEntity.ok(
                        new BaseResponse<>(
                                HttpStatus.OK.value(),
                                "Account successfully associated with the card",
                                card
                        )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(
                                HttpStatus.BAD_REQUEST.value(),
                                e.getMessage(),
                                null
                        ))));
    }
    @PutMapping("/{cardId}/primary-account")
    public Mono<ResponseEntity<BaseResponse<DebitCard>>> changePrimaryAccount(
            @PathVariable String cardId,
            @RequestBody AssociateAccountRequest request) {
        return debitCardService.changePrimaryAccount(cardId, request.getAccountId())
                .map(card -> ResponseEntity.ok(
                        new BaseResponse<>(
                                HttpStatus.OK.value(),
                                "Main account successfully updated",
                                card
                        )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(
                                HttpStatus.BAD_REQUEST.value(),
                                e.getMessage(),
                                null
                        ))));
    }
    @PutMapping("/{cardId}/status")
    public Mono<ResponseEntity<BaseResponse<DebitCard>>> updateCardStatus(
            @PathVariable String cardId,
            @RequestBody UpdateStatusRequest request) {
        return debitCardService.updateCardStatus(cardId, request.getStatus())
                .map(card -> ResponseEntity.ok(
                        new BaseResponse<>(
                                HttpStatus.OK.value(),
                                "Card status updated successfully",
                                card
                        )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(
                                HttpStatus.BAD_REQUEST.value(),
                                e.getMessage(),
                                null
                        ))));
    }
    @GetMapping("/by-account/{accountId}")
    public Mono<ResponseEntity<BaseResponse<List<DebitCard>>>> getDebitCardsByAccount(@PathVariable String accountId) {
        return debitCardService.getDebitCardsByAccountId(accountId)
                .collectList()
                .map(debitCards -> {
                    if (debitCards.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(BaseResponse.<List<DebitCard>>builder()
                                        .status(HttpStatus.NOT_FOUND.value())
                                        .message("No debit cards found for the account")
                                        .data(Collections.emptyList())
                                        .build());
                    } else {
                        return ResponseEntity.ok(
                                BaseResponse.<List<DebitCard>>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("Debit cards retrieved successfully")
                                        .data(debitCards)
                                        .build());
                    }
                });
    }
    @DeleteMapping("/{cardId}")
    public Mono<ResponseEntity<BaseResponse<DebitCard>>> deleteDebitCard(@PathVariable String cardId) {
        return debitCardService.deleteDebitCard(cardId)
                .map(card -> ResponseEntity.ok(
                        new BaseResponse<>(
                                HttpStatus.OK.value(),
                                "Debit card successfully deleted",
                                card
                        )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new BaseResponse<>(
                                HttpStatus.BAD_REQUEST.value(),
                                e.getMessage(),
                                null
                        ))));
    }
}
