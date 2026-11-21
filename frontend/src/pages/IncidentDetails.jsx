import { useState } from 'react';
import { useParams } from 'react-router-dom';

import { dateTime, label } from '../api';
import { useApiData } from '../useApiData';

const transitions = {
  EMPLOYEE: { VERIFIED: 'Verify fix', CLOSED: 'Close incident' },
  TECHNICIAN: { ACKNOWLEDGED: 'Acknowledge', IN_PROGRESS: 'Start work', RESOLVED: 'Resolve' },
};

export default function IncidentDetails({ request, account }) {
  const { id } = useParams();
  const path = `/incidents/${id}`;
  const incident = useApiData(request, path);
  const sla = useApiData(request, incident.data ? path + '/sla' : null);
  const audit = useApiData(request, incident.data ? path + '/audit' : null);
  const [failure, setFailure] = useState('');
  const [pending, setPending] = useState(false);

  function refresh() {
    incident.reload();
    sla.reload();
    audit.reload();
  }

  async function changeStatus(status) {
    setPending(true);
    setFailure('');
    try {
      await request(path + '/status', { method: 'PATCH', body: { status } });
      refresh();
    } catch (error) {
      setFailure(error.message);
    } finally {
      setPending(false);
    }
  }

  if (incident.error) return <p role="alert">{incident.error.message}</p>;
  if (!incident.data) return <p role="status">Loading incident…</p>;
  const item = incident.data;
  const actions = transitions[account.role] ?? {};
  const allowed = item.allowedTransitions?.filter(status => actions[status]) ?? [];

  return (
    <article className="itsm-panel">
      <div className="itsm-heading"><h1>#{item.id} {item.title}</h1>
        <button type="button" className="itsm-button secondary" onClick={refresh}>Refresh</button>
      </div>
      <p>{item.description}</p>
      <dl className="itsm-facts">
        <div><dt>Priority</dt><dd>{item.priority}</dd></div>
        <div><dt>Status</dt><dd>{label(item.status)}</dd></div>
        <div><dt>Technician</dt><dd>{item.assignedTechnician?.name ?? 'Unassigned'}</dd></div>
        <div><dt>Category</dt><dd>{label(item.category)}</dd></div>
      </dl>
      {failure && <p role="alert">{failure}</p>}
      <div className="itsm-actions">
        {allowed.map(status => <button className="itsm-button" type="button" key={status} disabled={pending} onClick={() => changeStatus(status)}>
          {item.status === 'RESOLVED' && status === 'IN_PROGRESS' ? 'Reopen incident' : actions[status]}
        </button>)}
      </div>
      <section className="itsm-section">
        <h2>SLA</h2>
        {sla.error && <p role="alert">{sla.error.message}</p>}
        {sla.data && <dl className="itsm-facts">
          <div><dt>Response</dt><dd>{label(sla.data.responseState)} · {dateTime(sla.data.responseDueAt)}</dd></div>
          <div><dt>Resolution</dt><dd>{label(sla.data.resolutionState)} · {dateTime(sla.data.resolutionDueAt)}</dd></div>
        </dl>}
        {!sla.data && !sla.error && <p role="status">Loading SLA…</p>}
      </section>
      <section className="itsm-section">
        <h2>Audit timeline</h2>
        {audit.error && <p role="alert">{audit.error.message}</p>}
        {audit.data?.length === 0 && <p>No audit events recorded.</p>}
        {audit.data?.length > 0 && <ol className="itsm-timeline">
          {audit.data.map(event => <li key={event.id}>
            <strong>{label(event.action)}</strong> · {dateTime(event.occurredAt)}
            <p>{event.details}</p>
            <small>{event.actor === account.userId ? 'You' : event.actor === 'system' ? 'System' : `Account ${event.actor}`}</small>
          </li>)}
        </ol>}
      </section>
    </article>
  );
}
