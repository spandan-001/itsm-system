package com.itsm.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.itsm.model.Incident;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Incident> findLockedById(Long id);

    // SLA breach detection: open clocks whose deadline has passed and are not yet flagged
    List<Incident> findByRespondedAtIsNullAndResponseBreachedFalseAndResponseDueAtBefore(LocalDateTime now);

    List<Incident> findByResolvedAtIsNullAndResolutionBreachedFalseAndResolutionDueAtBefore(LocalDateTime now);

    List<Incident> findByResponseBreachedTrueOrResolutionBreachedTrue();
}
