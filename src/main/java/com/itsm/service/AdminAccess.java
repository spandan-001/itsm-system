package com.itsm.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminAccess {

    private final Set<UUID> administrators;

    public AdminAccess(@Value("${itsm.auth.admin-user-ids:}") Set<UUID> administrators) {
        this.administrators = Set.copyOf(administrators);
    }

    public boolean allows(String subject) {
        return subject != null && administrators.contains(UUID.fromString(subject));
    }
}
