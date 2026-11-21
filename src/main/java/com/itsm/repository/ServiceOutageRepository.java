package com.itsm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itsm.model.ServiceOutage;

public interface ServiceOutageRepository extends JpaRepository<ServiceOutage, Long> {

    Optional<ServiceOutage> findFirstByService_IdAndStatus(Long serviceId, ServiceOutage.Status status);

    List<ServiceOutage> findTop20ByOrderByStatusAscDetectedAtDesc();

    long countByStatus(ServiceOutage.Status status);
}
