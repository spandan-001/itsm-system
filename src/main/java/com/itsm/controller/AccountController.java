package com.itsm.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.model.AccountLink.Role;
import com.itsm.service.AccountService;
import com.itsm.service.AdminAccess;

@RestController
public class AccountController {

    private final AccountService accountService;

    private final AdminAccess admins;

    public AccountController(AccountService accountService, AdminAccess admins) {
        this.accountService = accountService;
        this.admins = admins;
    }

    @GetMapping("/api/me")
    public AccountResponse currentAccount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        boolean admin = admins.allows(jwt.getSubject());
        var account = accountService.findLinkedAccount(userId);
        if (account.isEmpty() && !admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This Supabase user is not linked to an ITSM account");
        }
        return new AccountResponse(userId, jwt.getClaimAsString("email"),
                account.map(AccountService.LinkedAccount::role).orElse(admin ? Role.ADMIN : null),
                account.map(AccountService.LinkedAccount::id).orElse(null), admin);
    }

    public record AccountResponse(UUID userId, String email, Role role, Long accountId, boolean admin) {}
}
