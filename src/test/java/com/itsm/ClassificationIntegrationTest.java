package com.itsm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.itsm.enums.AuditAction;
import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Urgency;
import com.itsm.model.AuditLog;
import com.itsm.model.Incident;
import com.itsm.service.audit.AuditService;
import com.itsm.service.classification.ClassificationService;

/** The classifier inside the real create flow: suggestions, auto-correct, learning from history. */
class ClassificationIntegrationTest extends IntegrationTestSupport {

    @Autowired ClassificationService classificationService;
    @Autowired AuditService auditService;

    @AfterEach
    void restoreSettings() {
        classificationService.configure(false, 0.85); // the test profile's default
    }

    private Incident wifiOutageFiledAsHardware() {
        return report("Campus wifi down in the library", "No one can connect to the wireless network",
                IncidentCategory.HARDWARE, Impact.HIGH, Urgency.HIGH);
    }

    @Test
    void suggestionIsRecordedButNotAppliedWhenAutoCorrectIsOff() {
        Incident incident = wifiOutageFiledAsHardware();

        assertThat(incident.getCategory()).isEqualTo(IncidentCategory.HARDWARE);
        assertThat(incident.getSuggestedCategory()).isEqualTo(IncidentCategory.NETWORK);
        assertThat(incident.getClassificationConfidence()).isGreaterThan(0.5);
        assertThat(incident.isAutoReclassified()).isFalse();
    }

    @Test
    void confidentSuggestionCorrectsTheCategoryBeforeRouting() {
        classificationService.configure(true, 0.6);

        Incident incident = wifiOutageFiledAsHardware();

        assertThat(incident.getCategory()).isEqualTo(IncidentCategory.NETWORK);
        assertThat(incident.getReportedCategory()).isEqualTo(IncidentCategory.HARDWARE);
        assertThat(incident.isAutoReclassified()).isTrue();
        // Routed using the corrected category: the network technician got it.
        assertThat(incident.getAssignedTechnician().getId()).isEqualTo(networkTech.getId());
        assertThat(auditService.trailFor(incident.getId()))
                .filteredOn(l -> l.getAction() == AuditAction.CREATED).extracting(AuditLog::getDetails)
                .singleElement().asString().contains("Reclassified from HARDWARE to NETWORK");
    }

    @Test
    void lowConfidenceSuggestionNeverOverridesTheReporter() {
        classificationService.configure(true, 1.0); // confidence is capped at 0.999

        Incident incident = wifiOutageFiledAsHardware();

        assertThat(incident.getCategory()).isEqualTo(IncidentCategory.HARDWARE);
    }

    @Test
    void modelLearnsFromResolvedIncidents() {
        int before = classificationService.retrain().historicalExamples();
        Long id = reportWifiOutage().getId();
        incidentService.updateStatus(id, IncidentStatus.ACKNOWLEDGED);
        incidentService.updateStatus(id, IncidentStatus.IN_PROGRESS);
        incidentService.updateStatus(id, IncidentStatus.RESOLVED, "Restarted the access point");
        incidentService.updateStatus(id, IncidentStatus.VERIFIED);
        incidentService.updateStatus(id, IncidentStatus.CLOSED); // closing triggers a retrain

        var stats = classificationService.stats();
        assertThat(stats.historicalExamples()).isEqualTo(before + 1);
        assertThat(stats.crossValidatedAccuracy()).isGreaterThan(0.75);
    }
}
