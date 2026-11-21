package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class NewState extends AbstractIncidentState {
    NewState() {
        super(IncidentStatus.NEW, "Reported, awaiting classification", IncidentStatus.CLASSIFIED);
    }
}
