package com.itsm.service;

import org.springframework.stereotype.Component;

import com.itsm.event.IncidentEvent;

/**
 * Who is performing the current action, for the audit trail.
 *
 * <p>Returns "system" until sign-in exists. Once Google sign-in is in place,
 * change {@link #currentActor()} to return the signed-in user's email, e.g.
 * from {@code SecurityContextHolder.getContext().getAuthentication()}.
 */
@Component
public class CurrentActorProvider {

    public String currentActor() {
        return IncidentEvent.SYSTEM_ACTOR;
    }
}
