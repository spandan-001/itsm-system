import { useState } from 'react';
import { dateTime, label } from '../api';
import { useApiData } from '../useApiData';
import ActivityCard from './ActivityCard';
import DashboardIcon from './DashboardIcon';

const assignable = new Set(['CLASSIFIED', 'QUEUED', 'ASSIGNED']);
const statuses = ['NEW', 'CLASSIFIED', 'QUEUED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED'];

export default function AdminDashboard({ request }) {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState('');
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState('');
  const query = new URLSearchParams({ page, ...(status && { status }), ...(keyword && { keyword }) });
  const incidents = useApiData(request, '/admin/incidents?' + query);
  const technicians = useApiData(request, '/technicians');
  const summary = useApiData(request, '/analytics/summary');
  const audit = useApiData(request, '/audit?size=5');

  async function assign(id, technicianId) {
    if (!technicianId) return;
    setPending(true);
    setFailure('');
    try {
      await request(`/incidents/${id}/assignment`, { method: 'PUT', body: { technicianId: Number(technicianId) } });
      incidents.reload();
      technicians.reload();
      summary.reload();
      audit.reload();
    } catch (error) {
      setFailure(error.message);
    } finally {
      setPending(false);
    }
  }

  const error = incidents.error || technicians.error || summary.error;
  return (
    <section className="itsm-panel">
      <header className="admin-heading"><div><h1>Overview</h1><p>Incident activity and service performance.</p></div></header>
      {(failure || error) && <p role="alert">{failure || error.message}</p>}
      {summary.data && <dl className="admin-stats itsm-facts">
        <div className="admin-stat-card"><dt><DashboardIcon name="laptop" />Total incidents</dt><dd>{summary.data.totalIncidents}</dd></div>
        <div className="admin-stat-card"><dt><DashboardIcon name="laptop" />Open</dt><dd>{summary.data.openIncidents}</dd></div>
        <div className="admin-stat-card"><dt><DashboardIcon name="clock" />Queued</dt><dd>{summary.data.queuedIncidents}</dd></div>
        <div className="admin-stat-card"><dt><DashboardIcon name="clock" />SLA breached</dt><dd>{summary.data.slaBreachedIncidents}</dd></div>
      </dl>}
      <div className="itsm-dashboard-grid">
        <section className="admin-panel">
          <div className="admin-panel-heading itsm-heading"><h2>Incident management</h2>
            <label>Search <input type="search" value={keyword} onChange={e => { setKeyword(e.target.value); setPage(0); }} /></label>
            <label>Status <select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}>
              <option value="">All statuses</option>
              {statuses.map(value => <option key={value} value={value}>{label(value)}</option>)}
            </select></label>
          </div>
          {!incidents.data && !incidents.error && <p className="itsm-panel-message" role="status">Loading incidents…</p>}
          {incidents.data?.content.length === 0 && <p className="itsm-panel-message">No matching incidents.</p>}
          {!!incidents.data?.content.length && <div className="admin-table-container itsm-table"><table>
            <thead><tr><th>Incident</th><th>Status</th><th>SLA due</th><th>Technician</th></tr></thead>
            <tbody>{incidents.data.content.map(item => <tr key={item.id}>
              <td>#{item.id} {item.title} · {item.priority}</td><td>{label(item.status)}</td><td>{dateTime(item.resolutionDueAt)}</td>
              <td>{assignable.has(item.status) && technicians.data ? (
                <select aria-label={`Assign technician for incident ${item.id}`}
                  value={item.assignedTechnician?.id ?? ''} disabled={pending}
                  onChange={event => assign(item.id, event.target.value)}>
                  <option value="" disabled>Unassigned</option>
                  {technicians.data.map(tech => <option key={tech.id} value={tech.id}
                    disabled={!tech.available}>{tech.name}</option>)}
                </select>
              ) : item.assignedTechnician?.name ?? 'Unassigned'}</td>
            </tr>)}</tbody>
          </table></div>}
          {incidents.data && (page > 0 || incidents.data.page.totalPages > 1) && <div className="itsm-actions">
            <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
            <span>Page {page + 1}</span>
            <button type="button" disabled={page + 1 >= incidents.data.page.totalPages} onClick={() => setPage(page + 1)}>Next</button>
          </div>}
        </section>
        <ActivityCard
          title="Recent audit activity"
          error={audit.error}
          empty="No audit events recorded."
          entries={audit.data?.content.map(event => ({
            id: event.id,
            title: label(event.action),
            detail: event.details,
            at: event.occurredAt,
          }))}
        />
      </div>
      <section className="admin-panel">
        <div className="admin-panel-heading"><h2>Technician workload</h2></div>
        {technicians.data?.length === 0 && <p className="itsm-panel-message">No technicians configured.</p>}
        {!!technicians.data?.length && <div className="admin-table-container itsm-table"><table>
          <thead><tr><th>Name</th><th>Availability</th><th>Assigned workload</th></tr></thead>
          <tbody>{technicians.data.map(tech => <tr key={tech.id}>
            <td>{tech.name}</td><td>{tech.available ? 'Available' : 'Unavailable'}</td><td>{tech.activeIncidents}</td>
          </tr>)}</tbody>
        </table></div>}
      </section>
    </section>
  );
}
