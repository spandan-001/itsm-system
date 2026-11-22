import { useState } from 'react';
import '../App.css';
import { supabase } from '../api';

export default function Login({ message, disabled, signedIn }) {
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);

  function signInWithPassword(event) {
    event.preventDefault();
    const fields = new FormData(event.currentTarget);
    return authenticate(() => supabase.auth.signInWithPassword({
      email: fields.get('email').trim(),
      password: fields.get('password'),
    }));
  }

  async function authenticate(action) {
    setPending(true);
    setError('');
    try {
      const { error } = await action();
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
        {signedIn ? (
          <button className="google-button" type="button" disabled={disabled || pending}
            onClick={() => authenticate(() => supabase.auth.signOut())}>Sign out</button>
        ) : (
          <>
            <form className="login-form" onSubmit={signInWithPassword}>
              <label>Email<input name="email" type="email" autoComplete="username" required disabled={disabled || pending} /></label>
              <label>Password<input name="password" type="password" autoComplete="current-password" required disabled={disabled || pending} /></label>
              <button className="google-button login-submit" type="submit" disabled={disabled || pending}>Sign in</button>
            </form>
            <p className="login-divider">or</p>
            <button className="google-button" type="button" disabled={disabled || pending}
              onClick={() => authenticate(() => supabase.auth.signInWithOAuth({
                provider: 'google',
                options: { redirectTo: window.location.origin + '/' },
              }))}>
              <span className="google-icon" aria-hidden="true">G</span>Continue with Google
            </button>
          </>
        )}
      </div>
    </main>
  );
}
