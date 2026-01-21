// frontend/src/api.js
// Minimal fetch wrapper with HTTP Basic saved in localStorage.
// Returns the native Response (so your pages can do res.ok, res.json()).

function getAuthRaw() {
  return localStorage.getItem("basic_auth_raw") || "";
}

function setAuthRaw(raw) {
  localStorage.setItem("basic_auth_raw", raw);
}

function promptCreds() {
  const username = window.prompt("HTTP Basic username (hr / manager)");
  const password = window.prompt("HTTP Basic password (hrPass / managerPass)");
  if (!username || !password) return null;
  const raw = btoa(`${username}:${password}`);
  setAuthRaw(raw);
  return raw;
}

export async function apiFetch(url, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set("Accept", headers.get("Accept") || "application/json");

  let raw = getAuthRaw();
  if (raw) headers.set("Authorization", `Basic ${raw}`);

  let res = await fetch(url, { ...options, headers });

  // If unauthorized and no creds stored, prompt once and retry
  if ((res.status === 401 || res.status === 403) && !raw) {
    raw = promptCreds();
    if (raw) {
      const headers2 = new Headers(options.headers || {});
      headers2.set("Accept", headers2.get("Accept") || "application/json");
      headers2.set("Authorization", `Basic ${raw}`);
      res = await fetch(url, { ...options, headers: headers2 });
    }
  }

  return res;
}

export function clearAuth() {
  localStorage.removeItem("basic_auth_raw");
}
