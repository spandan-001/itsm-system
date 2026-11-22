import CreateUser from './CreateUser';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { dateTime, label } from '../api';
import { useApiData } from '../useApiData';
import { useAutoRefresh } from '../useAutoRefresh';
import ActivityCard from './ActivityCard';
import BarChart from './BarChart';
import ColumnChart from './ColumnChart';
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
  const outages = useApiData(request, '/outages');
  const atRisk = useApiData(request, '/sla/at-risk');
  const [live, setLive] = useState(true);
  const [updatedAt, setUpdatedAt] = useState(() => new Date());

  function reloadAll() {
    incidents.reload();
    technicians.reload();
    summary.reload();
    audit.reload();
    outages.reload();
    atRisk.reload();
    setUpdatedAt(new Date());
  }
  useAutoRefresh(reloadAll, 15000, live);

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
      <header className="admin-heading"><div><h1>Overview</h1><p>Incident activity and service performance.</p></div>
        <div className="itsm-live">
          <label><input type="checkbox" checked={live} onChange={e => setLive(e.target.checked)} /> Live updates (every 15 s)</label>
          <small>Updated {updatedAt.toLocaleTimeString()}</small>
      <CreateUser request={request} categories={categories} />
          <button type="button" className="itsm-button secondary" onClick={reloadAll}>Refresh</button>
        </div>
      </header>
      {(failure || error) && <p role="alert">{failure || error.message}</p>}
      {summary.data && <dl className="admin-stats itsm-facts">
        <div className="admin-stat-card"><dt><DashboardIcon name="laptop" />Open incidents</dt><dd>{summary.data.openIncidents}</dd></div>
        <div className="admin-stat-card"><dt><DashboardIcon name="clock" />Queued incidents</dt><dd>{summary.data.queuedIncidents}</dd></div>
        <div className="admin-stat-card"><dt><DashboardIcon name="bell" />Critical open (P1)</dt><dd>{summary.data.criticalOpenIncidents}</dd></div>
        <div className="admin-stat-card"><dt><DashboardIcon name="clock" />SLA breaches</dt><dd>{summary.data.slaBreachedIncidents}</dd></div>
      </dl>}
      {summary.data && <section className="admin-panel">
        <div className="admin-panel-heading"><h2>Analytics</h2></div>
        <div className="itsm-chart-grid">
          <ColumnChart title="Incidents reported, last 7 days" data={Object.entries(summary.data.createdPerDay)
            .map(([day, value]) => ({ label: new Date(`${day}T00:00`).toLocaleDateString(undefined, { weekday: 'short' }), value }))} />
          <BarChart variant="status" title="Incidents by status" data={statuses.map(value => ({ label: label(value), value: summary.data.byStatus[value] ?? 0 }))} />
          <BarChart variant="priority" title="Incidents by priority" data={priorities.map(value => ({ label: value, value: summary.data.byPriority[value] ?? 0 }))} />
          <BarChart variant="category" title="Incidents by category" data={categories.map(value => ({ label: label(value), value: summary.data.byCategory[value] ?? 0 }))
            .sort((a, b) => b.value - a.value)} />
          <BarChart variant="service" title="Incidents by affected service" data={Object.entries(summary.data.byService ?? {})
            .map(([service, value]) => ({ label: service, value }))} />
        </div>
      </section>}
      <div className="itsm-dashboard-grid">
        <section className="admin-panel">
          <div className="admin-panel-heading itsm-heading"><h2>Incident management</h2>
            <button type="button" className="itsm-button secondary"
              onClick={() => { setFilters(emptyFilters); setKeyword(''); setPage(0); }}>Clear filters</button>
          </div>
          <div className="incident-filter-grid">
            <label>Search <input type="search" placeholder="Search incidents" value={keyword} onChange={e => { setKeyword(e.target.value); setPage(0); }} /></label>

            <Select name="status" text="Status" value={filters.status} options={statuses} onChange={filter} />
            <Select name="priority" text="Priority" value={filters.priority} options={priorities} onChange={filter} />
          </div>
          <details className="incident-more-filters">
            <summary>More filters</summary>
            <div className="incident-filter-grid">
            <label>Incident ID <input type="number" min="1" value={filters.incidentId} onChange={e => filter('incidentId', e.target.value)} /></label>
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
          </details>
          {!incidents.data && !incidents.error && <p className="itsm-panel-message" role="status">Loading incidents…</p>}
          {incidents.data?.content.length === 0 && <p className="itsm-panel-message">No matching incidents.</p>}
          {!!incidents.data?.content.length && <div className="admin-table-container itsm-table"><table>
            <thead><tr><th>Incident</th><th>Status</th><th>SLA due</th><th>Technician</th></tr></thead>
            <tbody>{incidents.data.content.map(item => <tr key={item.id}>
              <td><Link to={`/incidents/${item.id}`}>#{item.id} {item.title}</Link> · {item.priority}
                {item.duplicateOfId && <span className="itsm-badge">duplicate of #{item.duplicateOfId}</span>}
                {item.awaitingReporterInfo && <span className="itsm-badge">waiting on reporter</span>}
                {item.outageId && <span className="itsm-badge">outage #{item.outageId}</span>}
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
      {!!atRisk.data?.length && <section className="admin-panel">
        <div className="admin-panel-heading"><h2>Predicted to miss SLA</h2></div>
        <div className="admin-table-container itsm-table"><table>
          <thead><tr><th>Incident</th><th>Predicted resolution</th><th>Due</th><th>Why</th></tr></thead>
          <tbody>{atRisk.data.map(p => <tr key={p.incidentId}>
            <td><Link to={`/incidents/${p.incidentId}`}>#{p.incidentId} {p.title}</Link></td>
            <td>{dateTime(p.predictedResolutionAt)}</td><td>{dateTime(p.resolutionDueAt)}</td>
            <td>{p.basis}{p.workloadFactor !== 1 ? `, workload ×${p.workloadFactor}` : ''}</td>
          </tr>)}</tbody>
        </table></div>
      </section>}
      <section className="admin-panel">
        <div className="admin-panel-heading"><h2>Service health</h2></div>
        {outages.error && <p className="itsm-panel-message" role="alert">{outages.error.message}</p>}
        {outages.data?.length === 0 && <p className="itsm-panel-message">No outages detected. Services look healthy.</p>}
        {!!outages.data?.length && <div className="admin-table-container itsm-table"><table>
          <thead><tr><th>Outage</th><th>Service</th><th>Status</th><th>Detected</th><th>Incidents (open)</th></tr></thead>
          <tbody>{outages.data.map(o => <tr key={o.id}>
            <td>#{o.id}</td><td>{o.serviceName}</td><td>{label(o.status)}</td><td>{dateTime(o.detectedAt)}</td>
            <td>{o.incidentCount} ({o.openIncidents})</td>
          </tr>)}</tbody>
        </table></div>}
      </section>
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
