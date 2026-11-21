package com.itsm.exception;

public class TechnicianUnavailableException extends ItsmException {

    public TechnicianUnavailableException(String technicianName, String reason) {
        super("Technician " + technicianName + " cannot take this incident: " + reason);
    }
}
