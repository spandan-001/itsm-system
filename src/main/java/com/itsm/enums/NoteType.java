package com.itsm.enums;

import java.util.EnumSet;
import java.util.Set;

import com.itsm.model.AccountLink.Role;

/**
 * Kinds of entry on an incident's conversation thread. Each type knows who may
 * write it and whether reporters can see it, so the rules live in one place.
 */
public enum NoteType {
    /** Reporter adds detail on their own initiative. */
    ADDITIONAL_INFO(false, Role.EMPLOYEE),
    /** Reporter answers a technician's question. */
    INFO_RESPONSE(false, Role.EMPLOYEE),
    /** Technician asks the reporter for more information. */
    INFO_REQUEST(false, Role.TECHNICIAN, Role.ADMIN),
    /** Technician's working notes; hidden from the reporter. */
    INVESTIGATION(true, Role.TECHNICIAN, Role.ADMIN),
    /** Written by the system from the resolution summary. */
    RESOLUTION(false),
    /** Written by the system when a reporter reopens with a reason. */
    REOPEN_REASON(false),
    /** Written by the system from a comment sent with a status change. */
    STATUS_COMMENT(false);

    private final boolean internal;
    private final Set<Role> writers;

    NoteType(boolean internal, Role... writers) {
        this.internal = internal;
        this.writers = writers.length == 0 ? Set.of() : Set.copyOf(EnumSet.of(writers[0], writers));
    }

    /** Hidden from employees. */
    public boolean isInternal() {
        return internal;
    }

    /** Whether this role may post this type directly (system types cannot be posted). */
    public boolean canBeWrittenBy(Role role) {
        return writers.contains(role);
    }
}
