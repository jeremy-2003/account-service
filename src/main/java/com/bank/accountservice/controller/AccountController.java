package com.bank.accountservice.controller;
import com.bank.accountservice.dto.BaseResponse;
import com.bank.accountservice.model.account.Account;
import com.bank.accountservice.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    @PostMapping
    public Mono<ResponseEntity<BaseResponse<Account>>> createAccount(@RequestBody Account account) {
        return accountService.createAccount(account)
                .map(savedAccount -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(BaseResponse.<Account>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Account created successfully")
                                .data(savedAccount)
                                .build()))
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.<Account>builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .message(e.getMessage())
                                .data(null)
                                .build())));
    }
    @GetMapping("/{accountId}")
    public Mono<ResponseEntity<BaseResponse<Account>>> getAccountById(@PathVariable String accountId) {
        return accountService.getAccountById(accountId)
                .map(account -> ResponseEntity.ok(
                        BaseResponse.<Account>builder()
                                .status(HttpStatus.OK.value())
                                .message("Account retrieved successfully")
                                .data(account)
                                .build()))
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.<Account>builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .message("Account not found")
                                .data(null)
                                .build())));
    }
    @GetMapping("/customer/{customerId}")
    public Mono<ResponseEntity<BaseResponse<List<Account>>>> getAccountsByCustomer(@PathVariable String customerId) {
        return accountService.getAccountsByCustomer(customerId)
                .collectList()
                .map(accounts -> {
                    if (accounts.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(BaseResponse.<List<Account>>builder()
                                        .status(HttpStatus.NOT_FOUND.value())
                                        .message("No accounts found for the customer")
                                        .data(Collections.emptyList())
                                        .build());
                    } else {
                        return ResponseEntity.ok(
                                BaseResponse.<List<Account>>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("Account retrieved successfully")
                                        .data(accounts)
                                        .build());
                    }
                });
    }
    @PutMapping("/{accountId}")
    public Mono<ResponseEntity<BaseResponse<Account>>> updateAccount(@PathVariable String accountId,
                                                                     @RequestBody Account updatedAccount) {
        return accountService.updateAccount(accountId, updatedAccount)
                .map(account -> ResponseEntity.ok(
                        BaseResponse.<Account>builder()
                                .status(HttpStatus.OK.value())
                                .message("Account updated successfully")
                                .data(account)
                                .build()))
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.<Account>builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .message(e.getMessage())
                                .data(null)
                                .build())));
    }
    @PutMapping("/{accountId}/vip-pym/status")
    public Mono<ResponseEntity<BaseResponse<Account>>> updateVipPymStatus(@PathVariable String accountId,
                                                                          @RequestParam boolean isVipPym,
                                                                          @RequestParam String type) {
        return accountService.updateVipPymStatus(accountId, isVipPym, type)
                .map(updatedCustomer -> ResponseEntity.ok(
                        BaseResponse.<Account>builder()
                                .status(HttpStatus.OK.value())
                                .message("Account update successfully")
                                .data(updatedCustomer)
                                .build()))
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.<Account>builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .message("Account not found")
                                .data(null)
                                .build())));
    }
    @DeleteMapping("/{accountId}")
    public Mono<ResponseEntity<BaseResponse<Void>>> deleteAccount(@PathVariable String accountId) {
        return accountService.deleteAccount(accountId)
                .then(Mono.just(ResponseEntity.ok(
                        BaseResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("Account deleted successfully")
                                .data(null)
                                .build())))
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.<Void>builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .message(e.getMessage())
                                .data(null)
                                .build())));
    }
    @GetMapping
    public Mono<ResponseEntity<BaseResponse<List<Account>>>> findAllAccounts() {
        return accountService.findAllAccounts()
                .collectList()
                .map(accounts -> {
                    if (accounts.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(BaseResponse.<List<Account>>builder()
                                        .status(HttpStatus.NOT_FOUND.value())
                                        .message("No accounts found")
                                        .data(Collections.emptyList())
                                        .build());
                    } else {
                        return ResponseEntity.ok(
                                BaseResponse.<List<Account>>builder()
                                        .status(HttpStatus.OK.value())
                                        .message("Account retrieved successfully")
                                        .data(accounts)
                                        .build());
                    }
                });
    }
}