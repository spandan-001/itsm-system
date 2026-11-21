package com.itsm.service;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.itsm.event.IncidentEvent;

/**
 * Who is performing the current action, for the audit trail: the linked ITSM
 * account's email, else the sign-in email, else the user id, else "system".
 */
@Component
public class CurrentActorProvider {

    private final AccountService accounts;

    public CurrentActorProvider(AccountService accounts) {
        this.accounts = accounts;
    }

    public String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return IncidentEvent.SYSTEM_ACTOR;
        }
        try {
            var linked = accounts.findLinkedAccount(UUID.fromString(jwt.getSubject()));
            if (linked.isPresent()) {
                return linked.get().email();
            }
        } catch (IllegalArgumentException notAUserId) {
            // fall through to the token's own claims
        }
        String email = jwt.getClaimAsString("email");
        return email != null ? email : jwt.getSubject();
    }
}
