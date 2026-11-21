package com.itsm.dto;

import com.itsm.enums.Priority;
import com.itsm.model.SlaPolicy;

public record SlaPolicyResponse(Priority priority, long responseMinutes, long resolutionMinutes) {

    public static SlaPolicyResponse from(SlaPolicy policy) {
        return new SlaPolicyResponse(policy.getPriority(), policy.getResponseMinutes(), policy.getResolutionMinutes());
    }
}
