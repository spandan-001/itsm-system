import { useState } from 'react';
import { useParams } from 'react-router-dom';

import { dateTime, label } from '../api';
import { useApiData } from '../useApiData';

const transitions = {
  EMPLOYEE: { VERIFIED: 'Verify fix', CLOSED: 'Close incident', IN_PROGRESS: 'Reopen incident' },
  TECHNICIAN: { ACKNOWLEDGED: 'Acknowledge', IN_PROGRESS: 'Start work', RESOLVED: 'Resolve' },
  ADMIN: {
    ACKNOWLEDGED: 'Acknowledge', IN_PROGRESS: 'Start work', RESOLVED: 'Resolve',
    VERIFIED: 'Verify fix', CLOSED: 'Close incident',
  },
};

export default function IncidentDetails({ request, account }) {
  const { id } = useParams();
  const path = `/incidents/${id}`;
  const incident = useApiData(request, path);
  const sla = useApiData(request, incident.data ? path + '/sla' : null);
  const audit = useApiData(request, incident.data ? path + '/audit' : null);
  const [failure, setFailure] = useState('');
  const [pending, setPending] = useState(false);
  const [comment, setComment] = useState('');

  function refresh() {
    incident.reload();
    sla.reload();
    audit.reload();
  }

  async function changeStatus(status) {
    setPending(true);
    setFailure('');
    try {
      const text = comment.trim();
      await request(path + '/status', { method: 'PATCH', body: text ? { status, comment: text } : { status } });
      setComment('');
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
  // Employees may only move to IN_PROGRESS by reopening a resolved incident.
  const allowed = item.allowedTransitions?.filter(status => actions[status]
    && (status !== 'IN_PROGRESS' || account.role !== 'EMPLOYEE' || item.status === 'RESOLVED')) ?? [];
  const canResolve = allowed.includes('RESOLVED');
  const canReopen = item.status === 'RESOLVED' && allowed.includes('IN_PROGRESS');

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
        {item.resolutionSummary && <div><dt>Resolution</dt><dd>{item.resolutionSummary}</dd></div>}
      </dl>
      {failure && <p role="alert">{failure}</p>}
      {(canResolve || canReopen) && <label className="form-group">
        {canResolve ? 'Resolution summary (required to resolve)' : 'Reason for reopening (optional)'}
        <textarea rows={3} maxLength={2000} value={comment} onChange={event => setComment(event.target.value)} />
      </label>}
      <div className="itsm-actions">
        {allowed.map(status => <button className="itsm-button" type="button" key={status}
          disabled={pending || (status === 'RESOLVED' && !comment.trim())} onClick={() => changeStatus(status)}>
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
            <small>{event.actor === 'system' ? 'System' : event.actor === account.email ? 'You' : event.actor}</small>
          </li>)}
        </ol>}
      </section>
    </article>
  );
}
