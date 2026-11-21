import { useState } from 'react';
import { Link } from 'react-router-dom';

import { dateTime, label } from '../api';
import { useApiData } from '../useApiData';
import ActivityCard from './ActivityCard';
import DashboardIcon from './DashboardIcon';

const categoryIcons = { NETWORK: 'wifi', AUTHENTICATION: 'lock', SECURITY: 'lock', HARDWARE: 'printer' };

const openStatuses = ['NEW', 'CLASSIFIED', 'QUEUED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS'];
const confirmationStatuses = ['RESOLVED', 'VERIFIED'];
const inProgressStatuses = ['ACKNOWLEDGED', 'IN_PROGRESS'];

export default function MyIncidents({ request, account, overview = true }) {
  const [page, setPage] = useState(0);
  const list = useApiData(request, overview ? '/incidents?size=5&sort=createdAt,desc&sort=id,desc' : `/incidents?page=${page}`);
  const sla = useApiData(request, overview ? '/incidents/search?size=1&slaBreached=true' : null);
  const unread = useApiData(request, overview ? '/notifications/unread-count' : null);
  const employee = account.role === 'EMPLOYEE';
  const { data, error, reload } = list;
  const title = employee ? 'My incidents' : 'Assigned incidents';

  return (
    <>
      <section className="welcome-section itsm-welcome">
        <div>
          <h1>{overview ? (employee ? 'My IT Support' : 'My Workspace') : title}</h1>
          <p>{employee ? 'Track your incidents and confirm resolutions.' : 'Review your assignments and update their status.'}</p>
        </div>
        {employee && <Link className="report-button" to="/report-incident">Report an incident</Link>}
      </section>
      {overview && (
        <>
          {!employee && <Availability request={request} accountId={account.accountId} onChange={reload} />}
          <section className="stats-grid" aria-label="Dashboard summary">
            <StatusCount name="Open incidents" icon="laptop" statuses={openStatuses} request={request} summary={data} />
            <StatusCount
              name={employee ? 'Awaiting confirmation' : 'In progress'}
              icon={employee ? 'check' : 'clock'}
              statuses={employee ? confirmationStatuses : inProgressStatuses}
              request={request}
              summary={data}
            />
            <StatCard title="Unread notifications" icon="bell" value={unread.data?.unread} error={unread.error} />
            <StatCard title="SLA breaches" icon="clock" value={sla.data?.page.totalElements} error={sla.error} />
          </section>
          {employee && <Confirmations request={request} summary={data} />}
        </>
      )}
      <div className={overview ? "itsm-dashboard-grid" : undefined}>
        <section className="incidents-section">
          <div className="section-heading itsm-heading">
            <div><h2>{overview && employee ? 'Recent incidents' : title}</h2>
              <p>{employee ? 'Your IT support requests' : 'Incidents assigned to your account'}</p>
            </div>
            {overview
              ? <Link to="/my-incidents">View all</Link>
              : <button className="itsm-button secondary" type="button" onClick={reload}>Refresh</button>}
          </div>
          {error && <p className="itsm-panel-message" role="alert">{error.message}</p>}
          {!data && !error && <p className="itsm-panel-message" role="status">Loading incidents…</p>}
          {data && (data.content.length ? (
            <div className="table-container itsm-table">
              <table>
                <thead><tr><th>Incident ID</th><th>Issue</th><th>Category</th><th>Priority</th><th>Status</th><th>Resolution due</th></tr></thead>
                <tbody>
                  {data.content.map(incident => (
                    <tr key={incident.id}>
                      <td className="incident-id">#{incident.id}</td>
                      <td><Link className="itsm-incident-link" to={`/incidents/${incident.id}`}>
                        <DashboardIcon name={categoryIcons[incident.category] ?? 'laptop'} />{incident.title}
                      </Link></td>
                      <td>{label(incident.category)}</td>
                      <td><span className={`priority ${incident.priority === 'P1' ? 'high' : ''}`}>{incident.priority}</span></td>
                      <td><span className={`status ${incident.status.toLowerCase().replaceAll('_', '-')}`}>{label(incident.status)}</span></td>
                      <td>{dateTime(incident.resolutionDueAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <p className="itsm-panel-message">No incidents on this page.</p>)}
          {!overview && data && (page > 0 || data.page.totalPages > 1) && (
            <div className="itsm-actions itsm-panel-message">
              <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
              <span>Page {page + 1}</span>
              <button type="button" disabled={page + 1 >= data.page.totalPages} onClick={() => setPage(page + 1)}>Next</button>
            </div>
          )}
        </section>
        {overview && <ActivityCard
          title={employee ? 'Recently reported' : 'Recent tickets in your queue'}
          error={error}
          empty="No incidents yet."
          entries={data?.content.map(incident => ({
            id: incident.id,
            title: <Link to={`/incidents/${incident.id}`}>{incident.title}</Link>,
            detail: `#${incident.id} · ${label(incident.status)}`,
            at: incident.createdAt,
          }))}
        />}
      </div>
    </>
  );
}

function StatCard({ title, value, error, icon = 'laptop' }) {
  return (
    <div className="stat-card">
      <DashboardIcon name={icon} />
      <div>
        <p>{title}</p>
        {error ? <span role="status">Unavailable</span> : <h2>{value ?? '—'}</h2>}
      </div>
    </div>
  );
}

function StatusCount({ name, icon, statuses, request, summary }) {
  const query = new URLSearchParams([['size', '1'], ...statuses.map(status => ['status', status])]);
  const { data } = useApiData(request, summary ? '/incidents/search?' + query : null);
  return <StatCard title={name} icon={icon} value={data?.page.totalElements} />;
}

function Confirmations({ request, summary }) {
  const { data, error } = useApiData(request, summary ? '/incidents/search?status=RESOLVED&size=5' : null);
  return (
    <section className="employee-notifications">
      <div className="section-heading">
        <div><h2>Resolution confirmations</h2><p>Review fixes submitted by your technician.</p></div>
      </div>
      {error && <p className="itsm-panel-message" role="alert">{error.message}</p>}
      {data?.content.length === 0 && <p className="itsm-panel-message">No fixes awaiting confirmation.</p>}
      {data?.content.map(item => <div className="employee-confirmation-card" key={item.id}>
        <div><small>#{item.id}</small><h3>{item.title}</h3></div>
        <Link className="itsm-button" to={`/incidents/${item.id}`}>Review fix</Link>
      </div>)}
    </section>
  );
}

function Availability({ request, accountId, onChange }) {
  const { data, error, reload } = useApiData(request, '/technicians/me');
  const [failure, setFailure] = useState('');
  const [pending, setPending] = useState(false);

  async function toggle() {
    setPending(true);
    setFailure('');
    try {
      await request(`/technicians/${accountId}/availability`, { method: 'PATCH', body: { available: !data.available } });
      reload();
      onChange();
    } catch (error) {
      setFailure(error.message);
    } finally {
      setPending(false);
    }
  }

  return (
    <section className="tech-availability-control">
      <div><strong>My availability</strong>
        {data && <p>{data.available ? 'You can receive new assignments.' : 'Automatic assignments are paused.'}</p>}
        {(error || failure) && <p role="alert">{failure || error.message}</p>}
      </div>
      {data && <button type="button" className="tech-availability-button" disabled={pending} onClick={toggle}>
        {data.available ? 'Set unavailable' : 'Set available'}
      </button>}
    </section>
  );
}
