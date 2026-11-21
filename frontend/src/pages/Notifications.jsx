import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { dateTime } from '../api';
import { useApiData } from '../useApiData';

export default function Notifications({ request }) {
  const navigate = useNavigate();
  const inbox = useApiData(request, '/notifications');
  const count = useApiData(request, '/notifications/unread-count');
  const [failure, setFailure] = useState('');
  const [pending, setPending] = useState(null);
  const items = inbox.data?.filter(item => item.channel === 'IN_APP');

  async function open(item) {
    setPending(item.id);
    setFailure('');
    try {
      if (!item.read) await request(`/notifications/${item.id}/read`, { method: 'PATCH' });
      if (item.incidentId) navigate(`/incidents/${item.incidentId}`);
      else refresh();
    } catch (error) {
      setFailure(error.message);
    } finally {
      setPending(null);
    }
  }

  function refresh() {
    inbox.reload();
    count.reload();
  }

  return (
    <section className="itsm-panel itsm-detail">
      <div className="itsm-heading"><h1>Notifications {count.data && <span className="itsm-tag">{count.data.unread} unread</span>}</h1>
        <button type="button" className="itsm-button secondary" onClick={refresh}>Refresh</button>
      </div>
      {(failure || inbox.error || count.error) && <p role="alert">{failure || inbox.error?.message || count.error?.message}</p>}
      {!items && !inbox.error && <p role="status">Loading notifications…</p>}
      {items?.length === 0 && <p>No notifications yet.</p>}
      {!!items?.length && <ul className="itsm-timeline">
        {items.map(item => <li key={item.id}>
          <button type="button" className="itsm-notification" disabled={pending !== null} onClick={() => open(item)}>
            <strong>{item.subject}{item.read ? '' : ' · Unread'}</strong>
            <span>{item.message}</span>
            <small>{dateTime(item.createdAt)}</small>
          </button>
        </li>)}
      </ul>}
    </section>
  );
}
