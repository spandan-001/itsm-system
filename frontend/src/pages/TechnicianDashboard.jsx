
import '../DashboardTheme.css';
import { Link } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { technicians } from '../data/technicians';
import { getSLAStatus } from '../data/sla';

const readIncidents = () =>
  JSON.parse(localStorage.getItem('itsmIncidents') || '[]');

const readAvailability = () =>
  JSON.parse(
    localStorage.getItem('itsmTechnicianAvailability') || '{}'
  );

function TechnicianDashboard() {
  const [session, setSession] = useState(() =>
    sessionStorage.getItem('itsmTechnicianSession') || ''
  );

  const [loginSelection, setLoginSelection] = useState('T001');
  const [incidents, setIncidents] = useState(readIncidents);
  const [availability, setAvailability] = useState(readAvailability);

  const technician = technicians.find(t => t.id === session);

  // Refresh saved data when the page becomes active
  useEffect(() => {
    const refresh = () => {
      setIncidents(readIncidents());
      setAvailability(readAvailability());
    };

    window.addEventListener('focus', refresh);
    window.addEventListener('storage', refresh);

    return () => {
      window.removeEventListener('focus', refresh);
      window.removeEventListener('storage', refresh);
    };
  }, []);

  function login() {
    sessionStorage.setItem(
      'itsmTechnicianSession',
      loginSelection
    );
    setSession(loginSelection);
  }

  function logout() {
    sessionStorage.removeItem('itsmTechnicianSession');
    setSession('');
  }

  // Only change this technician's availability
  function toggleAvailability() {
    if (!technician) return;

    const latest = readAvailability();

    const current =
      latest[technician.id] ?? technician.available;

    const updated = {
      ...latest,
      [technician.id]: !current
    };

    localStorage.setItem(
      'itsmTechnicianAvailability',
      JSON.stringify(updated)
    );

    setAvailability(updated);
  }

  function handleStatusChange(id, newStatus) {
    if (!technician) return;

    const latest = readIncidents();

    const target = latest.find(
      incident =>
        incident.id === id &&
        incident.assignedTo === technician.name
    );

    if (!target || target.status === 'Closed') return;

    // Technician requests confirmation, not final closure
    if (newStatus === 'Pending Confirmation') {
      if (target.status === 'Pending Confirmation') return;

      const updated = latest.map(incident =>
        incident.id === id
          ? {
              ...incident,
              status: 'Pending Confirmation',
              resolutionRequestedAt: new Date().toISOString(),
              resolvedAt: null
            }
          : incident
      );

      localStorage.setItem(
        'itsmIncidents',
        JSON.stringify(updated)
      );

      // Create an in-app employee notification
      const notifications = JSON.parse(
        localStorage.getItem('itsmNotifications') || '[]'
      );

      const notification = {
        id: `${id}-${Date.now()}`,
        incidentId: id,
        recipient: 'employee',
        type: 'resolution_confirmation',
        title: 'Please confirm resolution',
        message: `${technician.name} has marked "${target.title}" as resolved. Please confirm whether the issue is fixed.`,
        createdAt: new Date().toISOString(),
        read: false,
        answered: false
      };

      localStorage.setItem(
        'itsmNotifications',
        JSON.stringify([...notifications, notification])
      );

      setIncidents(updated);

      alert(
        'Resolution confirmation sent to the employee.'
      );
      return;
    }

    // Allow Open and In Progress changes only
    if (!['Open', 'In Progress'].includes(newStatus)) return;
    if (target.status === 'Pending Confirmation') return;

    const updated = latest.map(incident =>
      incident.id === id
        ? {
            ...incident,
            status: newStatus,
            resolvedAt: null
          }
        : incident
    );

    localStorage.setItem(
      'itsmIncidents',
      JSON.stringify(updated)
    );

    setIncidents(updated);
  }

  // Temporary demo sign-in
  if (!technician) {
    return (
      <div className="tech-demo-login">
        <div className="tech-login-box">
          <h1>I-ITSM</h1>
          <h2>Technician sign in</h2>
          <p>
            Select a demo account. This will be replaced
            by authenticated login.
          </p>

          <select
            value={loginSelection}
            onChange={e => setLoginSelection(e.target.value)}
          >
            {technicians.map(t => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>

          <button onClick={login}>
            Continue
          </button>

          <Link to="/">Back to home</Link>
        </div>
      </div>
    );
  }

  const myIncidents = incidents.filter(
    incident => incident.assignedTo === technician.name
  );

  const isAvailable =
    availability[technician.id] ?? technician.available;

  const open = myIncidents.filter(
    incident => incident.status === 'Open'
  ).length;

  const progress = myIncidents.filter(
    incident => incident.status === 'In Progress'
  ).length;

  const pending = myIncidents.filter(
    incident => incident.status === 'Pending Confirmation'
  ).length;

  const closed = myIncidents.filter(
    incident =>
      incident.status === 'Closed' ||
      incident.status === 'Resolved'
  ).length;

  return (
    <div className="technician-dashboard">

      <aside className="tech-sidebar">
        <h2>I-ITSM</h2>
        <p>IT Support Portal</p>

        <nav>
          <a href="#tech-overview" className="active">
            Dashboard
          </a>

          <a href="#assigned-incidents">
            My Assigned Incidents
          </a>
        </nav>

        <div className="tech-sidebar-bottom">
          <button
            className="tech-signout"
            onClick={logout}
          >
            Sign out
          </button>
        </div>
      </aside>

      <main className="tech-main" id="tech-overview">

        <header className="tech-header">
          <div>
            <h2>Technician Dashboard</h2>
            <p>Manage your assigned IT incidents.</p>
          </div>

          <div className="tech-profile">
            <div className="tech-avatar">
              {technician.name.charAt(0)}
            </div>
            <span>{technician.name}</span>
          </div>
        </header>

        <section className="tech-welcome">
          <h1>My Workspace</h1>
          <p>
            Your assignments and incident resolution activity.
          </p>

          <div className="tech-availability-control">
            <div>
              <strong>My availability</strong>
              <p>
                {isAvailable
                  ? 'You can receive new assignments.'
                  : 'New incidents will not be assigned to you automatically.'}
              </p>
            </div>

            <button
              onClick={toggleAvailability}
              className="tech-availability-button"
            >
              {isAvailable
                ? 'Set Unavailable'
                : 'Set Available'}
            </button>
          </div>
        </section>

        <section className="tech-stats">
          <div className="tech-stat-card">
            <p>Total Assigned</p>
            <h2>{myIncidents.length}</h2>
          </div>

          <div className="tech-stat-card">
            <p>Open</p>
            <h2>{open}</h2>
          </div>

          <div className="tech-stat-card">
            <p>In Progress</p>
            <h2>{progress}</h2>
          </div>

          <div className="tech-stat-card">
            <p>Awaiting Confirmation</p>
            <h2>{pending}</h2>
          </div>

          <div className="tech-stat-card">
            <p>Closed</p>
            <h2>{closed}</h2>
          </div>
        </section>

        <section
          className="tech-incidents"
          id="assigned-incidents"
        >
          <div className="tech-section-heading">
            <div>
              <h2>My Assigned Incidents</h2>
              <p>
                Only incidents assigned to {technician.name}
                are shown here.
              </p>
            </div>
          </div>

          <div className="tech-table-container">
            <table>
              <thead>
                <tr>
                  <th>Incident ID</th>
                  <th>Issue</th>
                  <th>Category</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>SLA</th>
                  <th>Action</th>
                </tr>
              </thead>

              <tbody>
                {myIncidents.length > 0 ? (
                  myIncidents.map(incident => (
                    <tr key={incident.id}>
                      <td>{incident.id}</td>
                      <td>{incident.title}</td>
                      <td>{incident.category}</td>
                      <td>{incident.priority}</td>
                      <td>{incident.status}</td>

                      <td>
                        <span className="tech-sla-badge">
                          {getSLAStatus(incident)}
                        </span>
                      </td>

                      <td>
                        {incident.status ===
                          'Pending Confirmation' ? (
                          <span>Awaiting employee</span>
                        ) : incident.status === 'Closed' ||
                          incident.status === 'Resolved' ? (
                          <span>Closed</span>
                        ) : (
                          <select
                            value={incident.status}
                            aria-label={`Update ${incident.id}`}
                            onChange={e =>
                              handleStatusChange(
                                incident.id,
                                e.target.value
                              )
                            }
                          >
                            <option value="Open">
                              Open
                            </option>

                            <option value="In Progress">
                              In Progress
                            </option>

                            <option value="Pending Confirmation">
                              Request Resolution
                            </option>
                          </select>
                        )}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="7">
                      No incidents assigned to your account.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

      </main>
    </div>
  );
}

export default TechnicianDashboard;
