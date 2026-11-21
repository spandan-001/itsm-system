import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { label, supabase } from '../api';
import './EmployeeDashboard.css';
import '../DashboardTheme.css';
import './Integration.css';

export default function AccountLayout({ account, home, children }) {
  const [error, setError] = useState('');
  async function signOut() {
    const { error } = await supabase.auth.signOut();
    if (error) setError(error.message);
  }

  return (
    <div className="dashboard">
      <aside className="sidebar">
        <h2>I-ITSM</h2>
        <p className="sidebar-subtitle">IT service management</p>
        <nav aria-label="Workspace">
          {account.role && <NavLink to={home}>My incidents</NavLink>}
          {account.role === 'EMPLOYEE' && <NavLink to="/report-incident">Report an incident</NavLink>}
          {account.role && <NavLink to="/notifications">Notifications</NavLink>}
          {account.admin && <NavLink to="/admin">Administration</NavLink>}
        </nav>
        <div className="sidebar-bottom">
          <button type="button" className="itsm-button secondary" onClick={signOut}>Sign out</button>
        </div>
      </aside>
      <main className="main-content">
        <header className="topbar">
          <span>{account.email}</span>
          <span className="itsm-tag">{account.role ? label(account.role) : 'Administrator'}</span>
        </header>
        {error && <p role="alert">{error}</p>}
        {children}
      </main>
    </div>
  );
}
