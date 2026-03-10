import React, { useState } from "react";
import { register } from "../api/auth";

export default function Register() {
  const [email, setEmail] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    if (!email.trim()) return;

    setLoading(true);
    setStatus("");
    try {
      const result = await register(email.trim());
      setStatus(`User created: ${result.id || "ok"}`);
      setEmail("");
    } catch (err) {
      setStatus(err.message || "Register failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="card">
      <h2>Register</h2>
      <p className="muted">Calls POST /users?email=...</p>
      <form onSubmit={onSubmit} className="column">
        <input
          type="email"
          placeholder="email@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <button type="submit" disabled={loading}>
          {loading ? "Creating..." : "Create account"}
        </button>
      </form>
      {status && <p>{status}</p>}
    </section>
  );
}
