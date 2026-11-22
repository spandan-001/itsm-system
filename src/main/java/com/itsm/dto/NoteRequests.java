package com.itsm.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.itsm.enums.NoteType;
import com.itsm.model.IncidentNote;

public final class NoteRequests {

    private NoteRequests() {
    }

    public record AddNote(@NotNull NoteType type, @NotBlank @Size(max = 4000) String body) {
    }

    public record Feedback(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 1000) String comment) {
    }

    public record NoteResponse(Long id, Long incidentId, NoteType type, boolean internal, String authorName,
                               String authorRole, String body, LocalDateTime createdAt) {

        public static NoteResponse from(IncidentNote note) {
            return new NoteResponse(note.getId(), note.getIncident().getId(), note.getType(),
                    note.getType().isInternal(), note.getAuthorName(), note.getAuthorRole(), note.getBody(),
                    note.getCreatedAt());
        }
    }
}
