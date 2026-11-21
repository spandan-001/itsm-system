package com.itsm.dto;
 


import com.itsm.enums.Impact;
import com.itsm.enums.Urgency;
import com.itsm.enums.IncidentCategory;


import java.util.List;


public class CreateIncidentRequest {

    private String title;
    private String description;

    private IncidentCategory category;
    private Impact impact;
    private Urgency urgency;

    private Long reporterId;
    private List<Long> serviceIds;

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public IncidentCategory getCategory() {
        return category;
    }
    public void setCategory(IncidentCategory category) {
        this.category = category;
    }
    public Impact getImpact() {
        return impact;
    }
    public void setImpact(Impact impact) {
        this.impact = impact;
    }
    public Urgency getUrgency() {
        return urgency;
    }
    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }
    public Long getReporterId() {
        return reporterId;
    }
    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }
    public List<Long> getServiceIds() {
        return serviceIds;
    }
    public void setServiceIds(List<Long> serviceIds) {
        this.serviceIds = serviceIds;
    }

    



    
}
