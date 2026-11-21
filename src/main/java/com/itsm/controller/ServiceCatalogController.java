package com.itsm.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.model.ITService;
import com.itsm.repository.ITServiceRepository;
import com.itsm.service.AccountService;

@RestController
public class ServiceCatalogController {

    private final ITServiceRepository services;
    private final AccountService accounts;

    public ServiceCatalogController(ITServiceRepository services, AccountService accounts) {
        this.services = services;
        this.accounts = accounts;
    }

    @GetMapping("/api/services")
    public List<ITService> listActiveServices() {
        accounts.requireCurrentAccount();
        return services.findByActiveTrueOrderByNameAscIdAsc();
    }
}
