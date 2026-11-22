package com.itsm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Urgency;
import com.itsm.model.AccountLink.Role;
import com.itsm.model.Incident;
import com.itsm.service.AccountService.LinkedAccount;
import com.itsm.service.knowledge.ResolutionSuggestion;
import com.itsm.service.knowledge.ResolutionSuggestionService;

class ResolutionSuggestionIntegrationTest extends IntegrationTestSupport {

    @Autowired ResolutionSuggestionService suggestions;

    private Incident closeWithFix(String title, String description, IncidentCategory category, String fix, Integer rating) {
        Incident incident = report(title, description, category, Impact.MEDIUM, Urgency.MEDIUM);
        incidentService.updateStatus(incident.getId(), IncidentStatus.ACKNOWLEDGED);
        incidentService.updateStatus(incident.getId(), IncidentStatus.IN_PROGRESS);
        incidentService.updateStatus(incident.getId(), IncidentStatus.RESOLVED, fix);
        incidentService.updateStatus(incident.getId(), IncidentStatus.VERIFIED);
        incidentService.updateStatus(incident.getId(), IncidentStatus.CLOSED);
        if (rating != null) {
            incidentService.recordFeedback(incident.getId(), rating, null,
                    new LinkedAccount(Role.EMPLOYEE, reporter.getId(), reporter.getEmail()));
        }
        return incidentService.getIncident(incident.getId());
    }

    @Test
    void suggestsTheFixFromTheMostSimilarPastIncident() {
        closeWithFix("Library wireless access point offline", "Wi-Fi in the reading room stopped working",
                IncidentCategory.NETWORK, "Power-cycled access point AP-3 and updated firmware", 5);
        closeWithFix("Printer jammed", "Paper stuck in the second floor printer",
                IncidentCategory.HARDWARE, "Replaced the pickup roller", null);

        Incident current = reportWifiOutage(); // "Wi-Fi down in library"
        List<ResolutionSuggestion> found = suggestions.suggestFor(current);

        assertThat(found).isNotEmpty();
        assertThat(found.get(0).resolution()).contains("AP-3");
        assertThat(found.get(0).confirmed()).isTrue();
        assertThat(found.get(0).feedbackRating()).isEqualTo(5);
        assertThat(found).extracting(ResolutionSuggestion::resolution).noneMatch(r -> r.contains("pickup roller"));
    }

    @Test
    void betterRatedFixesRankHigherWhenTextIsEquallySimilar() {
        closeWithFix("Wi-Fi down in library", "No devices can connect", IncidentCategory.NETWORK, "Fix A", 2);
        closeWithFix("Wi-Fi down in library", "No devices can connect", IncidentCategory.NETWORK, "Fix B", 5);

        Incident current = report("Wi-Fi down in library again", "No devices can connect to the library Wi-Fi",
                IncidentCategory.NETWORK, Impact.HIGH, Urgency.HIGH);

        assertThat(suggestions.suggestFor(current)).extracting(ResolutionSuggestion::resolution)
                .startsWith("Fix B", "Fix A");
    }

    @Test
    void noSuggestionsWithoutRelatedHistory() {
        closeWithFix("Printer jammed", "Paper stuck in the second floor printer",
                IncidentCategory.HARDWARE, "Replaced the pickup roller", null);

        assertThat(suggestions.suggestFor(reportWifiOutage())).isEmpty();
    }
}
