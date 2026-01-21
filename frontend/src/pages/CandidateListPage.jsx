// frontend/src/pages/CandidateListPage.jsx
import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../api";

/**
 * Backend:
 *  GET /api/hr/candidates?q=&status=&min_years=&min_score=&position_id=&sort=upload_date_desc|score_desc|score_asc
 *  GET /api/positions   (can be broken; we keep UI but don't depend on it)
 */

const STATUS_OPTIONS = [
  { label: "All", value: "ALL" },
  { label: "New", value: "NEW" },
  { label: "In Review", value: "IN_REVIEW" },
  { label: "Rejected", value: "REJECTED" },
  { label: "Hired", value: "HIRED" },
];

const SORT_OPTIONS = [
  { label: "Upload date (newest)", value: "upload_date_desc" },
  { label: "Score (high → low)", value: "score_desc" },
  { label: "Score (low → high)", value: "score_asc" },
];

function safeText(v) {
  if (v === null || v === undefined) return "";
  return String(v);
}

function loadSavedFilters() {
  try {
    const raw = localStorage.getItem("candidate_filters_v1");
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object") return null;
    return parsed;
  } catch {
    return null;
  }
}

function saveFilters(next) {
  try {
    localStorage.setItem("candidate_filters_v1", JSON.stringify(next));
  } catch {
    // ignore
  }
}

