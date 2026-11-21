import './Charts.css';

/**
 * Single-series horizontal bar chart. Every bar is labelled with its value, so
 * the list doubles as the table view; hover or focus a row for its share.
 */
export default function BarChart({ title, data, unit = 'incidents' }) {
  const max = Math.max(1, ...data.map(d => d.value));
  const total = data.reduce((sum, d) => sum + d.value, 0);
  return (
    <figure className="itsm-chart">
      <figcaption>{title}</figcaption>
      {total === 0 ? <p className="itsm-chart-empty">No data yet.</p> : <ul className="itsm-bars">
        {data.map(d => <li key={d.label} tabIndex={0} aria-label={`${d.label}: ${d.value} ${unit}`}>
          <span className="itsm-bar-label">{d.label}</span>
          <span className="itsm-bar-track">
            <span className="itsm-bar-fill" style={{ width: `${(d.value / max) * 100}%`, minWidth: d.value ? 2 : 0 }} />
          </span>
          <span className="itsm-bar-value">{d.value}</span>
          <span className="itsm-chart-tip" role="tooltip">{d.label}: {d.value} {unit} ({Math.round((d.value / total) * 100)}%)</span>
        </li>)}
      </ul>}
    </figure>
  );
}
