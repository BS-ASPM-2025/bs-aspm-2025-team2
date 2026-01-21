import React from "react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

function Input({ label, required, highlight, value, onChange, type = "text", placeholder }) {
  return (
    <div style={{ marginBottom: 12 }}>
      <strong>
        {label}
        {required ? " *" : ""}
      </strong>
      <input
        type={type}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        style={{
          width: "100%",
          padding: 10,
          marginTop: 6,
          border: highlight ? "2px solid crimson" : "1px solid #ccc",
          borderRadius: 8,
        }}
      />
    </div>
  );
}

function loadSavedPositionId() {
  try {
    const raw = localStorage.getItem("candidate_filters_v1");
    if (!raw) return "";
    const parsed = JSON.parse(raw);
    return parsed?.positionId ? String(parsed.positionId) : "";
  } catch {
    return "";
  }
}

export default function CandidateCardPage() {
  const { id } = useParams();

  const [loaded, setLoaded] = useState(null);
  const [form, setForm] = useState({
    status: "NEW",
    full_name: "",
    email: "",
    phone: "",
    skills: "",
    years_of_experience: "",
  });

  const [validation, setValidation] = useState({
    email_required_missing: false,
    phone_required_missing: false,
  });

  const [error, setError] = useState(null);
  const [savedMsg, setSavedMsg] = useState(null);
  const [saving, setSaving] = useState(false);

  // Position selector (for score calculation)
  const [positions, setPositions] = useState([]);
  const [positionsErr, setPositionsErr] = useState("");
  const [positionId, setPositionId] = useState(loadSavedPositionId());

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
      } catch {
        if (cancelled) return;
        setPositions([]);
        setPositionsErr("Failed to load positions");
      }
    }

    loadPositions();
    return () => {
      cancelled = true;
    };
  }, []);

  // load candidate (re-run on position change to update score)
  useEffect(() => {
    let cancelled = false;

    async function load() {
      setError(null);
      setSavedMsg(null);

      try {
        const params = new URLSearchParams();
        if (positionId) params.set("position_id", positionId);
        const qs = params.toString();
        const url = qs ? `/api/hr/candidates/${id}?${qs}` : `/api/hr/candidates/${id}`;

        const res = await apiFetch(url);
        const raw = await res.text();

        let json = null;
        try {
          json = raw ? JSON.parse(raw) : null;
        } catch {
          json = null;
        }

        if (!res.ok) {
          throw new Error((json && (json.message || json.error)) || raw || "Failed to load candidate");
        }

        if (cancelled) return;

        setLoaded(json);

        const f = json.fields || {};
        setForm({
          status: json.status ?? "NEW",
          full_name: f.fullName ?? "",
          email: f.email ?? "",
          phone: f.phone ?? "",
          skills: f.skills ?? "",
          years_of_experience:
            f.yearsOfExperience === null || f.yearsOfExperience === undefined
              ? ""
              : String(f.yearsOfExperience),
        });

        setValidation({
          email_required_missing: false,
          phone_required_missing: false,
        });
      } catch (e) {
        if (cancelled) return;
        setError(String(e?.message || e || "Failed to load candidate"));
      }
    }

    if (id) load();
    return () => {
      cancelled = true;
    };
  }, [id, positionId]);

  async function onSave() {
    setSavedMsg(null);
    setError(null);

    const email = form.email.trim();
    const phone = form.phone.trim();

    const emailEmpty = email === "";
    const phoneEmpty = phone === "";

    if (emailEmpty || phoneEmpty) {
      setValidation({
        email_required_missing: emailEmpty,
        phone_required_missing: phoneEmpty,
      });
      setError("Email and Phone are required.");
      return;
    }

    // Years of experience: must be null or a finite number
    const yoet = form.years_of_experience.trim();
    const yoe = yoet === "" ? null : Number(yoet);

    if (yoe !== null && !Number.isFinite(yoe)) {
      setError("Years of experience must be a number.");
      return;
    }

    const payload = {
      fullName: form.full_name.trim() || null,
      email,
      phone,
      skills: form.skills.trim() || null,
      yearsOfExperience: yoe,
      status: form.status,
    };

    setSaving(true);
    try {
      const params = new URLSearchParams();
      if (positionId) params.set("position_id", positionId);
      const qs = params.toString();
      const url = qs ? `/api/hr/candidates/${id}?${qs}` : `/api/hr/candidates/${id}`;

      const res = await apiFetch(url, {
        method: "PUT",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(payload),
      });

      const raw = await res.text();
      let json = null;
      try {
        json = raw ? JSON.parse(raw) : null;
      } catch {
        json = null;
      }

      if (!res.ok) {
        const msg = (json && (json.message || json.error)) || raw || `Save failed (${res.status})`;
        setError(`HTTP ${res.status}: ${msg}`);
        return;
      }

      setSavedMsg("Saved");

      // Update local state from server response
      if (json) {
        setLoaded(json);
        const f = json.fields || {};
        setForm({
          status: json.status ?? "NEW",
          full_name: f.fullName ?? "",
          email: f.email ?? "",
          phone: f.phone ?? "",
          skills: f.skills ?? "",
          years_of_experience:
            f.yearsOfExperience === null || f.yearsOfExperience === undefined
              ? ""
              : String(f.yearsOfExperience),
        });
      }

      setValidation({
        email_required_missing: false,
        phone_required_missing: false,
      });
    } catch (e) {
      setError(String(e?.message || e || "Network error"));
    } finally {
      setSaving(false);
    }
  }

  const score =
    loaded?.score === null || loaded?.score === undefined ? null : Number(loaded.score);

  return (
    <div>
      <div style={{ maxWidth: 860 }}>
        <h1 style={{ fontSize: 56, margin: "32px 0 8px" }}>Candidate Card</h1>

        <div style={{ display: "flex", gap: 16, alignItems: "center", marginBottom: 18 }}>
          <div style={{ padding: "10px 14px", border: "1px solid #ddd", borderRadius: 12 }}>
            <div style={{ fontSize: 12, color: "#667", marginBottom: 2 }}>Score</div>
            <div style={{ fontSize: 32, fontWeight: 800 }}>{score === null ? "—" : score}</div>
          </div>

          <div style={{ flex: 1 }}>
            <label style={{ display: "block", fontWeight: 700 }}>Position (for score)</label>
            <select
              value={positionId}
              onChange={(e) => setPositionId(e.target.value)}
              style={{ width: "100%", padding: 10, marginTop: 6 }}
            >
              <option value="">(not selected)</option>
              {positions.map((p) => (
                <option key={p.id} value={String(p.id)}>
                  {p.name}
                </option>
              ))}
            </select>
            {positionsErr ? (
              <div style={{ fontSize: 12, color: "crimson", marginTop: 4 }}>{positionsErr}</div>
            ) : null}
          </div>
        </div>

        {error ? <div style={{ color: "crimson", marginBottom: 12 }}>{error}</div> : null}
        {savedMsg ? <div style={{ color: "green", marginBottom: 12 }}>{savedMsg}</div> : null}

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
          <div>
            <strong>Status *</strong>
            <select
              value={form.status}
              onChange={(e) => setForm({ ...form, status: e.target.value })}
              style={{ width: "100%", padding: 10, marginTop: 6, borderRadius: 8 }}
            >
              <option value="NEW">NEW</option>
              <option value="IN_REVIEW">IN_REVIEW</option>
              <option value="REJECTED">REJECTED</option>
              <option value="HIRED">HIRED</option>
            </select>
          </div>

          <Input
            label="Full name"
            value={form.full_name}
            onChange={(v) => setForm({ ...form, full_name: v })}
            placeholder="Alice Smith"
          />
        </div>

        <div style={{ marginTop: 8 }}>
          <Input
            label="Email"
            required
            highlight={validation.email_required_missing}
            value={form.email}
            onChange={(v) => setForm({ ...form, email: v })}
            placeholder="alice@mail.com"
          />

          <Input
            label="Phone"
            required
            highlight={validation.phone_required_missing}
            value={form.phone}
            onChange={(v) => setForm({ ...form, phone: v })}
            placeholder="+1 555 123"
          />

          <Input
            label="Skills"
            value={form.skills}
            onChange={(v) => setForm({ ...form, skills: v })}
            placeholder="Java, Spring, SQL"
          />

          <Input
            label="Years of experience"
            value={form.years_of_experience}
            onChange={(v) => setForm({ ...form, years_of_experience: v })}
            placeholder="2"
          />

          <button onClick={onSave} disabled={saving} style={{ padding: "10px 16px", marginTop: 8 }}>
            {saving ? "Saving…" : "Save"}
          </button>
        </div>
      </div>
    </div>
  );
}
