
import { getSLAStatus } from './sla';

// Escalate incidents that have breached their SLA
export function checkAndEscalateIncidents(incidents) {

  let hasChanges = false;

  const updatedIncidents = incidents.map((incident) => {

    // Do not escalate resolved incidents
      if (['Resolved', 'Closed'].includes(incident.status)) {
      return incident;
    }

    // Check whether SLA has been breached
    const slaStatus = getSLAStatus(incident);

    // Only escalate breached incidents once
    if (
      slaStatus === 'Breached' &&
      !incident.escalationLevel
    ) {
      hasChanges = true;

      const escalationRecord = {
        level: 1,
        reason: 'SLA deadline breached',
        escalatedAt: new Date().toISOString()
      };

      return {
        ...incident,
        escalationLevel: 1,
        escalationHistory: [
          ...(incident.escalationHistory || []),
          escalationRecord
        ]
      };
    }

    return incident;
  });

  // Save only if an escalation occurred
  if (hasChanges) {
    localStorage.setItem(
      'itsmIncidents',
      JSON.stringify(updatedIncidents)
    );
  }

  return updatedIncidents;
}
