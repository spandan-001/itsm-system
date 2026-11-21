package com.itsm.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    @GetMapping("/api/me")
    public AccountResponse currentAccount(@AuthenticationPrincipal Jwt jwt) {
        return new AccountResponse(jwt.getSubject(), jwt.getClaimAsString("email"));
    }

    public record AccountResponse(String userId, String email) {}
}
