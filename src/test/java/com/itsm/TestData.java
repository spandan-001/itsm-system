package com.itsm;

import java.util.List;

import com.itsm.enums.BusinessCriticality;
import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import com.itsm.model.Employee;
import com.itsm.model.ITService;
import com.itsm.model.Incident;

/** Builders for unsaved domain objects used by unit tests. */
public final class TestData {

    private TestData() {
    }

    public static Employee employee() {
        return new Employee("Asha Rao", "asha@example.test", "Finance");
    }

    public static Incident incident(Priority priority) {
        Incident incident = new Incident("VPN down", "Cannot connect to VPN", IncidentCategory.NETWORK,
                Impact.HIGH, Urgency.HIGH, employee(),
                List.of(new ITService("VPN", "Remote access", BusinessCriticality.HIGH)));
        incident.setPriority(priority);
        return incident;
    }
}
