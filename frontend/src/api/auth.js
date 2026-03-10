const API_BASE = process.env.REACT_APP_API_BASE || "";
const TOKEN_KEY = "task_platform_token";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function setToken(token) {
  if (!token) return;
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export async function login() {
  const res = await fetch(`${API_BASE}/auth/login`, { method: "POST" });
  if (!res.ok) throw new Error("Login failed");
  const data = await res.json();
  if (!data.token) throw new Error("Token missing in response");
  setToken(data.token);
  return data.token;
}

export async function register(email) {
  const res = await fetch(`${API_BASE}/users?email=${encodeURIComponent(email)}`, {
    method: "POST",
  });
  if (!res.ok) throw new Error("Register failed");
  return res.json();
}
