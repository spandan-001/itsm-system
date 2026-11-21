package com.itsm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** The REST API through MockMvc: workflow errors, validation, strategies, analytics. */
class ApiErrorHandlingTest extends IntegrationTestSupport {

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUpMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void illegalTransitionReturns409WithAllowedMoves() throws Exception {
        Long id = reportWifiOutage().getId(); // auto-assigned

        mvc.perform(patch("/api/incidents/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid state transition"))
                .andExpect(jsonPath("$.from").value("ASSIGNED"))
                .andExpect(jsonPath("$.allowedTransitions[0]").value("ACKNOWLEDGED"));
    }

    @Test
    void invalidCreateRequestReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/incidents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\", \"serviceIds\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.serviceIds").exists());
    }

    @Test
    void missingRecordsReturn404() throws Exception {
        mvc.perform(get("/api/incidents/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/incidents/999999/audit")).andExpect(status().isNotFound());
    }

    @Test
    void strategiesCanBeListedAndSwitched() throws Exception {
        mvc.perform(get("/api/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority.active").value("weighted"))
                .andExpect(jsonPath("$.assignment.active").value("smart"));
        mvc.perform(put("/api/strategies/assignment/load"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value("load"));
        mvc.perform(put("/api/strategies/assignment/smart")).andExpect(status().isOk());
        mvc.perform(put("/api/strategies/priority/unknown")).andExpect(status().isBadRequest());
    }

    @Test
    void incidentJsonExposesWorkflowAndRecommendations() throws Exception {
        Long id = reportWifiOutage().getId();

        mvc.perform(get("/api/incidents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.allowedTransitions[0]").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.assignedTechnician.skills[0]").value("NETWORK"));
        mvc.perform(get("/api/incidents/" + id + "/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ravi Kumar"));
        mvc.perform(get("/api/incidents/search").param("priority", "P1").param("keyword", "wi-fi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidents").value(1));
    }
}
