import React, { useEffect, useMemo, useState } from "react";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Tasks from "./pages/Tasks";

const VIEWS = {
  login: "login",
  register: "register",
  tasks: "tasks",
};

function hashToView(hash) {
  const value = (hash || "").replace("#", "").toLowerCase();
  return VIEWS[value] || VIEWS.login;
}

export default function App() {
  const [view, setView] = useState(hashToView(window.location.hash));

  useEffect(() => {
    const onHashChange = () => setView(hashToView(window.location.hash));
    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, []);

  function go(next) {
    window.location.hash = next;
  }

  const page = useMemo(() => {
    if (view === VIEWS.register) return <Register />;
    if (view === VIEWS.tasks) return <Tasks />;
    return <Login />;
  }, [view]);

  return (
    <main className="layout">
      <header className="header">
        <h1>Task Management Platform</h1>
        <nav className="row">
          <button
            className={view === VIEWS.login ? "active" : "ghost"}
            onClick={() => go(VIEWS.login)}
          >
            Login
          </button>
          <button
            className={view === VIEWS.register ? "active" : "ghost"}
            onClick={() => go(VIEWS.register)}
          >
            Register
          </button>
          <button
            className={view === VIEWS.tasks ? "active" : "ghost"}
            onClick={() => go(VIEWS.tasks)}
          >
            Tasks
          </button>
        </nav>
      </header>
      {page}
    </main>
  );
}
