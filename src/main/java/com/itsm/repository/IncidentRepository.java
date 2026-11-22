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

    @Query("""
            select i.id from Incident i
            where (i.respondedAt is null and i.responseBreached = false and i.responseDueAt < :now)
               or (i.resolvedAt is null and i.resolutionBreached = false and i.resolutionDueAt < :now)
            order by i.id""")
    List<Long> findOverdueIds(@Param("now") LocalDateTime now);

    List<Incident> findByResponseBreachedTrueOrResolutionBreachedTrue();

    /** Incidents with an open SLA clock that has not been warned about or breached yet. */
    @Query("""
            select i.id from Incident i
            where (i.respondedAt is null and i.responseDueAt is not null
                   and i.responseWarned = false and i.responseBreached = false)
               or (i.resolvedAt is null and i.resolutionDueAt is not null
                   and i.resolutionWarned = false and i.resolutionBreached = false)
            order by i.id""")
    List<Long> findWarningCandidateIds();

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

    /** Unacknowledged, non-duplicate incidents that could still escalate. */
    @Query("""
            select i.id from Incident i
            where i.respondedAt is null and i.responseDueAt is not null and i.duplicateOf is null
              and i.priority in :priorities and i.status in :statuses
              and i.escalationLevel <> com.itsm.enums.EscalationLevel.IT_MANAGER
            order by i.id""")
    List<Long> findEscalationCandidateIds(@Param("priorities") Collection<com.itsm.enums.Priority> priorities,
                                          @Param("statuses") Collection<IncidentStatus> statuses);

    /** Open incidents (duplicates included: they are evidence too) on a service, reported since a time. */
    @Query("""
            select i from Incident i join i.affectedServices s
            where s.id = :serviceId and i.createdAt >= :since and i.status not in :closed
            order by i.createdAt""")
    List<Incident> findRecentOpenOnService(@Param("serviceId") Long serviceId, @Param("since") LocalDateTime since,
                                           @Param("closed") Collection<IncidentStatus> closed);

    @Query("select count(i) from Incident i where i.outage.id = :outageId and i.status not in :closed")
    long countOpenInOutage(@Param("outageId") Long outageId, @Param("closed") Collection<IncidentStatus> closed);

    // Suggested resolutions: incidents with a recorded fix
    List<Incident> findByResolutionSummaryIsNotNullAndDuplicateOfIsNull();

    interface TechnicianCount {
        Long getTechnicianId();

        Long getTotal();
    }
}
