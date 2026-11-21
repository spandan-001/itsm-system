import { useEffect, useRef, useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';

import { supabase } from '../api';
import { useApiData } from '../useApiData';
import './EmployeeDashboard.css';
import './TechnicianDashboard.css';
import './AdminDashboard.css';
import '../DashboardTheme.css';
import './Integration.css';
import '../OliveTheme.css';


const workspace = {
  employee: ['dashboard', 'main-content'],
  technician: ['technician-dashboard', 'tech-main'],
  admin: ['admin-dashboard', 'admin-main'],
};

export default function AccountLayout({ account, home, request, children }) {
  const location = useLocation();
  const role = location.pathname === '/admin' || !account.role ? 'admin' : account.role.toLowerCase();
  const [shell, main] = workspace[role];
  const [error, setError] = useState('');
  const profile = useRef(null);
  const profileButton = useRef(null);
  const accountRole = account.role === 'EMPLOYEE' ? 'Employee' : account.role === 'TECHNICIAN' ? 'Technician' : 'Administrator';
  const initials = account.email?.split('@')[0].split(/[._-]/).filter(Boolean)
    .slice(0, 2).map(part => part[0].toUpperCase()).join('') || accountRole[0];

  useEffect(() => {
    function closeProfile(event) {
      if (!profile.current?.open) return;
      if (event.key === 'Escape') {
        profile.current.open = false;
        profileButton.current?.focus();
      } else if (event.type === 'pointerdown' && !profile.current.contains(event.target)) {
        profile.current.open = false;
      }
    }

    document.addEventListener('keydown', closeProfile);
    document.addEventListener('pointerdown', closeProfile);
    return () => {
      document.removeEventListener('keydown', closeProfile);
      document.removeEventListener('pointerdown', closeProfile);
    };
  }, []);
  async function signOut() {
    const { error } = await supabase.auth.signOut({ scope: 'local' });
    if (error) setError(error.message);
  }

  return (
    <div className={`${shell} itsm-shell`}>
      <header className="itsm-topbar">
        <NavLink className="itsm-brand" to={home} aria-label="ITSM home">
          <span className="itsm-brand-mark" aria-hidden="true"><i /><i /><i /></span>
          ITSM
        </NavLink>
        <nav aria-label="Workspace">
          <NavLink to={home} end>Overview</NavLink>
          {account.role && <NavLink to="/my-incidents">{account.role === 'EMPLOYEE' ? 'My incidents' : 'Assigned incidents'}</NavLink>}
          {account.role && (
            <NavLink className="itsm-nav-count" to="/notifications">
              Notifications <UnreadBadge key={location.pathname} request={request} />
            </NavLink>
          )}
          {account.role === 'EMPLOYEE' && <NavLink to="/report-incident">Report an incident</NavLink>}
          {account.admin && home !== '/admin' && <NavLink to="/admin">Administration</NavLink>}
        </nav>
        <details className="itsm-profile" ref={profile}>
          <summary className="itsm-profile-trigger" aria-label="View profile" ref={profileButton}>{initials}</summary>
          <div className="itsm-profile-panel">
            <h2>Profile</h2>
            <p>{accountRole}{account.admin && account.role ? ' · Administrator' : ''}</p>
            {account.email && <p>{account.email}</p>}
            <button type="button" className="tech-signout" onClick={signOut}>Sign out</button>
          </div>
        </details>
      </header>
      <main className={`${main} itsm-main`}>
        {error && <p role="alert" className="itsm-alert">{error}</p>}
        {children}
      </main>
    </div>
  );
}

function UnreadBadge({ request }) {
  const { data } = useApiData(request, '/notifications/unread-count');
  return data?.unread > 0
    ? <span className="itsm-tag" aria-label={`${data.unread} unread`}>{data.unread}</span>
    : null;
}
