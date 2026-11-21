package com.itsm.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itsm.model.AccountLink;

public interface AccountLinkRepository extends JpaRepository<AccountLink, UUID> {
}
