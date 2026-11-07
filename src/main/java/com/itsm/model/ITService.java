package com.itsm.model;

import com.itsm.enums.BusinessCriticality;
import jakarta.persistence.*;

@Entity
@Table(name = "it_services")
public class ITService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessCriticality criticality;

    @Column(nullable = false)
    private boolean active;

    protected ITService() {
        // Required by JPA
    }

    public ITService(String name, String description,
                     BusinessCriticality criticality) {
        this.name = name;
        this.description = description;
        this.criticality = criticality;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BusinessCriticality getCriticality() {
        return criticality;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
