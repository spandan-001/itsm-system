import { useState } from 'react';
import { useParams } from 'react-router-dom';

import { dateTime, label } from '../api';
import { useApiData } from '../useApiData';
import { useAutoRefresh } from '../useAutoRefresh';

const transitions = {
  EMPLOYEE: { VERIFIED: 'Verify fix', CLOSED: 'Close incident', IN_PROGRESS: 'Reopen incident' },
  TECHNICIAN: { ACKNOWLEDGED: 'Acknowledge', IN_PROGRESS: 'Start work', RESOLVED: 'Resolve' },
  ADMIN: {
    ACKNOWLEDGED: 'Acknowledge', IN_PROGRESS: 'Start work', RESOLVED: 'Resolve',
    VERIFIED: 'Verify fix', CLOSED: 'Close incident',
  },
};

const noteTypes = {
  EMPLOYEE: { ADDITIONAL_INFO: 'Add information', INFO_RESPONSE: 'Answer the technician' },
  TECHNICIAN: { INVESTIGATION: 'Investigation note (internal)', INFO_REQUEST: 'Ask the reporter' },
  ADMIN: { INVESTIGATION: 'Investigation note (internal)', INFO_REQUEST: 'Ask the reporter' },
};
const feedbackStatuses = new Set(['RESOLVED', 'VERIFIED', 'CLOSED']);

