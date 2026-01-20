import React from "react";
import { Link, useLocation } from "react-router-dom";

export default function NavBar() {
  const { pathname } = useLocation();

  const btnStyle = (active) => ({
    padding: "10px 18px",
    borderRadius: 10,
    border: "1px solid #ddd",
    textDecoration: "none",
    color: "black",
    background: active ? "#f0f0f0" : "white",
    fontWeight: 600,
  });

  return (
    <div class="navbar">
    <div style={{ display: "flex", gap: 12, margin: "24px 0" }}>
      <Link to="/upload" style={btnStyle(pathname.startsWith("/upload"))}>
        Upload CV
      </Link>
      <Link to="/candidates" style={btnStyle(pathname.startsWith("/candidates"))}>
        Candidate List
      </Link>
    </div>
    </div>
  );
}
