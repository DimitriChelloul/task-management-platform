import { getToken } from "./auth";

const API_BASE = process.env.REACT_APP_API_BASE || "";

export async function fetchTasks() {
  const token = getToken();
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  const res = await fetch(`${API_BASE}/tasks`, { headers });
  if (!res.ok) {
    if (res.status === 401) {
      throw new Error("Unauthorized: please login first");
    }
    throw new Error("Failed to fetch tasks");
  }
  return res.json();
}
