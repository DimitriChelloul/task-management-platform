import React, { useEffect, useState } from "react";
import { fetchTasks, createTask, updateTask, deleteTask } from "../api/tasks";

export default function Tasks() {
  const [tasks, setTasks] = useState([]);
  const [title, setTitle] = useState("");

  useEffect(() => { load(); }, []);

  async function load() {
    try { setTasks(await fetchTasks()); } catch (e) { console.error(e); }
  }

  async function add() {
    if (!title.trim()) return;
    await createTask({ title, description: "", done: false });
    setTitle("");
    await load();
  }

  async function toggle(task) {
    await updateTask(task.id, { ...task, done: !task.done });
    await load();
  }

  async function remove(id) {
    await deleteTask(id);
    await load();
  }

  return (
    <div>
      <div style={{ marginBottom: 12 }}>
        <input value={title} onChange={e => setTitle(e.target.value)} placeholder="Nouvelle tâche" />
        <button onClick={add} style={{ marginLeft: 8 }}>Ajouter</button>
      </div>
      <ul>
        {tasks.map(t => (
          <li key={t.id} style={{ marginBottom: 6 }}>
            <input type="checkbox" checked={t.done} onChange={() => toggle(t)} />
            <span style={{ marginLeft: 8 }}>{t.title}</span>
            <button onClick={() => remove(t.id)} style={{ marginLeft: 12 }}>Supprimer</button>
          </li>
        ))}
      </ul>
    </div>
  );
}
