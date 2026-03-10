import React, { useState } from "react";
import { clearToken, login } from "../api/auth";

export default function Login() {
  const [token, setToken] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);

  async function onLogin() {
    setLoading(true);
    setStatus("");
    try {
      const newToken = await login();
      setToken(newToken);
      setStatus("Login success. Token saved in localStorage.");
    } catch (e) {
      setStatus(e.message || "Login failed");
    } finally {
      setLoading(false);
    }
  }

  function onLogout() {
    clearToken();
    setToken("");
    setStatus("Token cleared.");
  }

  return (
    <section className="card">
      <h2>Login</h2>
      <p className="muted">Calls POST /auth/login and stores JWT token.</p>
      <div className="row">
        <button onClick={onLogin} disabled={loading}>
          {loading ? "Logging in..." : "Login"}
        </button>
        <button onClick={onLogout} className="ghost">
          Logout
        </button>
      </div>
      {status && <p>{status}</p>}
      {token && (
        <textarea readOnly value={token} rows={6} className="token-box" />
      )}
    </section>
  );
}