export default function CandidateListPage() {
  const navigate = useNavigate();
  const saved = loadSavedFilters();

  // UI inputs
  const [q, setQ] = useState(saved?.q ?? "");
  const [statusUi, setStatusUi] = useState(saved?.status ?? "ALL");
  const [minYearsUi, setMinYearsUi] = useState(saved?.minYears ?? "");
  const [minScoreUi, setMinScoreUi] = useState(saved?.minScore ?? "");
  const [positionIdUi, setPositionIdUi] = useState(saved?.positionId ?? "");
  const [sortUi, setSortUi] = useState(saved?.sort ?? "upload_date_desc");

  // applied filters
  const [applied, setApplied] = useState({
    q: saved?.q ?? "",
    status: saved?.status ?? "ALL",
    minYears: saved?.minYears ?? "",
    minScore: saved?.minScore ?? "",
    positionId: saved?.positionId ?? "",
    sort: saved?.sort ?? "upload_date_desc",
  });

  // positions (non-blocking)
  const [positions, setPositions] = useState([]);
  const [positionsErr, setPositionsErr] = useState("");

  // data
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errText, setErrText] = useState("");

  // Load positions but NEVER block candidates
  useEffect(() => {
    let cancelled = false;

    async function loadPositions() {
      setPositionsErr("");
      try {
        const res = await apiFetch("/api/positions", { headers: { Accept: "application/json" } });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        if (cancelled) return;
        setPositions(Array.isArray(data) ? data : []);
      } catch (e) {
        if (cancelled) return;
        setPositions([]);
        setPositionsErr("Positions temporarily unavailable (backend). You can still load candidates.");
      }
    }

    loadPositions();
    return () => {
      cancelled = true;
    };
  }, []);

  // IMPORTANT: build query safely so candidates always load
  const queryString = useMemo(() => {
    const params = new URLSearchParams();

    // Always safe baseline
    params.set("limit", "50");
    params.set("offset", "0");
    params.set("sort", "upload_date_desc");

    const qTrim = safeText(applied.q).trim();
    if (qTrim) params.set("q", qTrim);

    if (applied.status && applied.status !== "ALL") params.set("status", applied.status);

    const minYearsTrim = safeText(applied.minYears).trim();
    if (minYearsTrim !== "") params.set("min_years", minYearsTrim);

    const positionTrim = safeText(applied.positionId).trim();

    // Only use score-related stuff if position is explicitly chosen
    if (positionTrim !== "") {
      params.set("position_id", positionTrim);

      const minScoreTrim = safeText(applied.minScore).trim();
      if (minScoreTrim !== "") params.set("min_score", minScoreTrim);

      const sort = applied.sort || "upload_date_desc";
      params.set("sort", sort);
    }

    return params.toString();
  }, [applied]);

  async function load(signal) {
    setLoading(true);
    setErrText("");

    try {
      const res = await apiFetch(`/api/hr/candidates?${queryString}`, {
        signal,
        method: "GET",
        headers: { Accept: "application/json" },
      });

      if (!res.ok) {
        const body = await res.text().catch(() => "");
        const msg = body?.trim()
          ? `HTTP ${res.status}: ${body.trim()}`
          : `HTTP ${res.status}: Failed to load candidates`;
        setItems([]);
        setErrText(msg);
        return;
      }

      const data = await res.json();
      const list = Array.isArray(data?.items) ? data.items : [];
      setItems(list);
    } catch (e) {
      if (e?.name === "AbortError") return;
      setItems([]);
      setErrText("Network error: failed to load candidates");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryString]);

  function onApply() {
    const next = {
      q,
      status: statusUi,
      minYears: minYearsUi,
      minScore: minScoreUi,
      positionId: positionIdUi,
      sort: sortUi,
    };
    setApplied(next);
    saveFilters(next);
  }

  function onReset() {
    const next = {
      q: "",
      status: "ALL",
      minYears: "",
      minScore: "",
      positionId: "",
      sort: "upload_date_desc",
    };
    setQ(next.q);
    setStatusUi(next.status);
    setMinYearsUi(next.minYears);
    setMinScoreUi(next.minScore);
    setPositionIdUi(next.positionId);
    setSortUi(next.sort);
    setApplied(next);
    saveFilters(next);
  }

  function openCandidate(id) {
    if (!id) return;
    navigate(`/candidates/${id}`);
  }

  const scoreEnabled = safeText(applied.positionId).trim() !== "";

  return (
    <div>
      <h1 style={{ fontSize: 56, margin: "32px 0 24px" }}>Candidate List</h1>

      {/* Filters */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "2fr 1fr 1fr 1fr 1fr 1fr",
          gap: 12,
          alignItems: "end",
          maxWidth: 980,
        }}
      >
        <div>
          <label style={{ display: "block", fontWeight: 700 }}>Search</label>
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="name / email / phone / skills"
            style={{ width: "100%", padding: 10 }}
          />
        </div>

        <div>
          <label style={{ display: "block", fontWeight: 700 }}>Status</label>
          <select
            value={statusUi}
            onChange={(e) => setStatusUi(e.target.value)}
            style={{ width: "100%", padding: 10 }}
          >
            {STATUS_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </div>

        <div>
          <label style={{ display: "block", fontWeight: 700 }}>Min years</label>
          <input
            value={minYearsUi}
            onChange={(e) => setMinYearsUi(e.target.value)}
            placeholder="e.g. 2"
            inputMode="numeric"
            style={{ width: "100%", padding: 10 }}
          />
        </div>

        <div>
          <label style={{ display: "block", fontWeight: 700 }}>Min score</label>
          <input
            value={minScoreUi}
            onChange={(e) => setMinScoreUi(e.target.value)}
            placeholder="0..100"
            inputMode="numeric"
            style={{ width: "100%", padding: 10 }}
            disabled={!safeText(positionIdUi).trim()}
            title={!safeText(positionIdUi).trim() ? "Select Position to enable score filters" : ""}
          />
        </div>

        <div>
          <label style={{ display: "block", fontWeight: 700 }}>Position</label>
          <select
            value={positionIdUi}
            onChange={(e) => setPositionIdUi(e.target.value)}
            style={{ width: "100%", padding: 10 }}
          >
            <option value="">(not selected)</option>
            {positions.map((p) => (
              <option key={p.id} value={String(p.id)}>{p.name}</option>
            ))}
          </select>

          {positionsErr ? (
            <div style={{ fontSize: 12, color: "crimson", marginTop: 4 }}>{positionsErr}</div>
          ) : (
            positions.length === 0 ? (
              <div style={{ fontSize: 12, color: "#667", marginTop: 4 }}>
                (Positions empty) — score features will stay off.
              </div>
            ) : null
          )}
        </div>

        <div>
          <label style={{ display: "block", fontWeight: 700 }}>Sort</label>
          <select
            value={sortUi}
            onChange={(e) => setSortUi(e.target.value)}
            style={{ width: "100%", padding: 10 }}
            disabled={!safeText(positionIdUi).trim()}
            title={!safeText(positionIdUi).trim() ? "Select Position to enable score sorting" : ""}
          >
            {SORT_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </div>

        <div style={{ gridColumn: "1 / -1", display: "flex", gap: 10, marginTop: 10 }}>
          <button onClick={onApply} style={{ padding: "10px 18px", fontWeight: 800 }}>
            Apply
          </button>
          <button onClick={onReset} style={{ padding: "10px 18px", fontWeight: 800 }}>
            Reset
          </button>

          {!scoreEnabled ? (
            <div style={{ marginLeft: 10, color: "#667", alignSelf: "center" }}>
              Score is shown only after selecting a Position.
            </div>
          ) : null}
        </div>
      </div>

      {/* Status */}
      <div style={{ marginTop: 16, minHeight: 24 }}>
        {loading ? <div>Loading…</div> : errText ? <div style={{ color: "crimson" }}>{errText}</div> : null}
      </div>

      {/* Table */}
      <div style={{ marginTop: 18, maxWidth: 980 }}>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "2fr 2fr 1.5fr 1fr 0.8fr 0.8fr",
            gap: 12,
            fontWeight: 700,
            borderBottom: "1px solid #ddd",
            padding: "10px 0",
          }}
        >
          <div>Full name</div>
          <div>Email</div>
          <div>Phone</div>
          <div>Status</div>
          <div>Years</div>
          <div>Score</div>
        </div>

        {items.length === 0 && !loading && !errText ? (
          <div style={{ padding: "18px 0", color: "#667" }}>No candidates found</div>
        ) : null}

        {items.map((c) => {
          const id = c?.candidate_id ?? c?.id;
          const fullName = safeText(c?.full_name ?? c?.fullName) || "—";
          const email = safeText(c?.email) || "—";
          const phone = safeText(c?.phone) || "—";
          const status = safeText(c?.status) || "—";
          const years =
            c?.years_of_experience === null || c?.years_of_experience === undefined
              ? "—"
              : String(c.years_of_experience);

          const score =
            !scoreEnabled
              ? ""
              : (c?.score === null || c?.score === undefined ? "—" : String(c.score));

          return (
            <div
              key={id ?? `${email}-${phone}-${Math.random()}`}
              onClick={() => openCandidate(id)}
              role="button"
              tabIndex={0}
              style={{
                display: "grid",
                gridTemplateColumns: "2fr 2fr 1.5fr 1fr 0.8fr 0.8fr",
                gap: 12,
                padding: "12px 0",
                borderBottom: "1px solid #f0f0f0",
                cursor: id ? "pointer" : "default",
              }}
            >
              <div>{fullName}</div>
              <div>{email}</div>
              <div>{phone}</div>
              <div>{status}</div>
              <div>{years}</div>
              <div>{score}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
