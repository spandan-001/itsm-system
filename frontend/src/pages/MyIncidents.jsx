import { useState } from 'react';
import { Link } from 'react-router-dom';

import { dateTime, label } from '../api';
import { useApiData } from '../useApiData';

export default function MyIncidents({ request, account }) {
  const [page, setPage] = useState(0);
  const { data, error, reload } = useApiData(request, `/incidents?page=${page}`);
  const employee = account.role === 'EMPLOYEE';

  return (
    <section className="itsm-panel">
      <div className="itsm-heading">
        <h1>{employee ? 'My incidents' : 'Assigned incidents'}</h1>
        {employee && <Link className="itsm-button" to="/report-incident">Report an incident</Link>}
        <button className="itsm-button secondary" type="button" onClick={reload}>Refresh</button>
      </div>
      {error && <p role="alert">{error.message}</p>}
      {!data && !error && <p role="status">Loading incidents…</p>}
      {data && (data.content.length ? (
        <div className="itsm-table">
          <table>
            <thead><tr><th>Incident</th><th>Priority</th><th>Status</th><th>Resolution due</th></tr></thead>
            <tbody>
              {data.content.map(incident => (
                <tr key={incident.id}>
                  <td><Link to={`/incidents/${incident.id}`}>#{incident.id} {incident.title}</Link>
                    {incident.duplicateOfId && <span className="itsm-badge">linked to #{incident.duplicateOfId}</span>}
                    {incident.awaitingReporterInfo && <span className="itsm-badge">{employee ? 'your reply needed' : 'waiting on reporter'}</span>}
                  </td>
                  <td>{incident.priority}</td>
                  <td>{label(incident.status)}</td>
                  <td>{dateTime(incident.resolutionDueAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : <p>No incidents on this page.</p>)}
      {data && (page > 0 || data.page.totalPages > 1) && (
        <div className="itsm-actions">
          <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
          <span>Page {page + 1}</span>
          <button type="button" disabled={page + 1 >= data.page.totalPages} onClick={() => setPage(page + 1)}>Next</button>
        </div>
      )}
      {!employee && <Availability request={request} accountId={account.accountId} onChange={reload} />}
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
    <section className="itsm-section">
      <h2>Availability</h2>
      {(error || failure) && <p role="alert">{failure || error.message}</p>}
      {data && <button type="button" className="itsm-button secondary" disabled={pending} onClick={toggle}>
        {data.available ? 'Set unavailable' : 'Set available'}
      </button>}
    </section>
  );
}
