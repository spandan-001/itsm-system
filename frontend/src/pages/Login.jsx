import { useState } from 'react';
import '../App.css';
import { supabase } from '../api';

export default function Login({ message, disabled, signedIn }) {
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);

  async function authenticate() {
    setPending(true);
    setError('');
    try {
      const { error } = signedIn
        ? await supabase.auth.signOut()
        : await supabase.auth.signInWithOAuth({
            provider: 'google',
            options: { redirectTo: window.location.origin + '/' },
          });
      if (error) throw error;
    } catch (error) {
      setError(error.message);
    } finally {
      setPending(false);
    }
  }

  return (
    <main className="login-page">
      <div className="login-card">
        <h1 className="itsm-brand">
          <span className="itsm-brand-mark" aria-hidden="true"><i /><i /><i /></span>
          ITSM
        </h1>
        <p className="login-description">Sign in to report and track IT incidents.</p>
        {message && <p role="status">{message}</p>}
        {error && <p role="alert">{error}</p>}
        <button className="google-button" type="button" disabled={disabled || pending} onClick={authenticate}>
          {signedIn ? 'Sign out' : <><span className="google-icon" aria-hidden="true">G</span>Continue with Google</>}
        </button>
      </div>
    </main>
  );
}
