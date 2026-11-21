package com.itsm.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.itsm.event.IncidentEvent;

/** Identifies the actor for incident audit events. */
@Component
public class CurrentActorProvider {

    public String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return IncidentEvent.SYSTEM_ACTOR;
    }
}
