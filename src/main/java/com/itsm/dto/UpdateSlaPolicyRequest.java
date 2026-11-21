package com.itsm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateSlaPolicyRequest(
        @NotNull @Positive Long responseMinutes,
        @NotNull @Positive Long resolutionMinutes) {
}
