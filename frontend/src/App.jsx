import { lazy, Suspense, useCallback, useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';

import { api, supabase } from './api';
import AccountLayout from './pages/AccountLayout';
import Login from './pages/Login';
import MyIncidents from './pages/MyIncidents';
import ReportIncident from './pages/ReportIncident';

const IncidentDetails = lazy(() => import('./pages/IncidentDetails'));
const Notifications = lazy(() => import('./pages/Notifications'));
const AdminDashboard = lazy(() => import('./pages/AdminDashboard'));

function App() {
  const [session, setSession] = useState(undefined);
  const [identity, setIdentity] = useState(null);

  useEffect(() => {
    if (!supabase) return;
    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, nextSession) => {
      if (event === 'SIGNED_OUT') setIdentity(null);
      setSession(nextSession);
    });
    return () => subscription.unsubscribe();
  }, []);

  const accessToken = session?.access_token;
  useEffect(() => {
    if (!accessToken) return;
    const controller = new AbortController();
    api(accessToken, '/me', { signal: controller.signal })
      .then(account => setIdentity({ accessToken, account }))
      .catch(error => {
        if (!controller.signal.aborted) setIdentity({ accessToken, error });
      });
    return () => controller.abort();
  }, [accessToken]);

  const request = useCallback((path, options) => api(accessToken, path, options), [accessToken]);
  if (!supabase) return <Login message="Configure the Supabase URL and publishable key to sign in." disabled />;
  if (session === undefined) return <Login message="Checking your session…" disabled />;
  if (!session) {
    const failed = new URLSearchParams(window.location.search).has('error');
    return <Login message={failed ? 'Google sign-in didn’t finish. Please try again.' : undefined} />;
  }
  if (identity?.accessToken !== accessToken) return <Login message="Checking your ITSM access…" disabled />;
  if (identity.error) {
    const message = identity.error.status === 403
      ? 'Your Google account is signed in. Ask the ITSM team to link it to an employee or technician.'
      : identity.error.message;
    return <Login message={message} signedIn />;
  }

  const account = identity.account;
  const home = account.role === 'EMPLOYEE' ? '/employee' : account.role === 'TECHNICIAN' ? '/technician' : '/admin';
  return (
    <BrowserRouter>
      <AccountLayout account={account} home={home} request={request}>
        <Suspense fallback={<p role="status">Loading workspace…</p>}>
          <Routes>
            <Route path="/employee" element={account.role === 'EMPLOYEE' ? <MyIncidents request={request} account={account} /> : <Navigate to={home} replace />} />
            <Route path="/technician" element={account.role === 'TECHNICIAN' ? <MyIncidents request={request} account={account} /> : <Navigate to={home} replace />} />
            <Route path="/my-incidents" element={account.role ? <MyIncidents request={request} account={account} overview={false} /> : <Navigate to={home} replace />} />
            <Route path="/report-incident" element={account.role === 'EMPLOYEE' ? <ReportIncident request={request} /> : <Navigate to={home} replace />} />
            <Route path="/incidents/:id" element={account.role ? <IncidentDetails request={request} account={account} /> : <Navigate to={home} replace />} />
            <Route path="/notifications" element={account.role ? <Notifications request={request} /> : <Navigate to={home} replace />} />
            <Route path="/admin" element={account.admin ? <AdminDashboard request={request} /> : <Navigate to={home} replace />} />
            <Route path="*" element={<Navigate to={home} replace />} />
          </Routes>
        </Suspense>
      </AccountLayout>
    </BrowserRouter>
  );
}

export default App;
