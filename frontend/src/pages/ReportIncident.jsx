import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { label } from '../api';
import { useApiData } from '../useApiData';
import './ReportIncident.css';

const categories = ['HARDWARE', 'SOFTWARE', 'NETWORK', 'DATABASE', 'CLOUD', 'SECURITY', 'AUTHENTICATION', 'APPLICATION', 'STORAGE'];
const levels = ['LOW', 'MEDIUM', 'HIGH'];

export default function ReportIncident({ request }) {
  const navigate = useNavigate();
  const { data: services, error } = useApiData(request, '/services');
  const [failure, setFailure] = useState(null);
  const [pending, setPending] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  const [suggestion, setSuggestion] = useState(null);
  const [suggesting, setSuggesting] = useState(false);

  async function suggest() {
    if (!title.trim() || suggesting) return;
    setSuggesting(true);
    try {
      setSuggestion(await request('/incidents/classify', { method: 'POST', body: { title: title.trim(), description: description.trim() } }));
    } catch {
      setSuggestion(null); // suggestions are optional; the form still works without them
    } finally {
      setSuggesting(false);
    }
  }

  async function submit(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = {
      title: title.trim(),
      description: description.trim(),
      category,
      impact: form.get('impact'),
      urgency: form.get('urgency'),
      serviceIds: form.getAll('serviceIds').map(Number),
    };
    setPending(true);
    setFailure(null);
    try {
      const incident = await request('/incidents', { method: 'POST', body });
      navigate(`/incidents/${incident.id}`);
    } catch (error) {
      setFailure(error);
    } finally {
      setPending(false);
    }
  }

  return (
    <section className="itsm-panel">
      <h1>Report an incident</h1>
      {error && <p role="alert">{error.message}</p>}
      {!services && !error && <p role="status">Loading services…</p>}
      {services?.length === 0 && <p>No active services are available.</p>}
      {failure && <div role="alert"><p>{failure.message}</p>
        {failure.fields && <ul>{Object.entries(failure.fields).map(([name, message]) => <li key={name}>{label(name)}: {message}</li>)}</ul>}
      </div>}
      <form className="incident-form" onSubmit={submit}>
        <label className="form-group">Title<input name="title" required maxLength={255} value={title}
          onChange={event => setTitle(event.target.value)} /></label>
        <label className="form-group">Description<textarea name="description" required rows={5} value={description}
          onChange={event => setDescription(event.target.value)} onBlur={suggest} /></label>
        <label className="form-group">Category
          <select name="category" required value={category} onChange={event => setCategory(event.target.value)}>
            <option value="" disabled>Choose category</option>
            {categories.map(value => <option key={value} value={value}>{label(value)}</option>)}
          </select>
        </label>
        <div className="itsm-suggestion" aria-live="polite">
          <button type="button" className="itsm-button secondary" disabled={!title.trim() || suggesting} onClick={suggest}>
            {suggesting ? 'Thinking…' : 'Suggest a category'}
          </button>
          {suggestion && <span>
            Suggested: <strong>{label(suggestion.category)}</strong> ({Math.round(suggestion.confidence * 100)}% confident)
            {suggestion.category !== category && <button type="button" className="itsm-link-button"
              onClick={() => setCategory(suggestion.category)}>Use suggestion</button>}
          </span>}
        </div>
        <div className="form-row"><Choice name="impact" choices={levels} /><Choice name="urgency" choices={levels} /></div>
        <fieldset className="itsm-services">
          <legend>Affected services</legend>
          <label className="itsm-service-picker">
            <span>Select one or more</span>
            <select name="serviceIds" multiple required size={Math.min(6, Math.max(2, services?.length ?? 0))}>
              {services?.map(service => <option key={service.id} value={service.id}>{service.name}</option>)}
            </select>
          </label>
        </fieldset>
        <div className="itsm-actions">
          <button className="itsm-button" type="submit" disabled={!services?.length || pending}>{pending ? 'Submitting…' : 'Submit incident'}</button>
          <Link to="/employee" className="itsm-button secondary">Cancel</Link>
        </div>
      </form>
    </section>
  );
}

function Choice({ name, choices }) {
  return <label className="form-group">{label(name)}
    <select name={name} required defaultValue="">
      <option value="" disabled>Choose {name}</option>
      {choices.map(value => <option key={value} value={value}>{label(value)}</option>)}
    </select>
  </label>;
}
