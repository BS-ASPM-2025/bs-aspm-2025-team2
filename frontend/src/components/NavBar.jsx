import React, { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";

export default function NavBar() {
  const [canSeeSettings, setCanSeeSettings] = useState(false);
  const [canUpload, setCanUpload] = useState(false);
  const [checkedRole, setCheckedRole] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function checkRole() {
      try {
        const res = await fetch("/api/positions", {
          method: "GET",
          headers: { Accept: "application/json" },
        });

        if (cancelled) return;

        if (res.ok) {
          // manager
          setCanSeeSettings(true);
          setCanUpload(false);
        } else if (res.status === 401 || res.status === 403) {
          // hr (API protected)
          setCanSeeSettings(false);
          setCanUpload(true);
        } else {
          // unexpected (500 etc)
          setCanSeeSettings(false);
          setCanUpload(true);
        }

        setCheckedRole(true);
      } catch {
        if (cancelled) return;
        // network error
        setCanSeeSettings(false);
        setCanUpload(true);
        setCheckedRole(true);
      }
    }

    checkRole();
    return () => {
      cancelled = true;
    };
  }, []);

  const linkStyle = ({ isActive }) => ({
    padding: "12px 18px",
    borderRadius: 14,
    border: "1px solid #d9d9d9",
    textDecoration: "none",
    fontWeight: 600,
    color: "#111",
    background: isActive ? "#0b66ff" : "#fff",
    ...(isActive ? { color: "#fff", borderColor: "#0b66ff" } : {}),
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    minWidth: 140,
  });

  const barStyle = {
    display: "inline-flex",
    gap: 12,
    padding: 10,
    borderRadius: 16,
    border: "1px solid #e5e5e5",
    background: "#fff",
  };

  return (
    <div style={{ marginTop: 18, marginBottom: 18 }}>
      <div style={barStyle}>
        {checkedRole && canUpload && (
          <NavLink to="/upload" style={linkStyle}>
            Upload CV
          </NavLink>
        )}

        {checkedRole && (
          <NavLink to="/candidates" style={linkStyle}>
            Candidate List
          </NavLink>
        )}

        {checkedRole && canSeeSettings && (
          <NavLink to="/reports/1" style={linkStyle}>
            Report #1
          </NavLink>
        )}

        {checkedRole && canSeeSettings && (
          <NavLink to="/reports/2" style={linkStyle}>
            Report #2
          </NavLink>
        )}

        {checkedRole && canSeeSettings && (
          <NavLink to="/settings" style={linkStyle}>
            Settings
          </NavLink>
        )}
      </div>
    </div>
  );
}
