
import '../DashboardTheme.css';
import { Link } from 'react-router-dom';
import { useState, useEffect } from 'react';

const loadIncidents = () =>
  JSON.parse(localStorage.getItem('itsmIncidents') || '[]');

const loadNotifications = () =>
  JSON.parse(localStorage.getItem('itsmNotifications') || '[]');

function EmployeeDashboard() {
  const [incidents, setIncidents] = useState(loadIncidents);
  const [notifications, setNotifications] =
    useState(loadNotifications);

  // Refresh notifications when visiting the dashboard
  useEffect(() => {
    const refresh = () => {
      setIncidents(loadIncidents());
      setNotifications(loadNotifications());
    };

    window.addEventListener('focus', refresh);
    window.addEventListener('storage', refresh);

    return () => {
      window.removeEventListener('focus', refresh);
      window.removeEventListener('storage', refresh);
    };
  }, []);

  function handleConfirmation(incidentId, accepted) {
    const latestIncidents = loadIncidents();

    const target = latestIncidents.find(
      incident => incident.id === incidentId
    );

    if (!target || target.status !== 'Pending Confirmation') {
      setIncidents(latestIncidents);
      alert('This incident no longer requires confirmation.');
      return;
    }

    const now = new Date().toISOString();

    // Only the employee confirmation closes the incident
    const updatedIncidents = latestIncidents.map(incident =>
      incident.id === incidentId
        ? {
            ...incident,
            status: accepted ? 'Closed' : 'Open',
            resolvedAt: accepted ? now : null,
            closedAt: accepted ? now : null,
            resolutionRequestedAt: null
          }
        : incident
    );

    localStorage.setItem(
      'itsmIncidents',
      JSON.stringify(updatedIncidents)
    );

    const latestNotifications = loadNotifications();

    const updatedNotifications = latestNotifications.map(
      notification =>
        notification.incidentId === incidentId &&
        notification.type === 'resolution_confirmation'
          ? {
              ...notification,
              answered: true,
              read: true,
              response: accepted ? 'Confirmed' : 'Rejected',
              answeredAt: now
            }
          : notification
    );

    localStorage.setItem(
      'itsmNotifications',
      JSON.stringify(updatedNotifications)
    );

    setIncidents(updatedIncidents);
    setNotifications(updatedNotifications);

    alert(
      accepted
        ? 'Thank you. Your incident is now closed.'
        : 'The incident has been reopened for the technician.'
    );
  }

  const pendingConfirmations = notifications.filter(
    notification =>
      notification.recipient === 'employee' &&
      notification.type === 'resolution_confirmation' &&
      !notification.answered &&
      incidents.some(
        incident =>
          incident.id === notification.incidentId &&
          incident.status === 'Pending Confirmation'
      )
  );

  const total = incidents.length;

  const open = incidents.filter(
    incident => incident.status === 'Open'
  ).length;

  const progress = incidents.filter(
    incident => incident.status === 'In Progress'
  ).length;

  const pending = incidents.filter(
    incident => incident.status === 'Pending Confirmation'
  ).length;

  const closed = incidents.filter(
    incident =>
      incident.status === 'Closed' ||
      incident.status === 'Resolved'
  ).length;

  const recentIncidents = [...incidents].reverse().slice(0, 5);

  return (
    <div className="dashboard">

      <aside className="sidebar">
        <h2>I-ITSM</h2>
        <p className="sidebar-subtitle">
          IT Support Portal
        </p>

        <nav>
          <Link className="active" to="/employee">
            Dashboard
          </Link>

          <Link to="/my-incidents">
            My Incidents
          </Link>

          <Link to="/report-incident">
            Report Incident
          </Link>

          <a href="#employee-notifications">
            Notifications
            {pendingConfirmations.length > 0 &&
              ` (${pendingConfirmations.length})`}
          </a>
        </nav>

        <div className="sidebar-bottom">
          <Link to="/">Logout</Link>
        </div>
      </aside>

      <main className="main-content">

        <header className="topbar">
          <div>
            <h2>Employee Dashboard</h2>
            <p>Manage and track IT support requests.</p>
          </div>

          <div className="profile">
            <div className="profile-avatar">E</div>
            <span>Employee</span>
          </div>
        </header>

        <section className="welcome-section">
          <div>
            <h1>My IT Support</h1>
            <p>
              Track your incidents and confirm resolutions.
            </p>
          </div>

          <Link
            to="/report-incident"
            className="report-button"
          >
            + Report an Incident
          </Link>
        </section>

        <section className="stats-grid">
          <div className="stat-card">
            <p>Total Incidents</p>
            <h2>{total}</h2>
          </div>

          <div className="stat-card">
            <p>Open</p>
            <h2>{open}</h2>
          </div>

          <div className="stat-card">
            <p>In Progress</p>
            <h2>{progress}</h2>
          </div>

          <div className="stat-card">
            <p>Awaiting Confirmation</p>
            <h2>{pending}</h2>
          </div>

          <div className="stat-card">
            <p>Closed</p>
            <h2>{closed}</h2>
          </div>
        </section>

        {/* Employee notifications */}
        <section
          id="employee-notifications"
          className="employee-notifications"
        >
          <div className="section-heading">
            <div>
              <h2>Resolution Confirmations</h2>
              <p>
                Confirm whether the technician fixed your issue.
              </p>
            </div>
          </div>

          {pendingConfirmations.length === 0 ? (
            <div className="employee-notification-empty">
              No pending resolution confirmations.
            </div>
          ) : (
            pendingConfirmations.map(notification => (
              <div
                key={notification.id}
                className="employee-confirmation-card"
              >
                <div>
                  <small>{notification.incidentId}</small>
                  <h3>{notification.title}</h3>
                  <p>{notification.message}</p>
                  <strong>
                    Has your problem actually been resolved?
                  </strong>
                </div>

                <div className="employee-confirmation-actions">
                  <button
                    className="employee-confirm-yes"
                    onClick={() =>
                      handleConfirmation(
                        notification.incidentId,
                        true
                      )
                    }
                  >
                    Yes, close incident
                  </button>

                  <button
                    className="employee-confirm-no"
                    onClick={() =>
                      handleConfirmation(
                        notification.incidentId,
                        false
                      )
                    }
                  >
                    No, reopen incident
                  </button>
                </div>
              </div>
            ))
          )}
        </section>

        <section className="incidents-section">
          <div className="section-heading">
            <div>
              <h2>Recent Incidents</h2>
              <p>Your recently reported requests.</p>
            </div>

            <Link to="/my-incidents">
              View all
            </Link>
          </div>

          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Incident ID</th>
                  <th>Issue</th>
                  <th>Category</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>

              <tbody>
                {recentIncidents.length > 0 ? (
                  recentIncidents.map(incident => (
                    <tr key={incident.id}>
                      <td>{incident.id}</td>
                      <td>{incident.title}</td>
                      <td>{incident.category}</td>
                      <td>{incident.priority}</td>
                      <td>{incident.status}</td>
                      <td>{incident.date}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="6">
                      No incidents reported yet.
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

export default EmployeeDashboard;
