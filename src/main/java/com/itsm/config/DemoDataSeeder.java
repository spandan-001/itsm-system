package com.itsm.config;

import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.itsm.enums.BusinessCriticality;
import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Urgency;
import com.itsm.model.Employee;
import com.itsm.model.ITService;
import com.itsm.model.Incident;
import com.itsm.model.Technician;
import com.itsm.repository.EmployeeRepository;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.TechnicianRepository;
import com.itsm.model.AccountLink.Role;
import com.itsm.service.AccountService;
import com.itsm.service.IncidentService;

/**
 * Demo data for an empty database, created through IncidentService so every
 * record has a real audit trail, SLA deadlines and notifications. Does
 * nothing if employees already exist (e.g. a seeded Supabase database).
 * Turn off with {@code itsm.seed.demo-data=false}.
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "itsm.seed.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final EmployeeRepository employees;
    private final TechnicianRepository technicians;
    private final ITServiceRepository services;
    private final IncidentService incidentService;

    public DemoDataSeeder(EmployeeRepository employees, TechnicianRepository technicians,
                          ITServiceRepository services, IncidentService incidentService) {
        this.employees = employees;
        this.technicians = technicians;
        this.services = services;
        this.incidentService = incidentService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (employees.count() > 0) {
            return;
        }
        ITService wifi = services.save(new ITService("Campus Wi-Fi", "Main campus wireless network", BusinessCriticality.HIGH));
        ITService portal = services.save(new ITService("Student Portal", "Web portal for students", BusinessCriticality.MEDIUM));
        ITService email = services.save(new ITService("Email", "Staff and student email", BusinessCriticality.HIGH));
        ITService printing = services.save(new ITService("Printing Service", "Shared campus printers", BusinessCriticality.LOW));
        ITService library = services.save(new ITService("Library Database", "Catalogue and journal access", BusinessCriticality.MEDIUM));

        Employee asha = employees.save(new Employee("Asha Rao", "asha.rao@example.test", "Finance"));
        Employee vikram = employees.save(new Employee("Vikram Shah", "vikram.shah@example.test", "Operations"));
        Employee neha = employees.save(new Employee("Neha Iyer", "neha.iyer@example.test", "Library"));

        technicians.save(new Technician("Ravi Kumar", "ravi.kumar@example.test", "NETWORK", true,
                EnumSet.of(IncidentCategory.NETWORK, IncidentCategory.SECURITY)));
        technicians.save(new Technician("Meera Nair", "meera.nair@example.test", "HARDWARE", true,
                EnumSet.of(IncidentCategory.HARDWARE, IncidentCategory.STORAGE)));
        technicians.save(new Technician("Arjun Menon", "arjun.menon@example.test", "SOFTWARE", true,
                EnumSet.of(IncidentCategory.SOFTWARE, IncidentCategory.APPLICATION, IncidentCategory.DATABASE)));
        technicians.save(new Technician("Kavya Reddy", "kavya.reddy@example.test", "AUTHENTICATION", false,
                EnumSet.of(IncidentCategory.AUTHENTICATION, IncidentCategory.CLOUD)));

        // A past outage, already fixed and rated: its fix is suggested for the new Wi-Fi incident below.
        Incident pastWifi = create("Library wireless access point offline", "Wi-Fi in the library reading room stopped working for everyone.",
                IncidentCategory.NETWORK, Impact.HIGH, Urgency.HIGH, vikram, wifi);
        advance(pastWifi, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS);
        incidentService.updateStatus(pastWifi.getId(), IncidentStatus.RESOLVED,
                "Access point AP-3 had crashed. Power-cycled it from the controller and updated its firmware to 8.10.");
        advance(pastWifi, IncidentStatus.VERIFIED, IncidentStatus.CLOSED);
        incidentService.recordFeedback(pastWifi.getId(), 5, "Fixed within the hour, thank you",
                new AccountService.LinkedAccount(Role.EMPLOYEE, vikram.getId(), vikram.getEmail()));

        Incident wifiDown = create("Campus Wi-Fi down in library", "No devices can connect to the Wi-Fi on any floor of the library.",
                IncidentCategory.NETWORK, Impact.HIGH, Urgency.HIGH, neha, wifi);
        // Detected as a duplicate of the one above.
        create("Library wifi not working", "Wi-Fi in the library is down, laptops cannot connect.",
                IncidentCategory.NETWORK, Impact.MEDIUM, Urgency.HIGH, asha, wifi);
        Incident portalSlow = create("Student portal very slow", "Pages take over 30 seconds to load during registration.",
                IncidentCategory.APPLICATION, Impact.MEDIUM, Urgency.MEDIUM, vikram, portal);
        Incident printer = create("Printer jammed on 2nd floor", "The shared printer near finance keeps jamming.",
                IncidentCategory.HARDWARE, Impact.LOW, Urgency.LOW, asha, printing);
        create("Cannot log in to email", "Password reset link never arrives and SSO login fails.",
                IncidentCategory.AUTHENTICATION, Impact.MEDIUM, Urgency.HIGH, vikram, email); // queued: Kavya is away
        create("Journal search returns errors", "Library database search shows a 500 error for every query.",
                IncidentCategory.DATABASE, Impact.MEDIUM, Urgency.MEDIUM, neha, library);

        advance(wifiDown, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS);
        advance(portalSlow, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS);
        incidentService.updateStatus(portalSlow.getId(), IncidentStatus.RESOLVED,
                "Added a database index on the registration table; page loads are under 2 seconds again.");
        advance(printer, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS);
        incidentService.updateStatus(printer.getId(), IncidentStatus.RESOLVED,
                "Replaced the worn pickup roller and cleared the paper path.");
        advance(printer, IncidentStatus.VERIFIED, IncidentStatus.CLOSED);

        log.info("Seeded demo data: {} services, {} employees, {} technicians, {} incidents",
                services.count(), employees.count(), technicians.count(), 7);
    }

    private Incident create(String title, String description, IncidentCategory category, Impact impact,
                            Urgency urgency, Employee reporter, ITService service) {
        return incidentService.createIncident(title, description, category, impact, urgency,
                reporter.getId(), List.of(service.getId()));
    }

    private void advance(Incident incident, IncidentStatus... steps) {
        for (IncidentStatus step : steps) {
            incidentService.updateStatus(incident.getId(), step);
        }
    }
}
