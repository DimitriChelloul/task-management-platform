import React, { useEffect, useState } from "react";
import { fetchTasks } from "../api/tasks";

export default function Tasks() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    setError("");
    try {
      const data = await fetchTasks();
      setTasks(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(e.message || "Could not load tasks");
      setTasks([]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="card">
      <h2>Tasks</h2>
      <p className="muted">Reads tasks from /tasks with Bearer token.</p>
      <button onClick={load} disabled={loading}>
        {loading ? "Loading..." : "Refresh"}
      </button>

      {error && <p className="error">{error}</p>}

      <ul className="task-list">
        {tasks.map((task) => (
          <li key={task.id}>
            <strong>{task.title}</strong>
            <span className="muted"> #{task.id}</span>
          </li>
        ))}
      </ul>

      {!loading && !error && tasks.length === 0 && (
        <p className="muted">No tasks returned by API.</p>
      )}
    </section>
  );
}
