
export const technicians = [
  {
    id: 'T001',
    name: 'Technician 1',
    skills: ['Network', 'Hardware'],
    available: true
  },
  {
    id: 'T002',
    name: 'Technician 2',
    skills: ['Software'],
    available: true
  },
  {
    id: 'T003',
    name: 'Technician 3',
    skills: ['Network', 'Software', 'Hardware'],
    available: true
  }
];

// Automatic incident assignment
export function assignTechnician(incident, allIncidents) {

  // Read availability changes made by Admin
  const savedAvailability = JSON.parse(
    localStorage.getItem('itsmTechnicianAvailability') || '{}'
  );

  // Step 1: Find skilled and available technicians
  const suitableTechnicians = technicians.filter((technician) => {

    const isAvailable =
      savedAvailability[technician.id] ?? technician.available;

    return (
      technician.skills.includes(incident.category) &&
      isAvailable
    );
  });

  // Step 2: No suitable technician found
  if (suitableTechnicians.length === 0) {
    return null;
  }

  // Step 3: Calculate active workload
  const techniciansWithWorkload = suitableTechnicians.map(
    (technician) => {

      const workload = allIncidents.filter(
        (item) =>
          item.assignedTo === technician.name &&
          !['Resolved', 'Closed'].includes(item.status)
      ).length;

      return {
        ...technician,
        workload
      };
    }
  );

  // Step 4: Select the least busy technician
  techniciansWithWorkload.sort(
    (a, b) =>
      a.workload - b.workload ||
      a.id.localeCompare(b.id)
  );

  // Step 5: Return technician name
  return techniciansWithWorkload[0].name;
}
