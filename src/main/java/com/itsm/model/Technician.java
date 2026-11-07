package com.itsm.model;

import jakarta.persistence.*;

@Entity
@Table(name = "technicians")
public class Technician extends User {

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false)
    private boolean available;

    protected Technician() {
        // Required by JPA
    }

    public Technician(String name, String email,
                      String domain, boolean available) {
        super(name, email);
        this.domain = domain;
        this.available = available;
    }

    public String getDomain() {
        return domain;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
