import { useRef, useState } from 'react';
import './CreateUser.css';

export default function CreateUser({ request, categories }) {
  const dialog = useRef(null);
  const [role, setRole] = useState('EMPLOYEE');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const body = Object.fromEntries(new FormData(form));
    setBusy(true);
    setMessage('');
    try {
      await request('/admin/users', { method: 'POST', body });
      form.reset();
      setRole('EMPLOYEE');
      setMessage('Account created. The user can sign in now.');
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }
  return <>
    <button type="button" className="itsm-button" onClick={() => dialog.current.showModal()}>Create user</button>
    <dialog className="create-user-dialog" ref={dialog} aria-labelledby="create-user-title">
      <header><h2 id="create-user-title">Create user</h2>
        <button type="button" aria-label="Close form" onClick={() => dialog.current.close()}>×</button>
      </header>
    <form className="create-user-form" onSubmit={submit}>
      <label>Full name<input name="name" required maxLength={100} /></label>
      <label>Email address<input name="email" type="email" required /></label>
      <label>Password<input name="password" type="password" required minLength={12} maxLength={72} placeholder="At least 12 characters" autoComplete="new-password" /></label>
      <label>Role<select name="role" value={role} onChange={event => setRole(event.target.value)}>
        <option value="EMPLOYEE">Employee</option><option value="TECHNICIAN">Technician</option>
      </select></label>
      {role === 'TECHNICIAN' && <label>Specialty<select name="domain">{categories.map(value => <option key={value}>{value}</option>)}</select></label>}
      <div className="create-user-actions">
        <button className="itsm-button" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button>
        {message && <p role="status">{message}</p>}
      </div>
    </form>
  </dialog></>;
}
