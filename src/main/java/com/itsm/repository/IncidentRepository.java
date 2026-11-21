package com.itsm.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.model.Incident;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Incident> findLockedById(Long id);

    // SLA breach detection: open clocks whose deadline has passed and are not yet flagged
    List<Incident> findByRespondedAtIsNullAndResponseBreachedFalseAndResponseDueAtBefore(LocalDateTime now);

    List<Incident> findByResolvedAtIsNullAndResolutionBreachedFalseAndResolutionDueAtBefore(LocalDateTime now);

    List<Incident> findByResponseBreachedTrueOrResolutionBreachedTrue();

    // Routing
    List<Incident> findByStatusOrderByCreatedAtAsc(IncidentStatus status);

    /** Per-technician count of incidents in the given statuses. */
    @Query("""
            select i.assignedTechnician.id as technicianId, count(i) as total
            from Incident i
            where i.assignedTechnician is not null and i.status in :statuses
            group by i.assignedTechnician.id""")
    List<TechnicianCount> countByTechnicianAndStatusIn(@Param("statuses") Collection<IncidentStatus> statuses);

    /** Per-technician count of resolved incidents in one category (their track record). */
    @Query("""
            select i.assignedTechnician.id as technicianId, count(i) as total
            from Incident i
            where i.assignedTechnician is not null and i.category = :category and i.resolvedAt is not null
            group by i.assignedTechnician.id""")
    List<TechnicianCount> countResolvedByTechnician(@Param("category") IncidentCategory category);

    // Duplicate detection
    List<Incident> findByCategoryAndStatusNotInAndDuplicateOfIsNullAndCreatedAtAfter(
            IncidentCategory category, Collection<IncidentStatus> excludedStatuses, LocalDateTime since);

    List<Incident> findByDuplicateOf_Id(Long originalId);

    interface TechnicianCount {
        Long getTechnicianId();

        Long getTotal();
    }
}
