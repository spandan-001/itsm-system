
// SLA resolution targets in hours
export const slaTargets = {
  High: 4,
  Medium: 8,
  Low: 24
};

// Calculate the SLA deadline
export function calculateSLADeadline(priority, createdAt) {
  const hours = slaTargets[priority] ?? 24;

  const deadline = new Date(createdAt);

  deadline.setTime(
    deadline.getTime() + hours * 60 * 60 * 1000
  );

  return deadline.toISOString();
}

// Check SLA status
export function getSLAStatus(incident) {
  if (!incident.slaDeadline) {
    return 'Not Set';
  }

  const deadline = new Date(incident.slaDeadline);

  // Support both old and new incident statuses
  if (
    incident.status === 'Resolved' ||
    incident.status === 'Closed'
  ) {
    if (!incident.resolvedAt) {
      return 'Resolution Time Unknown';
    }

    return new Date(incident.resolvedAt) <= deadline
      ? 'Met'
      : 'Breached';
  }

  return new Date() > deadline
    ? 'Breached'
    : 'Within SLA';
}
