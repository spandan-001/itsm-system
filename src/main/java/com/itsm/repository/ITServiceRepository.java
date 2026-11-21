package com.itsm.repository;

import com.itsm.model.ITService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITServiceRepository
        extends JpaRepository<ITService, Long> {

}