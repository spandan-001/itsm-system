package com.itsm.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.dto.NoteRequests;
import com.itsm.dto.NoteRequests.NoteResponse;
import com.itsm.model.Incident;
import com.itsm.service.AccountService;
import com.itsm.service.IncidentService;
import com.itsm.service.note.NoteService;

/** The incident conversation thread and reporter feedback. */
@RestController
public class NoteController {

    private final NoteService noteService;
    private final IncidentService incidentService;
    private final AccountService accounts;

    public NoteController(NoteService noteService, IncidentService incidentService, AccountService accounts) {
        this.noteService = noteService;
        this.incidentService = incidentService;
        this.accounts = accounts;
    }

    /** Oldest first. Employees do not see internal investigation notes. */
    @GetMapping("/api/incidents/{id}/notes")
    public List<NoteResponse> notes(@PathVariable Long id) {
        return noteService.notesFor(id, accounts.requireCurrentAccount()).stream().map(NoteResponse::from).toList();
    }

    /**
     * Employees: {@code ADDITIONAL_INFO}, {@code INFO_RESPONSE}.
     * Technicians and admins: {@code INVESTIGATION} (internal), {@code INFO_REQUEST}.
     */
    @PostMapping("/api/incidents/{id}/notes")
    public NoteResponse addNote(@PathVariable Long id, @Valid @RequestBody NoteRequests.AddNote request) {
        return NoteResponse.from(noteService.addNote(id, request.type(), request.body(), accounts.requireCurrentAccount()));
    }

    /** The reporter rates the resolution 1-5, once, after it is resolved. */
    @PostMapping("/api/incidents/{id}/feedback")
    public Incident feedback(@PathVariable Long id, @Valid @RequestBody NoteRequests.Feedback request) {
        return incidentService.recordFeedback(id, request.rating(), request.comment(), accounts.requireCurrentAccount());
    }
}
