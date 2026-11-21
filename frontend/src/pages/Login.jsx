
import '../App.css';

function Login() {
  return (
    <div className="login-page">
      <div className="login-card">

        <h1>I-ITSM</h1>

        <p className="subtitle">
          Intelligent IT Service Management System
        </p>

        <h2>Welcome!</h2>

        <p className="description">
          Your central platform for reporting and managing IT incidents.
        </p>

        <button className="google-button">
          <span className="google-icon">G</span>
          Continue with Google
        </button>

        <p className="footer-text">
          Secure access to your IT support portal
        </p>

      </div>
    </div>
  );
}

export default Login;
