package com.itsm;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.itsm.model.AccountLink;
import com.itsm.enums.BusinessCriticality;
import com.itsm.enums.NotificationChannel;
import com.itsm.model.Employee;
import com.itsm.model.ITService;
import com.itsm.model.Notification;
import com.itsm.model.Technician;
import com.itsm.repository.AccountLinkRepository;
import com.itsm.repository.EmployeeRepository;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.NotificationRepository;
import com.itsm.repository.TechnicianRepository;

@SpringBootTest(properties = "itsm.auth.admin-user-ids=f4db796e-a659-44dd-9dc9-2d28c3f7964a")
@ActiveProfiles("test")
@Transactional
class AccountControllerTest {

    private static final String USER_ID = "e304dc94-6169-4263-985a-c822e6481096";
    private static final String TECHNICIAN_ID = "17d39eda-4592-4704-b9b9-f2cea2346945";
    private static final String ADMIN_ID = "f4db796e-a659-44dd-9dc9-2d28c3f7964a";

    @Autowired WebApplicationContext context;
    @Autowired AccountLinkRepository accountLinks;
    @Autowired EmployeeRepository employees;
    @Autowired TechnicianRepository technicians;
    @Autowired ITServiceRepository services;
    @Autowired IncidentRepository incidents;
    @Autowired NotificationRepository notifications;
    @MockitoBean JwtDecoder jwtDecoder;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", context))
                .build();
    }

    @Test
    void requiresValidBearerToken() throws Exception {
        when(jwtDecoder.decode("invalid")).thenThrow(new BadJwtException("Invalid token"));

        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsLinkedEmployee() throws Exception {
        var employee = linkTestEmployee();
        stubVerifiedToken();

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.email").value("reporter@example.test"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.accountId").value(employee.getId()));
    }

    @Test
    void returnsLinkedTechnician() throws Exception {
        var technician = technicians.save(new Technician("Test Technician", "technician@example.test", "NETWORK", true));
        accountLinks.saveAndFlush(new AccountLink(UUID.fromString(USER_ID), technician));
        stubVerifiedToken();

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.accountId").value(technician.getId()));
    }

    @Test
    void rejectsVerifiedUserWithoutAccountLink() throws Exception {
        stubVerifiedToken();

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsIncidentAsLinkedEmployeeEvenIfRequestSuppliesAnotherReporter() throws Exception {
        var employee = linkTestEmployee();
        var otherEmployee = employees.save(new Employee("Other Employee", "other@example.test", "Support"));
        var service = services.save(new ITService("Test network", "Wi-Fi", BusinessCriticality.HIGH));
        stubVerifiedToken();

        mvc.perform(createIncident("""
                {"title":"Network outage","description":"Cannot connect","category":"NETWORK",
                 "impact":"HIGH","urgency":"HIGH","serviceIds":[%d],"reporterId":%d}
                """.formatted(service.getId(), otherEmployee.getId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporter.id").value(employee.getId()));
    }

    @Test
    void rejectsCreateRequestsWithoutLinkedEmployee() throws Exception {
        String body = """
                {"title":"Network outage","description":"Cannot connect","category":"NETWORK",
                 "impact":"HIGH","urgency":"HIGH","serviceIds":[1]}
                """;
        mvc.perform(createIncident(body)).andExpect(status().isUnauthorized());

        stubVerifiedToken();
        mvc.perform(createIncident(body).header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isForbidden());

        var technician = technicians.save(new Technician("Test Technician", "technician@example.test", "NETWORK", true));
        accountLinks.saveAndFlush(new AccountLink(UUID.fromString(USER_ID), technician));
        mvc.perform(createIncident(body).header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCreateRequestReturnsFieldErrors() throws Exception {
        linkTestEmployee();
        stubVerifiedToken();
        mvc.perform(createIncident("{\"title\": \"\", \"serviceIds\": []}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.serviceIds").exists());
    }

    private Employee linkTestEmployee() {
        var employee = employees.save(new Employee("Test Employee", "employee@example.test", "Support"));
        accountLinks.saveAndFlush(new AccountLink(UUID.fromString(USER_ID), employee));
        return employee;
    }

    private MockHttpServletRequestBuilder createIncident(String body) {
        return post("/api/incidents").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void employeeAndAssignedTechnicianCompleteIncidentAndRecordAuditActors() throws Exception {
        var employee = linkTestEmployee();
        var technician = technicians.save(new Technician("Assigned Technician", "assigned@example.test", "NETWORK", true));
        accountLinks.saveAndFlush(new AccountLink(UUID.fromString(TECHNICIAN_ID), technician));
        var service = services.save(new ITService("Workflow network", "Wi-Fi", BusinessCriticality.HIGH));
        stubVerifiedToken();
        stubToken("technician", TECHNICIAN_ID);

        mvc.perform(get("/api/services").header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(service.getId()));
        mvc.perform(createIncident("""
                {"title":"Network outage","description":"Cannot connect","category":"NETWORK",
                 "impact":"HIGH","urgency":"HIGH","serviceIds":[%d]}
                """.formatted(service.getId())).header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTechnician.id").value(technician.getId()));
        Long id = incidents.findAll().stream()
                .filter(incident -> incident.getReporter().getId().equals(employee.getId()))
                .findFirst().orElseThrow().getId();
        var unrelated = employees.save(new Employee("Other Employee", "unrelated@example.test", "Finance"));
        var unrelatedUserId = UUID.fromString("4e8b798a-40fa-4373-8587-f657dd83932f");
        accountLinks.saveAndFlush(new AccountLink(unrelatedUserId, unrelated));
        stubToken("unrelated", unrelatedUserId.toString());

        mvc.perform(get("/api/incidents").header(HttpHeaders.AUTHORIZATION, "Bearer unrelated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/incidents/search").param("reporterId", employee.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer unrelated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/incidents/{id}", id).header(HttpHeaders.AUTHORIZATION, "Bearer unrelated"))
                .andExpect(status().isNotFound());
        mvc.perform(changeStatus(id, "ACKNOWLEDGED", "unrelated")).andExpect(status().isNotFound());

        mvc.perform(changeStatus(id, "ACKNOWLEDGED", "verified")).andExpect(status().isForbidden());
        for (String status : new String[]{"ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED"}) {
            mvc.perform(changeStatus(id, status, "technician")).andExpect(status().isOk());
        }
        for (String status : new String[]{"VERIFIED", "CLOSED"}) {
            mvc.perform(changeStatus(id, status, "verified")).andExpect(status().isOk());
        }

        mvc.perform(get("/api/incidents/{id}/sla", id).header(HttpHeaders.AUTHORIZATION, "Bearer technician"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respondedAt").exists())
                .andExpect(jsonPath("$.resolvedAt").exists());
        mvc.perform(get("/api/incidents/{id}/audit", id).header(HttpHeaders.AUTHORIZATION, "Bearer technician"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actor").value("employee@example.test"))
                .andExpect(jsonPath("$[3].actor").value("assigned@example.test"));
    }

    @Test
    void scopesNotificationsAndAdministrativeEndpoints() throws Exception {
        var employee = linkTestEmployee();
        var own = notifications.save(new Notification("Employee", employee.getEmail(),
                NotificationChannel.IN_APP, "Mine", "Test", null));
        var other = notifications.save(new Notification("Other", "other@example.test",
                NotificationChannel.IN_APP, "Other", "Test", null));
        stubVerifiedToken();
        stubToken("admin", ADMIN_ID);

        mvc.perform(get("/api/notifications").param("email", "other@example.test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(own.getId()));
        mvc.perform(patch("/api/notifications/{id}/read", other.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/notifications/{id}/read", own.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        mvc.perform(get("/api/audit")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/audit").header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/incidents/similar").header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").header(HttpHeaders.AUTHORIZATION, "Bearer admin"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/incidents").header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/incidents").header(HttpHeaders.AUTHORIZATION, "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value(true));
    }

    @Test
    void techniciansCanUpdateOnlyTheirOwnAvailability() throws Exception {
        var technician = technicians.save(new Technician("Technician", "tech@example.test", "NETWORK", true));
        var other = technicians.save(new Technician("Other Technician", "other-tech@example.test", "HARDWARE", true));
        accountLinks.saveAndFlush(new AccountLink(UUID.fromString(TECHNICIAN_ID), technician));
        stubToken("technician", TECHNICIAN_ID);

        mvc.perform(get("/api/technicians/me").header(HttpHeaders.AUTHORIZATION, "Bearer technician"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(technician.getId()));
        mvc.perform(patch("/api/technicians/{id}/availability", other.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer technician")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\": false}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/technicians/{id}/availability", technician.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer technician")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    private MockHttpServletRequestBuilder changeStatus(Long id, String status, String token) {
        return patch("/api/incidents/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .content("RESOLVED".equals(status)
                        ? "{\"status\":\"RESOLVED\",\"comment\":\"Replaced the faulty switch port\"}"
                        : "{\"status\":\"" + status + "\"}");
    }

    private void stubVerifiedToken() {
        stubToken("verified", USER_ID);
    }

    private void stubToken(String token, String userId) {
        when(jwtDecoder.decode(token)).thenReturn(Jwt.withTokenValue(token)
                .header("alg", "ES256")
                .subject(userId)
                .claim("email", "reporter@example.test")
                .build());
    }
}
