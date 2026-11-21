package com.itsm;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.itsm.enums.BusinessCriticality;
import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.Urgency;
import com.itsm.model.Employee;
import com.itsm.model.ITService;
import com.itsm.model.Incident;
import com.itsm.model.Technician;
import com.itsm.repository.AuditLogRepository;
import com.itsm.repository.EmployeeRepository;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.NotificationRepository;
import com.itsm.repository.TechnicianRepository;
import com.itsm.service.IncidentService;

/**
 * Shared setup for tests that run the whole application against H2.
 * Not @Transactional on purpose: notifications are only sent after commit.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Autowired protected IncidentService incidentService;
    @Autowired protected IncidentRepository incidentRepository;
    @Autowired protected AuditLogRepository auditLogRepository;
    @Autowired protected NotificationRepository notificationRepository;
    @Autowired protected EmployeeRepository employeeRepository;
    @Autowired protected TechnicianRepository technicianRepository;
    @Autowired protected ITServiceRepository itServiceRepository;

    protected Employee reporter;
    protected Technician networkTech;
    protected ITService wifi;

    @BeforeEach
    void resetDatabase() {
        notificationRepository.deleteAll();
        auditLogRepository.deleteAll();
        // Duplicates reference their original, so delete them first.
        incidentRepository.deleteAll(incidentRepository.findAll().stream().filter(Incident::isDuplicate).toList());
        incidentRepository.deleteAll();
        employeeRepository.deleteAll();
        technicianRepository.deleteAll();
        itServiceRepository.deleteAll();

        reporter = employeeRepository.save(new Employee("Asha Rao", "asha@example.test", "Finance"));
        networkTech = technicianRepository.save(new Technician("Ravi Kumar", "ravi@example.test", "NETWORK", true,
                EnumSet.of(IncidentCategory.NETWORK)));
        wifi = itServiceRepository.save(new ITService("Campus Wi-Fi", "Wireless", BusinessCriticality.HIGH));
    }

    /** A P1 network incident (HIGH impact, HIGH urgency, HIGH-criticality service). */
    protected Incident reportWifiOutage() {
        return report("Wi-Fi down in library", "No devices can connect to the library Wi-Fi",
                IncidentCategory.NETWORK, Impact.HIGH, Urgency.HIGH);
    }

    protected Incident report(String title, String description, IncidentCategory category,
                              Impact impact, Urgency urgency) {
        return incidentService.createIncident(title, description, category, impact, urgency,
                reporter.getId(), List.of(wifi.getId()));
    }
}
