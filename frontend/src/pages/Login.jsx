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
        <h1>I-ITSM</h1>
        <p className="subtitle">Intelligent IT Service Management System</p>
        <h2>Welcome</h2>
        {message && <p role="status">{message}</p>}
        {error && <p role="alert">{error}</p>}
        <button className="google-button" type="button" disabled={disabled || pending} onClick={authenticate}>
          {signedIn ? 'Sign out' : 'Continue with Google'}
        </button>
      </div>
    </main>
  );
}
