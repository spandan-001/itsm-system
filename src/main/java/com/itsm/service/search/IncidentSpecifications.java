package com.itsm.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import com.itsm.model.Incident;
import com.itsm.model.AccountLink.Role;
import com.itsm.service.AccountService.LinkedAccount;

/** Builds a JPA Specification (dynamic WHERE clause) from search criteria. */
public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    public static Specification<Incident> visibleTo(LinkedAccount account) {
        String owner = account.role() == Role.EMPLOYEE ? "reporter" : "assignedTechnician";
        return (root, query, cb) -> cb.equal(root.get(owner).get("id"), account.id());
    }

    public static Specification<Incident> matching(IncidentSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (c.status() != null && !c.status().isEmpty()) {
                where.add(root.get("status").in(c.status()));
            }
            if (c.priority() != null && !c.priority().isEmpty()) {
                where.add(root.get("priority").in(c.priority()));
            }
            if (c.category() != null) {
                where.add(cb.equal(root.get("category"), c.category()));
            }
            if (c.technicianId() != null) {
                where.add(cb.equal(root.get("assignedTechnician").get("id"), c.technicianId()));
            }
            if (Boolean.TRUE.equals(c.unassigned())) {
                where.add(cb.isNull(root.get("assignedTechnician")));
            }
            if (c.reporterId() != null) {
                where.add(cb.equal(root.get("reporter").get("id"), c.reporterId()));
            }
            if (c.serviceId() != null) {
                where.add(cb.equal(root.join("affectedServices").get("id"), c.serviceId()));
                query.distinct(true);
            }
            if (c.createdFrom() != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("createdAt"), c.createdFrom().atStartOfDay()));
            }
            if (c.createdTo() != null) {
                where.add(cb.lessThan(root.get("createdAt"), c.createdTo().plusDays(1).atStartOfDay()));
            }
            if (c.keyword() != null && !c.keyword().isBlank()) {
                String pattern = "%" + c.keyword().trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                where.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\')));
            }
            if (c.slaBreached() != null) {
                Predicate breached = cb.or(cb.isTrue(root.get("responseBreached")), cb.isTrue(root.get("resolutionBreached")));
                where.add(c.slaBreached() ? breached : cb.not(breached));
            }
            if (c.duplicate() != null) {
                where.add(c.duplicate() ? cb.isNotNull(root.get("duplicateOf")) : cb.isNull(root.get("duplicateOf")));
            }
            return cb.and(where.toArray(new Predicate[0]));
        };
    }
}
