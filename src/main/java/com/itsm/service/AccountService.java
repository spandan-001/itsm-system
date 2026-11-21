package com.itsm.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.itsm.model.AccountLink.Role;
import com.itsm.model.Incident;
import com.itsm.repository.AccountLinkRepository;

@Service
public class AccountService {

    private final AccountLinkRepository accountLinks;

    public AccountService(AccountLinkRepository accountLinks) {
        this.accountLinks = accountLinks;
    }

    @Transactional(readOnly = true)
    public LinkedAccount requireLinkedAccount(UUID userId) {
        return findLinkedAccount(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "This Supabase user is not linked to an ITSM account"));
    }

    @Transactional(readOnly = true)
    public Optional<LinkedAccount> findLinkedAccount(UUID userId) {
        return accountLinks.findById(userId)
                .map(link -> new LinkedAccount(link.role(), link.accountId(), link.email()));
    }

    @Transactional(readOnly = true)
    public LinkedAccount requireCurrentAccount() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue");
        }
        return requireLinkedAccount(UUID.fromString(jwt.getSubject()));
    }

    public record LinkedAccount(Role role, Long id, String email) {
        public boolean canView(Incident incident) {
            if (role == Role.EMPLOYEE) {
                return id.equals(incident.getReporter().getId());
            }
            var technician = incident.getAssignedTechnician();
            return technician != null && id.equals(technician.getId());
        }

        public void requireAccess(Incident incident) {
            if (!canView(incident)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
            }
        }
    }
}
