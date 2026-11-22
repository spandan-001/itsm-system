import './Charts.css';

/** Single-series column chart for change over time (e.g. incidents per day). */
export default function ColumnChart({ title, data, unit = 'incidents' }) {
  const max = Math.max(1, ...data.map(d => d.value));
  return (
    <figure className="itsm-chart">
      <figcaption>{title}</figcaption>
      <ul className="itsm-columns">
        {data.map(d => <li key={d.label} tabIndex={0} aria-label={`${d.label}: ${d.value} ${unit}`}>
          <span className="itsm-column-value">{d.value}</span>
          <span className="itsm-column-track">
            <span className="itsm-column-fill" style={{ height: `${(d.value / max) * 100}%`, minHeight: d.value ? 2 : 0 }} />
          </span>
          <span className="itsm-column-label">{d.label}</span>
          <span className="itsm-chart-tip" role="tooltip">{d.label}: {d.value} {unit}</span>
        </li>)}
      </ul>
    </figure>
  );
}
