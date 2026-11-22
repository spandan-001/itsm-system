package com.itsm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itsm.model.IncidentNote;

public interface IncidentNoteRepository extends JpaRepository<IncidentNote, Long> {

    List<IncidentNote> findByIncident_IdOrderByCreatedAtAscIdAsc(Long incidentId);
}
