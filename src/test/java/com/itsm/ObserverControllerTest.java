package com.itsm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "itsm.sla.initial-delay-ms=3600000")
class ObserverControllerTest {

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void listsSeededSlaPolicies() throws Exception {
        mvc.perform(get("/api/sla/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].priority").value("P1"))
                .andExpect(jsonPath("$[0].responseMinutes").value(15));
    }

    @Test
    void rejectsInvalidSlaPolicyUpdate() throws Exception {
        mvc.perform(put("/api/sla/policies/P2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseMinutes\": 120, \"resolutionMinutes\": 60}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/sla/policies/P2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseMinutes\": -5, \"resolutionMinutes\": 60}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void auditTrailOfMissingIncidentIs404() throws Exception {
        mvc.perform(get("/api/incidents/999999/audit")).andExpect(status().isNotFound());
    }
}
