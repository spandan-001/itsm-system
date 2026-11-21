package com.itsm.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.model.AccountLink.Role;
import com.itsm.service.AccountService;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/api/me")
    public AccountResponse currentAccount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var account = accountService.requireLinkedAccount(userId);
        return new AccountResponse(userId, jwt.getClaimAsString("email"), account.role(), account.id());
    }

    public record AccountResponse(UUID userId, String email, Role role, Long accountId) {}
}
