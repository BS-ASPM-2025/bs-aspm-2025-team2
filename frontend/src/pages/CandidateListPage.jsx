import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

/**
 * Backend expects:
 *  GET /api/hr/candidates?q=&status=&min_years=&sort=upload_date_desc&limit=&offset=
 *
 * Response (example):
 * {
 *   "items": [
 *     {
 *       "candidate_id": 1,
 *       "full_name": null,
 *       "email": null,
 *       "phone": "1953...",
 *       "status": "NEW",
 *       "years_of_experience": null,
 *       "upload_date": "...",
 *       "score": 0
 *     }
 *   ],
 *   "limit": 20,
 *   "offset": 0,
 *   "total": 4
 * }
 */

const STATUS_OPTIONS = [
  { label: "All", value: "ALL" },
  { label: "New", value: "NEW" },
  { label: "In Review", value: "IN_REVIEW" },
  { label: "Rejected", value: "REJECTED" },
  { label: "Hired", value: "HIRED" },
];

function safeText(v) {
  if (v === null || v === undefined) return "";
  return String(v);
}

function toIntOrEmpty(v) {
  const s = String(v ?? "").trim();
  if (s === "") return "";
  const n = Number(s);
  if (!Number.isFinite(n)) return "";
  return Math.max(0, Math.floor(n));
}

export default function CandidateListPage() {
  const navigate = useNavigate();

  // UI inputs
  const [q, setQ] = useState("");
  const [statusUi, setStatusUi] = useState("ALL");
  const [minYearsUi, setMinYearsUi] = useState("");

  // applied filters (only these trigger load)
  const [applied, setApplied] = useState({
    q: "",
    status: "ALL",
    minYears: "",
  });

  // data state
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errText, setErrText] = useState("");

  const queryString = useMemo(() => {
    const params = new URLSearchParams();
    params.set("sort", "upload_date_desc");

    const qTrim = safeText(applied.q).trim();
    if (qTrim) params.set("q", qTrim);

    if (applied.status && applied.status !== "ALL") {
      params.set("status", applied.status); // must be NEW/IN_REVIEW/REJECTED/HIRED
    }

    const minYears = toIntOrEmpty(applied.minYears);
    // backend says min_years optional; sending 0 is allowed but often pointless
    if (minYears !== "") params.set("min_years", String(minYears));

    return params.toString();
  }, [applied]);

  async function load(signal) {
    setLoading(true);
    setErrText("");

    try {
      const res = await fetch(`/api/hr/candidates?${queryString}`, {
        method: "GET",
        signal,
        headers: {
          Accept: "application/json",
        },
      });

      if (!res.ok) {
        // try to show backend error body if any
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

  // initial load + reload on Apply/Reset
  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryString]);

  function onApply() {
    setApplied({
      q,
      status: statusUi,
      minYears: minYearsUi,
    });
  }

  function onReset() {
    setQ("");
    setStatusUi("ALL");
    setMinYearsUi("");
    setApplied({ q: "", status: "ALL", minYears: "" });
  }

  function openCandidate(id) {
    if (!id) return;
    navigate(`/candidates/${id}`);
  }

  return (
    <div>
      <h1 style={{ fontSize: 56, margin: "32px 0 24px" }}>Candidate List</h1>

      <div class="filters">
      {/* Filters */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 220px 240px auto auto",
          gap: 16,
          alignItems: "end",
          maxWidth: 980,
        }}
      >
        <div>
          <label style={{ display: "block", marginBottom: 8 }}>
            Search (name/email/phone)
          </label>
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="e.g. John / john@ / 055..."
            style={{ width: "100%", padding: 10 }}
          />
        </div>

        <div>
          <label style={{ display: "block", marginBottom: 8 }}>Status</label>
          <select
            value={statusUi}
            onChange={(e) => setStatusUi(e.target.value)}
            style={{ width: "100%", padding: 10 }}
          >
            {STATUS_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label style={{ display: "block", marginBottom: 8 }}>
            Min years of experience
          </label>
          <input
            value={minYearsUi}
            onChange={(e) => setMinYearsUi(e.target.value)}
            placeholder="(empty = any)"
            inputMode="numeric"
            style={{ width: "100%", padding: 10 }}
          />
        </div>

        <button onClick={onApply} style={{ padding: "10px 18px" }}>
          Apply
        </button>
        <button onClick={onReset} style={{ padding: "10px 18px" }}>
          Reset
        </button>
      </div>
      </div>

      {/* Status */}
      <div style={{ marginTop: 16, minHeight: 24 }}>
        {loading ? (
          <div>Loading…</div>
        ) : errText ? (
          <div style={{ color: "crimson" }}>{errText}</div>
        ) : null}
      </div>

      {/* Table */}
      <div style={{ marginTop: 18, maxWidth: 980 }}>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "2fr 2fr 1.5fr 1fr 1fr",
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
        </div>

        {items.length === 0 && !loading && !errText ? (
          <div style={{ padding: "18px 0", color: "#667" }}>
            No candidates found
          </div>
        ) : null}

        {items.map((c) => {
          const id = c?.candidate_id;
          const fullName = safeText(c?.full_name) || "—";
          const email = safeText(c?.email) || "—";
          const phone = safeText(c?.phone) || "—";
          const status = safeText(c?.status) || "—";
          const years =
            c?.years_of_experience === null || c?.years_of_experience === undefined
              ? "—"
              : String(c.years_of_experience);

          return (
            <div
              key={id ?? Math.random()}
              onClick={() => openCandidate(id)}
              role="button"
              tabIndex={0}
              style={{
                display: "grid",
                gridTemplateColumns: "2fr 2fr 1.5fr 1fr 1fr",
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
            </div>
          );
        })}
      </div>
    </div>
  );
}
