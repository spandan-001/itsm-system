package com.itsm.model;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class Employee extends User {

    @Column(nullable = false)
    private String department;

    protected Employee() {
        // Required by JPA
    }

    public Employee(String name, String email, String department) {
        super(name, email);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