export default function IncidentDetails({ request, account }) {
  const { id } = useParams();
  const path = `/incidents/${id}`;
  const incident = useApiData(request, path);
  const sla = useApiData(request, incident.data ? path + '/sla' : null);
  const audit = useApiData(request, incident.data ? path + '/audit' : null);
  const notes = useApiData(request, incident.data ? path + '/notes' : null);
  const prediction = useApiData(request, incident.data ? path + '/sla-prediction' : null);
  const fixes = useApiData(request, incident.data && account.role !== 'EMPLOYEE'
    && !feedbackStatuses.has(incident.data.status) ? path + '/suggested-resolutions' : null);
  const [failure, setFailure] = useState('');
  const [pending, setPending] = useState(false);
  const [comment, setComment] = useState('');

  useAutoRefresh(() => refresh(), 30000);

  function refresh() {
    incident.reload();
    sla.reload();
    audit.reload();
    notes.reload();
    prediction.reload();
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
    <article className="itsm-panel itsm-detail">
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
      {item.escalationLevel && item.escalationLevel !== 'NONE' && <p className="itsm-banner">
        Escalated to {label(item.escalationLevel)} at {dateTime(item.escalatedAt)}: not acknowledged within the response target.</p>}
      {item.outageId && <p className="itsm-banner">Part of suspected service outage #{item.outageId}:
        several incidents were reported against the same service at the same time.</p>}
      {item.awaitingReporterInfo && <p className="itsm-banner">Waiting for more information from the reporter.</p>}
      {item.duplicateOfId && <p className="itsm-banner">Linked as a duplicate of incident #{item.duplicateOfId}.
        Updates on the original are shared with you.</p>}
      {item.autoReclassified && <p className="itsm-banner">Category corrected from {label(item.reportedCategory)} to{' '}
        {label(item.category)} by the classifier ({Math.round(item.classificationConfidence * 100)}% confident).</p>}
      {!item.autoReclassified && account.role !== 'EMPLOYEE' && item.suggestedCategory && item.suggestedCategory !== item.category
        && <p className="itsm-banner">The classifier thinks this may be {label(item.suggestedCategory)}
          {' '}({Math.round(item.classificationConfidence * 100)}%).</p>}
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
      {fixes.data?.length > 0 && <section className="itsm-section">
        <h2>Suggested fixes from similar incidents</h2>
        <ol className="itsm-thread">
          {fixes.data.map(fix => <li key={fix.incidentId}>
            <strong>#{fix.incidentId} {fix.title}</strong>
            {fix.confirmed && <span className="itsm-badge">confirmed fix</span>}
            {fix.feedbackRating && <span className="itsm-badge">rated {fix.feedbackRating}/5</span>}
            <p>{fix.resolution}</p>
            {canResolve && <button type="button" className="itsm-link-button" onClick={() => setComment(fix.resolution)}>
              Use as resolution summary</button>}
          </li>)}
        </ol>
      </section>}
      <section className="itsm-section">
        <h2>Conversation</h2>
        {notes.error && <p role="alert">{notes.error.message}</p>}
        {notes.data?.length === 0 && <p>No notes yet.</p>}
        {notes.data?.length > 0 && <ol className="itsm-thread">
          {notes.data.map(note => <li key={note.id} className={note.internal ? 'internal' : undefined}>
            <strong>{note.authorName}</strong> · {label(note.type)} · {dateTime(note.createdAt)}
            {note.internal && <span className="itsm-badge">internal</span>}
            <p>{note.body}</p>
          </li>)}
        </ol>}
        {item.status !== 'CLOSED' && !item.duplicateOfId && noteTypes[account.role]
          && <NoteForm request={request} path={path} types={noteTypes[account.role]} onSaved={refresh} />}
      </section>
      {account.role === 'EMPLOYEE' && feedbackStatuses.has(item.status) && <section className="itsm-section">
        <h2>Feedback</h2>
        {item.feedbackRating
          ? <p>You rated this resolution {item.feedbackRating}/5{item.feedbackComment ? `: ${item.feedbackComment}` : '.'}</p>
          : <FeedbackForm request={request} path={path} onSaved={refresh} />}
      </section>}
      <section className="itsm-section">
        <h2>SLA</h2>
        {sla.error && <p role="alert">{sla.error.message}</p>}
        {sla.data && <dl className="itsm-facts">
          <div><dt>Response</dt><dd>{label(sla.data.responseState)} · {dateTime(sla.data.responseDueAt)}</dd></div>
          <div><dt>Resolution</dt><dd>{label(sla.data.resolutionState)} · {dateTime(sla.data.resolutionDueAt)}</dd></div>
          {prediction.data && prediction.data.risk !== 'NOT_APPLICABLE' && <div>
            <dt>Predicted resolution</dt>
            <dd>{dateTime(prediction.data.predictedResolutionAt)} · {label(prediction.data.risk)} risk
              <br /><small>{prediction.data.basis}{prediction.data.workloadFactor !== 1 ? `, workload ×${prediction.data.workloadFactor}` : ''}</small></dd>
          </div>}
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

function NoteForm({ request, path, types, onSaved }) {
  const [type, setType] = useState(Object.keys(types)[0]);
  const [body, setBody] = useState('');
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState('');

  async function submit(event) {
    event.preventDefault();
    setPending(true);
    setFailure('');
    try {
      await request(path + '/notes', { method: 'POST', body: { type, body: body.trim() } });
      setBody('');
      onSaved();
    } catch (error) {
      setFailure(error.message);
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit}>
      {failure && <p role="alert">{failure}</p>}
      <label className="form-group">Note type
        <select value={type} onChange={event => setType(event.target.value)}>
          {Object.entries(types).map(([value, text]) => <option key={value} value={value}>{text}</option>)}
        </select>
      </label>
      <label className="form-group">Message
        <textarea rows={3} maxLength={4000} required value={body} onChange={event => setBody(event.target.value)} />
      </label>
      <div className="itsm-actions">
        <button className="itsm-button" type="submit" disabled={pending || !body.trim()}>{pending ? 'Saving…' : 'Add note'}</button>
      </div>
    </form>
  );
}

function FeedbackForm({ request, path, onSaved }) {
  const [rating, setRating] = useState('');
  const [comment, setComment] = useState('');
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState('');

  async function submit(event) {
    event.preventDefault();
    setPending(true);
    setFailure('');
    try {
      const text = comment.trim();
      await request(path + '/feedback', { method: 'POST', body: text ? { rating: Number(rating), comment: text } : { rating: Number(rating) } });
      onSaved();
    } catch (error) {
      setFailure(error.message);
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit}>
      {failure && <p role="alert">{failure}</p>}
      <label className="form-group">How well was this resolved?
        <select required value={rating} onChange={event => setRating(event.target.value)}>
          <option value="" disabled>Choose a rating</option>
          {[5, 4, 3, 2, 1].map(value => <option key={value} value={value}>{value} / 5</option>)}
        </select>
      </label>
      <label className="form-group">Comment (optional)
        <textarea rows={2} maxLength={1000} value={comment} onChange={event => setComment(event.target.value)} />
      </label>
      <div className="itsm-actions">
        <button className="itsm-button" type="submit" disabled={pending || !rating}>{pending ? 'Sending…' : 'Send feedback'}</button>
      </div>
    </form>
  );
}
