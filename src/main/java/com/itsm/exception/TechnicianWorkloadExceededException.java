package com.itsm.exception;

public class TechnicianWorkloadExceededException extends ItsmException {

    public TechnicianWorkloadExceededException(String technicianName, long activeIncidents, int limit) {
        super("Technician " + technicianName + " already has " + activeIncidents
                + " open incidents (limit " + limit + ")");
    }
}
