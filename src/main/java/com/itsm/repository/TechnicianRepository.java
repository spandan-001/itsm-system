package com.itsm.repository;

import com.itsm.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianRepository
        extends JpaRepository<Technician, Long> {

}