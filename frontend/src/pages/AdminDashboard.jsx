import { useState } from 'react';
import { Link } from 'react-router-dom';
import { dateTime, label } from '../api';
import { useApiData } from '../useApiData';
import ActivityCard from './ActivityCard';
import DashboardIcon from './DashboardIcon';

const assignable = new Set(['CLASSIFIED', 'QUEUED', 'ASSIGNED']);
const statuses = ['NEW', 'CLASSIFIED', 'QUEUED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED'];
const priorities = ['P1', 'P2', 'P3', 'P4'];
const categories = ['HARDWARE', 'SOFTWARE', 'NETWORK', 'DATABASE', 'CLOUD', 'SECURITY', 'AUTHENTICATION', 'APPLICATION', 'STORAGE'];
const emptyFilters = { incidentId: '', status: '', priority: '', category: '', technicianId: '', createdFrom: '', slaBreached: '', awaitingInfo: '' };

export default function AdminDashboard({ request }) {
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [filters, setFilters] = useState(emptyFilters);
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState('');
  const active = Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== ''));
  const query = new URLSearchParams({ page, ...active, ...(keyword && { keyword }) });

  function filter(name, value) {
    setFilters(current => ({ ...current, [name]: value }));
    setPage(0);
  }
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
        <div className="admin-stat-card"><dt><DashboardIcon name="laptop" />Critical open (P1)</dt><dd>{summary.data.criticalOpenIncidents}</dd></div>
        <div className="admin-stat-card"><dt><DashboardIcon name="bell" />Escalated (open)</dt><dd>{summary.data.escalatedOpenIncidents}</dd></div>
        <div className="admin-stat-card"><dt><DashboardIcon name="bell" />Waiting on reporter</dt><dd>{summary.data.awaitingReporterInfo}</dd></div>
        <div className="admin-stat-card"><dt><DashboardIcon name="check" />SLA compliance</dt>
          <dd>{summary.data.slaCompliancePercent == null ? '—' : `${summary.data.slaCompliancePercent}%`}</dd></div>
        <div className="admin-stat-card"><dt><DashboardIcon name="check" />Average rating</dt>
          <dd>{summary.data.averageFeedbackRating == null ? '—' : `${summary.data.averageFeedbackRating}/5`}</dd></div>
      </dl>}
      <div className="itsm-dashboard-grid">
        <section className="admin-panel">
          <div className="admin-panel-heading itsm-heading"><h2>Incident management</h2>
            <button type="button" className="itsm-button secondary"
              onClick={() => { setFilters(emptyFilters); setKeyword(''); setPage(0); }}>Clear filters</button>
          </div>
          <div className="itsm-filters">
            <label>Search <input type="search" value={keyword} onChange={e => { setKeyword(e.target.value); setPage(0); }} /></label>
            <label>Incident ID <input type="number" min="1" value={filters.incidentId} onChange={e => filter('incidentId', e.target.value)} /></label>
            <Select name="status" text="Status" value={filters.status} options={statuses} onChange={filter} />
            <Select name="priority" text="Priority" value={filters.priority} options={priorities} onChange={filter} />
            <Select name="category" text="Category" value={filters.category} options={categories} onChange={filter} />
            <label>Technician <select value={filters.technicianId} onChange={e => filter('technicianId', e.target.value)}>
              <option value="">Any</option>
              {technicians.data?.map(tech => <option key={tech.id} value={tech.id}>{tech.name}</option>)}
            </select></label>
            <label>Created from <input type="date" value={filters.createdFrom} onChange={e => filter('createdFrom', e.target.value)} /></label>
            <label>SLA <select value={filters.slaBreached} onChange={e => filter('slaBreached', e.target.value)}>
              <option value="">Any</option><option value="true">Breached</option><option value="false">Within SLA</option>
            </select></label>
            <label>Waiting on reporter <select value={filters.awaitingInfo} onChange={e => filter('awaitingInfo', e.target.value)}>
              <option value="">Any</option><option value="true">Yes</option><option value="false">No</option>
            </select></label>
          </div>
          {!incidents.data && !incidents.error && <p className="itsm-panel-message" role="status">Loading incidents…</p>}
          {incidents.data?.content.length === 0 && <p className="itsm-panel-message">No matching incidents.</p>}
          {!!incidents.data?.content.length && <div className="admin-table-container itsm-table"><table>
            <thead><tr><th>Incident</th><th>Status</th><th>SLA due</th><th>Technician</th></tr></thead>
            <tbody>{incidents.data.content.map(item => <tr key={item.id}>
              <td><Link to={`/incidents/${item.id}`}>#{item.id} {item.title}</Link> · {item.priority}
                {item.duplicateOfId && <span className="itsm-badge">duplicate of #{item.duplicateOfId}</span>}
                {item.awaitingReporterInfo && <span className="itsm-badge">waiting on reporter</span>}
                {item.escalationLevel && item.escalationLevel !== 'NONE' && <span className="itsm-badge">escalated: {label(item.escalationLevel)}</span>}
              </td><td>{label(item.status)}</td><td>{dateTime(item.resolutionDueAt)}</td>
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
      {summary.data && Object.keys(summary.data.byService ?? {}).length > 0 && <section className="admin-panel">
        <div className="admin-panel-heading"><h2>Incidents by service</h2></div>
        <div className="admin-table-container itsm-table"><table>
          <thead><tr><th>Service</th><th>Incidents</th></tr></thead>
          <tbody>{Object.entries(summary.data.byService).map(([service, count]) => <tr key={service}>
            <td>{service}</td><td>{count}</td>
          </tr>)}</tbody>
        </table></div>
      </section>}
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

function Select({ name, text, value, options, onChange }) {
  return <label>{text} <select value={value} onChange={event => onChange(name, event.target.value)}>
    <option value="">Any</option>
    {options.map(option => <option key={option} value={option}>{label(option)}</option>)}
  </select></label>;
}
