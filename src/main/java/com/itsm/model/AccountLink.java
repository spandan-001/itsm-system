package com.itsm.model;

import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.Check;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_links")
@Check(constraints = "(employee_id IS NOT NULL AND technician_id IS NULL) OR "
        + "(employee_id IS NULL AND technician_id IS NOT NULL)")
public class AccountLink {

    @Id
    private UUID supabaseUserId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", unique = true)
    private Employee employee;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id", unique = true)
    private Technician technician;

    protected AccountLink() {}

    public AccountLink(UUID supabaseUserId, Employee employee) {
        this.supabaseUserId = Objects.requireNonNull(supabaseUserId);
        this.employee = Objects.requireNonNull(employee);
    }

    public AccountLink(UUID supabaseUserId, Technician technician) {
        this.supabaseUserId = Objects.requireNonNull(supabaseUserId);
        this.technician = Objects.requireNonNull(technician);
    }

    public Role role() {
        return employee == null ? Role.TECHNICIAN : Role.EMPLOYEE;
    }

    public Long accountId() {
        return employee == null ? technician.getId() : employee.getId();
    }

    public String email() {
        return employee == null ? technician.getEmail() : employee.getEmail();
    }

    public enum Role {
        EMPLOYEE,
        TECHNICIAN
    }
}
