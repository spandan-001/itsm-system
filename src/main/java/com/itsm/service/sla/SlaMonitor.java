package com.itsm.service.sla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically checks every open incident for missed SLA deadlines. */
@Component
public class SlaMonitor {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitor.class);

    private final SlaService slaService;

    public SlaMonitor(SlaService slaService) {
        this.slaService = slaService;
    }

    @Scheduled(initialDelayString = "${itsm.sla.initial-delay-ms:30000}",
               fixedDelayString = "${itsm.sla.check-interval-ms:60000}")
    public void checkForBreaches() {
        int breaches = slaService.detectBreaches();
        if (breaches > 0) {
            log.warn("SLA monitor flagged {} new breach(es)", breaches);
        }
    }
}
