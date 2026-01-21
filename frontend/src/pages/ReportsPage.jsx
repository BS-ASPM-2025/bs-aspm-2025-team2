// frontend/src/pages/ReportsPage.jsx
import React, { useEffect, useMemo, useState } from "react";
import { apiFetch } from "../api";

const DEFAULT_FILTERS = {
  q: "",
  status: "ALL",
  minYears: "",
  minScore: "",
  positionId: "",
};

function loadSavedFilters() {
  try {
    const raw = localStorage.getItem("candidate_filters_v1");
    if (!raw) return DEFAULT_FILTERS;
    const x = JSON.parse(raw);
    return {
      q: x.q ?? "",
      status: x.status ?? "ALL",
      minYears: x.minYears ?? "",
      minScore: x.minScore ?? "",
      positionId: x.positionId ?? "",
    };
  } catch {
    return DEFAULT_FILTERS;
  }
}

export default function ReportsPage({ initialTab = "pipeline" }) {
  const [tab, setTab] = useState(initialTab);
  useEffect(() => setTab(initialTab), [initialTab]);

  const [filters, setFilters] = useState(loadSavedFilters());

  const canUseScore = useMemo(() => String(filters.positionId || "").trim() !== "", [filters.positionId]);

  const [pipeline, setPipeline] = useState(null);
  const [top, setTop] = useState(null);

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  function buildCommonParams() {
    const params = new URLSearchParams();
    if (filters.q?.trim()) params.set("q", filters.q.trim());
    if (filters.status && filters.status !== "ALL") params.set("status", filters.status);

    if (filters.minYears !== "" && filters.minYears != null) params.set("min_years", String(filters.minYears));

    // only if position selected
    if (String(filters.positionId || "").trim() !== "") {
      params.set("position_id", String(filters.positionId));
      if (filters.minScore !== "" && filters.minScore != null) params.set("min_score", String(filters.minScore));
    }

    return params;
  }

  async function loadPipeline() {
    setLoading(true);
    setErr("");
    try {
      const params = buildCommonParams();
      const data = await apiFetch(`/api/manager/reports/pipeline-stats?${params.toString()}`);
      setPipeline(data);
    } catch (e) {
      console.error(e);
      setPipeline(null);
      setErr("Failed to load pipeline stats");
    } finally {
      setLoading(false);
    }
  }

  async function loadTop(n = 10) {
    setLoading(true);
    setErr("");
    try {
      const params = buildCommonParams();
      params.set("n", String(n));
      const data = await apiFetch(`/api/manager/reports/top-candidates?${params.toString()}`);
      setTop(data);
    } catch (e) {
      console.error(e);
      setTop(null);
      setErr("Failed to load top candidates");
    } finally {
      setLoading(false);
    }
  }

  async function downloadCsv(n = 10) {
    try {
      const params = buildCommonParams();
      params.set("n", String(n));
      params.set("download", "csv");
      // CSV may return text/csv
      const raw = localStorage.getItem("basic_auth");
      const headers = new Headers();
      if (raw) headers.set("Authorization", `Basic ${raw}`);

      const res = await fetch(`/api/manager/reports/top-candidates?${params.toString()}`, { headers });
      if (!res.ok) throw new Error(`CSV HTTP ${res.status}`);

      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "top_candidates.csv";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      console.error(e);
      alert("CSV export failed");
    }
  }

  // load active tab
  useEffect(() => {
    if (tab === "pipeline") loadPipeline();
    else loadTop(10);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, filters.q, filters.status, filters.minYears, filters.minScore, filters.positionId]);

  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: 56, margin: "10px 0 20px" }}>Reports</h1>

      <div style={{ display: "flex", gap: 16, alignItems: "flex-end", flexWrap: "wrap" }}>
        <div>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>Search</div>
          <input
            value={filters.q}
            onChange={(e) => setFilters((p) => ({ ...p, q: e.target.value }))}
            placeholder="name / email / phone / skills"
            style={{ width: 260, height: 44, padding: "0 12px" }}
          />
        </div>

        <div>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>Status</div>
          <select
            value={filters.status}
            onChange={(e) => setFilters((p) => ({ ...p, status: e.target.value }))}
            style={{ width: 180, height: 44, padding: "0 12px" }}
          >
            <option value="ALL">All</option>
            <option value="NEW">NEW</option>
            <option value="IN_REVIEW">IN_REVIEW</option>
            <option value="REJECTED">REJECTED</option>
            <option value="HIRED">HIRED</option>
          </select>
        </div>

        <div>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>Min years</div>
          <input
            value={filters.minYears}
            onChange={(e) => setFilters((p) => ({ ...p, minYears: e.target.value }))}
            placeholder="e.g. 2"
            style={{ width: 120, height: 44, padding: "0 12px" }}
          />
        </div>

        <div>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>Min score</div>
          <input
            value={filters.minScore}
            onChange={(e) => setFilters((p) => ({ ...p, minScore: e.target.value }))}
            placeholder="0..100"
            style={{ width: 120, height: 44, padding: "0 12px" }}
            disabled={!canUseScore}
            title={!canUseScore ? "Score filter works only when position_id is provided by backend" : ""}
          />
        </div>

        <div>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>Position id</div>
          <input
            value={filters.positionId}
            onChange={(e) => setFilters((p) => ({ ...p, positionId: e.target.value }))}
            placeholder="(optional) e.g. 1"
            style={{ width: 160, height: 44, padding: "0 12px" }}
          />
          <div style={{ fontSize: 12, color: "#666", marginTop: 6 }}>
            (Temporary) If positions endpoint is broken, type position id manually.
          </div>
        </div>
      </div>

      <div style={{ display: "flex", gap: 12, marginTop: 18 }}>
        <button
          onClick={() => setTab("pipeline")}
          style={{
            height: 44,
            padding: "0 18px",
            fontWeight: 800,
            background: tab === "pipeline" ? "#1d4ed8" : "transparent",
            color: tab === "pipeline" ? "white" : "black",
            borderRadius: 10,
            border: "1px solid #ddd",
          }}
        >
          Pipeline stats
        </button>

        <button
          onClick={() => setTab("top")}
          style={{
            height: 44,
            padding: "0 18px",
            fontWeight: 800,
            background: tab === "top" ? "#1d4ed8" : "transparent",
            color: tab === "top" ? "white" : "black",
            borderRadius: 10,
            border: "1px solid #ddd",
          }}
        >
          Top candidates + CSV
        </button>
      </div>

      {err ? <div style={{ color: "crimson", marginTop: 14, fontSize: 18 }}>{err}</div> : null}
      {loading ? <div style={{ marginTop: 14 }}>Loading…</div> : null}

      {tab === "pipeline" ? (
        <div style={{ marginTop: 18 }}>
          <h2 style={{ fontSize: 24, marginBottom: 10 }}>Pipeline stats</h2>
          <pre style={{ background: "#f6f6f6", padding: 12, borderRadius: 12 }}>
            {pipeline ? JSON.stringify(pipeline, null, 2) : "No data"}
          </pre>
        </div>
      ) : (
        <div style={{ marginTop: 18 }}>
          <h2 style={{ fontSize: 24, marginBottom: 10 }}>Top candidates</h2>
          <div style={{ display: "flex", gap: 10, alignItems: "center", marginBottom: 10 }}>
            <button onClick={() => loadTop(10)} style={{ height: 40, padding: "0 14px", fontWeight: 800 }}>
              Refresh Top 10
            </button>
            <button onClick={() => downloadCsv(10)} style={{ height: 40, padding: "0 14px", fontWeight: 800 }}>
              Download CSV
            </button>
          </div>

          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", borderBottom: "1px solid #ddd" }}>
                <th style={{ padding: "10px 8px" }}>Full name</th>
                <th style={{ padding: "10px 8px" }}>Email</th>
                <th style={{ padding: "10px 8px" }}>Phone</th>
                <th style={{ padding: "10px 8px" }}>Status</th>
                <th style={{ padding: "10px 8px" }}>Score</th>
              </tr>
            </thead>
            <tbody>
              {(top?.items || []).map((c) => (
                <tr key={c.candidate_id} style={{ borderBottom: "1px solid #eee" }}>
                  <td style={{ padding: "10px 8px" }}>{c.full_name}</td>
                  <td style={{ padding: "10px 8px" }}>{c.email}</td>
                  <td style={{ padding: "10px 8px" }}>{c.phone}</td>
                  <td style={{ padding: "10px 8px" }}>{c.status}</td>
                  <td style={{ padding: "10px 8px" }}>{c.score}</td>
                </tr>
              ))}
              {!loading && !err && (top?.items || []).length === 0 ? (
                <tr><td colSpan={5} style={{ padding: "14px 8px", color: "#666" }}>No data</td></tr>
              ) : null}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
