package com.itsm.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.itsm.enums.IncidentCategory;

public final class TechnicianUpdateRequests {

    private TechnicianUpdateRequests() {
    }

    public record Availability(@NotNull Boolean available) {
    }

    public record Skills(@NotNull Set<@NotNull IncidentCategory> skills) {
    }
}
