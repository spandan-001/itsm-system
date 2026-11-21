package com.itsm.repository;

import java.util.List;

import com.itsm.model.ITService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITServiceRepository
        extends JpaRepository<ITService, Long> {

    List<ITService> findByActiveTrueOrderByNameAscIdAsc();

}