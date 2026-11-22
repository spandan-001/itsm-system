package com.itsm.service.note;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.enums.IncidentStatus;
import com.itsm.enums.NoteType;
import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentNoteAddedEvent;
import com.itsm.exception.BusinessRuleException;
import com.itsm.exception.OperationNotPermittedException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.model.AccountLink.Role;
import com.itsm.model.Incident;
import com.itsm.model.IncidentNote;
import com.itsm.model.User;
import com.itsm.repository.EmployeeRepository;
import com.itsm.repository.IncidentNoteRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.TechnicianRepository;
import com.itsm.service.AccountService.LinkedAccount;

/**
 * The conversation on an incident: investigation notes, information requests
 * and replies, and the notes the system writes for resolutions and reopens.
 */
@Service
public class NoteService {

    private final IncidentNoteRepository notes;
    private final IncidentRepository incidents;
    private final EmployeeRepository employees;
    private final TechnicianRepository technicians;
    private final ApplicationEventPublisher eventPublisher;

    public NoteService(IncidentNoteRepository notes, IncidentRepository incidents, EmployeeRepository employees,
                       TechnicianRepository technicians, ApplicationEventPublisher eventPublisher) {
        this.notes = notes;
        this.incidents = incidents;
        this.employees = employees;
        this.technicians = technicians;
        this.eventPublisher = eventPublisher;
    }

    /**
     * A user posts to the thread.
     * @throws OperationNotPermittedException if their role may not post this type
     * @throws BusinessRuleException if the incident is closed or a linked duplicate
     */
    @Transactional
    public IncidentNote addNote(Long incidentId, NoteType type, String body, LinkedAccount account) {
        Incident incident = incidents.findLockedById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", incidentId));
        account.requireAccess(incident);
        if (!type.canBeWrittenBy(account.role())) {
            throw new OperationNotPermittedException(account.role() + " accounts cannot add " + type + " notes");
        }
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BusinessRuleException("Closed incidents cannot be changed");
        }
        if (incident.isDuplicate()) {
            throw new BusinessRuleException("Incident #" + incident.getId() + " is a duplicate of #"
                    + incident.getDuplicateOfId() + "; add notes to the original");
        }
        switch (type) {
            case INFO_REQUEST -> incident.requestInfoFromReporter();
            case INFO_RESPONSE, ADDITIONAL_INFO -> incident.reporterInfoReceived();
            default -> { }
        }
        return record(incident, type, body, account);
    }

    /** Writes a note on behalf of the system or a user (account may be null for the system). */
    @Transactional
    public IncidentNote record(Incident incident, NoteType type, String body, LinkedAccount account) {
        Author author = authorOf(account);
        IncidentNote note = notes.save(new IncidentNote(incident, type, author.name(), author.email(), author.role(), body));
        eventPublisher.publishEvent(new IncidentNoteAddedEvent(note, author.email()));
        return note;
    }

    /** The thread, oldest first. Employees do not see internal investigation notes. */
    @Transactional(readOnly = true)
    public List<IncidentNote> notesFor(Long incidentId, LinkedAccount account) {
        Incident incident = incidents.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", incidentId));
        account.requireAccess(incident);
        return notes.findByIncident_IdOrderByCreatedAtAscIdAsc(incidentId).stream()
                .filter(note -> account.role() != Role.EMPLOYEE || !note.getType().isInternal())
                .toList();
    }

    private Author authorOf(LinkedAccount account) {
        if (account == null) {
            return new Author("System", IncidentEvent.SYSTEM_ACTOR, "SYSTEM");
        }
        String fallback = account.email() == null ? "Administrator" : account.email();
        User user = switch (account.role()) {
            case EMPLOYEE -> employees.findById(account.id()).orElse(null);
            case TECHNICIAN -> technicians.findById(account.id()).orElse(null);
            case ADMIN -> null;
        };
        return user == null
                ? new Author(fallback, fallback, account.role().name())
                : new Author(user.getName(), user.getEmail(), account.role().name());
    }

    private record Author(String name, String email, String role) {
    }
}
