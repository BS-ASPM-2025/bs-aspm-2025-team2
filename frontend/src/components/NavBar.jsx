import React, { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { apiFetch, clearAuth } from "../api";

export default function NavBar() {
  const loc = useLocation();
  const [role, setRole] = useState("UNKNOWN"); // MANAGER | HR | UNKNOWN

  useEffect(() => {
    let cancelled = false;

    async function detectRole() {
      try {
        const res = await apiFetch("/api/manager/reports/pipeline-stats", {
          method: "GET",
          headers: { Accept: "application/json" },
        });

        if (cancelled) return;

        if (res.ok) setRole("MANAGER");
        else setRole("HR");
      } catch {
        if (!cancelled) setRole("UNKNOWN");
      }
    }

    detectRole();
    return () => {
      cancelled = true;
    };
  }, []);

  const isActive = (p) => loc.pathname.startsWith(p);

  const btn = (active) => ({
    display: "inline-block",
    padding: "14px 18px",
    borderRadius: 14,
    border: "1px solid #ddd",
    fontWeight: 900,
    textDecoration: "none",
    color: active ? "white" : "black",
    background: active ? "#1d4ed8" : "transparent",
  });

  return (
    <div style={{ padding: 24, display: "flex", gap: 12, alignItems: "center" }}>
      <Link to="/candidates" style={btn(isActive("/candidates"))}>Candidate List</Link>
      <Link to="/upload" style={btn(isActive("/upload"))}>Upload CV</Link>

      {role === "MANAGER" ? (
        <>
          <Link to="/reports/1" style={btn(isActive("/reports/1"))}>Report #1</Link>
          <Link to="/reports/2" style={btn(isActive("/reports/2"))}>Report #2</Link>
          <Link to="/settings" style={btn(isActive("/settings"))}>Settings</Link>
        </>
      ) : null}

      <div style={{ marginLeft: "auto", display: "flex", gap: 10, alignItems: "center" }}>
        <div style={{ fontWeight: 900, color: "#334" }}>Role: {role}</div>
        <button
          onClick={() => {
            clearAuth();
            window.location.reload();
          }}
          style={{ padding: "10px 14px", fontWeight: 900 }}
          title="Clear saved Basic Auth"
        >
          Logout
        </button>
      </div>
    </div>
  );
}
