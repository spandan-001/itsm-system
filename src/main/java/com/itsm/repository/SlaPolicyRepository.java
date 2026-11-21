package com.itsm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itsm.enums.Priority;
import com.itsm.model.SlaPolicy;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    Optional<SlaPolicy> findByPriority(Priority priority);
}
