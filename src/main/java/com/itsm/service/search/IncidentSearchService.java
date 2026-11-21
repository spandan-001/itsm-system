package com.itsm.service.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.model.Incident;
import com.itsm.repository.IncidentRepository;
import com.itsm.service.AccountService.LinkedAccount;

@Service
public class IncidentSearchService {

    private final IncidentRepository incidentRepository;

    public IncidentSearchService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public Page<Incident> search(IncidentSearchCriteria criteria, Pageable pageable, LinkedAccount account) {
        return incidentRepository.findAll(IncidentSpecifications.visibleTo(account)
                .and(IncidentSpecifications.matching(criteria)), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Incident> search(IncidentSearchCriteria criteria, Pageable pageable) {
        return incidentRepository.findAll(IncidentSpecifications.matching(criteria), pageable);
    }
}
