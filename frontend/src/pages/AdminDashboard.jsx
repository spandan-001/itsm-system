
import '../DashboardTheme.css';
import { Link } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { technicians } from '../data/technicians';
import { getSLAStatus } from '../data/sla';
import { checkAndEscalateIncidents } from '../data/escalation';

const readIncidents = () =>
  JSON.parse(localStorage.getItem('itsmIncidents') || '[]');

function AdminDashboard() {
  const [incidents, setIncidents] = useState(readIncidents);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');

  useEffect(() => {
    const refresh = () => {
      const latest = readIncidents();

      setIncidents(checkAndEscalateIncidents(latest));
    };

    refresh();

    const timer = setInterval(refresh, 60000);
    window.addEventListener('focus', refresh);

    return () => {
      clearInterval(timer);
      window.removeEventListener('focus', refresh);
    };
  }, []);

  function handleAssign(id, technicianName) {
    const updated = readIncidents().map(incident =>
      incident.id === id
        ? { ...incident, assignedTo: technicianName }
        : incident
    );

    localStorage.setItem(
      'itsmIncidents',
      JSON.stringify(updated)
    );

    setIncidents(updated);
  }

  const total = incidents.length;

  const open = incidents.filter(
    i => i.status === 'Open'
  ).length;

  const progress = incidents.filter(
    i => i.status === 'In Progress'
  ).length;

  const pending = incidents.filter(
    i => i.status === 'Pending Confirmation'
  ).length;

  const closed = incidents.filter(
    i => i.status === 'Closed' || i.status === 'Resolved'
  ).length;

  const breached = incidents.filter(
    i => getSLAStatus(i) === 'Breached'
  ).length;

  const attention = incidents.filter(
    i =>
      i.status !== 'Closed' &&
      i.status !== 'Resolved' &&
      (
        !i.assignedTo ||
        getSLAStatus(i) === 'Breached'
      )
  ).length;

  const filteredIncidents = incidents.filter(incident => {
    const query = search.toLowerCase();

    const matchesSearch = [
      incident.id,
      incident.title,
      incident.category
    ].some(value =>
      String(value || '').toLowerCase().includes(query)
    );

    const matchesStatus =
      statusFilter === 'All' ||
      (statusFilter === 'Closed'
        ? ['Closed', 'Resolved'].includes(incident.status)
        : incident.status === statusFilter);

    return matchesSearch && matchesStatus;
  });

  const stats = [
    { label: 'Total incidents', value: total },
    { label: 'Open', value: open },
    { label: 'In progress', value: progress },
    { label: 'Awaiting confirmation', value: pending },
    { label: 'Closed', value: closed },
    { label: 'SLA breached', value: breached }
  ];

  return (
    <div className="admin-dashboard">

      <aside className="admin-sidebar">
        <div className="admin-brand">
          <h2>I-ITSM</h2>
          <p>IT Service Management</p>
        </div>

        <div className="nav-label">WORKSPACE</div>

        <nav>
          <a className="active" href="#overview">
            Overview
          </a>
          <a href="#incidents">Incidents</a>
          <a href="#technicians">Technicians</a>
          <a href="#monitoring">SLA Monitoring</a>
        </nav>

        <div className="admin-sidebar-bottom">
          <Link to="/">Sign out</Link>
        </div>
      </aside>

      <main className="admin-main" id="overview">
        <header className="admin-header">
          <span className="admin-breadcrumb">
            ITSM / Administration
          </span>

          <div className="admin-profile">
            <span>Administrator</span>
            <div className="admin-avatar">A</div>
          </div>
        </header>

        <section className="admin-heading">
          <div>
            <h1>Overview</h1>
            <p>
              Incident operations, SLA tracking and assignments.
            </p>
          </div>
        </section>

        <section className="admin-stats">
          {stats.map(stat => (
            <div className="admin-stat-card" key={stat.label}>
              <p>{stat.label}</p>
              <h2>{stat.value}</h2>
            </div>
          ))}
        </section>

        <section className="admin-monitoring" id="monitoring">
          <div className="admin-monitor-block">
            <p className="monitor-label">ACTIVE INCIDENTS</p>
            <h2>{open + progress + pending}</h2>
            <p>Incidents not yet closed</p>
          </div>

          <div className="admin-monitor-block">
            <p className="monitor-label">AWAITING EMPLOYEE</p>
            <h2>{pending}</h2>
            <p>Resolution confirmations pending</p>
          </div>

          <div className="admin-monitor-block">
            <p className="monitor-label">NEEDS ATTENTION</p>
            <h2>{attention}</h2>
            <p>Unassigned or overdue incidents</p>
          </div>
        </section>

        <section className="admin-panel" id="incidents">
          <div className="admin-panel-heading">
            <div>
              <h2>Incident management</h2>
              <p>All reported support requests</p>
            </div>

            <span className="panel-count">
              {filteredIncidents.length} incidents
            </span>
          </div>

          <div className="admin-toolbar">
            <input
              type="text"
              placeholder="Search incidents..."
              aria-label="Search incidents"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />

            <select
              aria-label="Filter incident status"
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
            >
              <option value="All">All statuses</option>
              <option value="Open">Open</option>
              <option value="In Progress">In Progress</option>
              <option value="Pending Confirmation">
                Pending Confirmation
              </option>
              <option value="Closed">Closed</option>
            </select>
          </div>

          <div className="admin-table-container">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Incident</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>SLA</th>
                  <th>Escalation</th>
                  <th>Assigned to</th>
                </tr>
              </thead>

              <tbody>
                {filteredIncidents.length > 0 ? (
                  filteredIncidents.map(incident => {
                    const sla = getSLAStatus(incident);

                    return (
                      <tr key={incident.id}>
                        <td className="incident-id">
                          {incident.id}
                        </td>

                        <td>
                          <div className="incident-info">
                            <strong>{incident.title}</strong>
                            <small>{incident.category}</small>
                          </div>
                        </td>

                        <td>
                          <span className="plain-tag">
                            {incident.priority}
                          </span>
                        </td>

                        <td>{incident.status}</td>

                        <td>
                          <span className={
                            sla === 'Breached'
                              ? 'text-alert'
                              : 'text-muted'
                          }>
                            {sla}
                          </span>
                        </td>

                        <td>
                          {incident.escalationLevel > 0
                            ? `Level ${incident.escalationLevel}`
                            : '—'}
                        </td>

                        <td>
                          <select
                            aria-label={`Assign ${incident.id}`}
                            value={incident.assignedTo || ''}
                            onChange={e =>
                              handleAssign(
                                incident.id,
                                e.target.value
                              )
                            }
                          >
                            <option value="">Unassigned</option>

                            {technicians.map(t => (
                              <option
                                key={t.id}
                                value={t.name}
                              >
                                {t.name}
                              </option>
                            ))}
                          </select>
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan="7" className="empty-state">
                      No incidents found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="admin-panel" id="technicians">
          <div className="admin-panel-heading">
            <div>
              <h2>Technician workload</h2>
              <p>Read-only assignment overview</p>
            </div>
          </div>

          <div className="admin-table-container">
            <table>
              <thead>
                <tr>
                  <th>Technician</th>
                  <th>Skills</th>
                  <th>Active incidents</th>
                </tr>
              </thead>

              <tbody>
                {technicians.map(technician => {
                  const workload = incidents.filter(i =>
                    i.assignedTo === technician.name &&
                    !['Closed', 'Resolved'].includes(i.status)
                  ).length;

                  return (
                    <tr key={technician.id}>
                      <td>
                        <div className="incident-info">
                          <strong>{technician.name}</strong>
                          <small>{technician.id}</small>
                        </div>
                      </td>

                      <td>
                        {technician.skills.join(', ')}
                      </td>

                      <td>{workload}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </section>

        <footer className="admin-footer">
          I-ITSM · IT Service Management System
        </footer>
      </main>
    </div>
  );
}

export default AdminDashboard;
