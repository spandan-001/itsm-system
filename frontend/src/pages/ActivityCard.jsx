import { dateTime } from '../api';

export default function ActivityCard({ title, entries, error, empty }) {
  return (
    <section className="itsm-activity-card">
      <h2>{title}</h2>
      {error && <p role="alert">{error.message}</p>}
      {!entries && !error && <p role="status">Loading activity…</p>}
      {entries?.length === 0 && <p>{empty}</p>}
      {!!entries?.length && (
        <ol className="itsm-activity">
          {entries.map(({ id, title, detail, at }) => (
            <li key={id}>
              {at && <time dateTime={at}>{dateTime(at)}</time>}
              <strong>{title}</strong>
              <span>{detail}</span>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
