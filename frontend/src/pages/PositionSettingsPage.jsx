import React, { useEffect, useMemo, useState } from "react";
import NavBar from "../components/NavBar.jsx";

const emptyDraft = () => ({
  id: null,
  name: "",
  requiredSkills: "",
  skillsWeight: 50,
  experienceWeight: 50,
});

function parseBackendError(json) {
  if (!json) return "Request failed";
  const msg = json.message || "Request failed";
  const fields = json.fields && typeof json.fields === "object" ? json.fields : null;
  if (!fields) return msg;

  const lines = Object.entries(fields).map(([k, v]) => `${k}: ${v}`);
  return `${msg}\n${lines.join("\n")}`;
}

export default function PositionSettingsPage() {
  const [positions, setPositions] = useState([]);
  const [selectedId, setSelectedId] = useState(null);

  const [draft, setDraft] = useState(emptyDraft());
  const [loadingList, setLoadingList] = useState(false);
  const [saving, setSaving] = useState(false);

  const [err, setErr] = useState("");
  const [info, setInfo] = useState("");

  const selected = useMemo(
    () => positions.find((p) => p.id === selectedId) || null,
    [positions, selectedId]
  );

  // Load positions (Manager only)
  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoadingList(true);
      setErr("");
      setInfo("");

      try {
        const res = await fetch("/api/manager/positions", {
          method: "GET",
          headers: { Accept: "application/json" },
        });

        if (!res.ok) {
          const body = await res.json().catch(() => null);
          throw new Error(parseBackendError(body) || `HTTP ${res.status}`);
        }

        const data = await res.json();
        if (cancelled) return;

        setPositions(Array.isArray(data) ? data : []);
        // auto-select first
        if (Array.isArray(data) && data.length > 0) {
          setSelectedId(data[0].id);
          setDraft({
            id: data[0].id,
            name: data[0].name ?? "",
            requiredSkills: data[0].requiredSkills ?? "",
            skillsWeight: Number(data[0].skillsWeight ?? 0),
            experienceWeight: Number(data[0].experienceWeight ?? 0),
          });
        } else {
          setSelectedId(null);
          setDraft(emptyDraft());
        }
      } catch (e) {
        if (!cancelled) setErr(String(e.message || e));
      } finally {
        if (!cancelled) setLoadingList(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  // When selecting a position -> load into draft
  useEffect(() => {
    if (!selected) return;
    setErr("");
    setInfo("");
    setDraft({
      id: selected.id,
      name: selected.name ?? "",
      requiredSkills: selected.requiredSkills ?? "",
      skillsWeight: Number(selected.skillsWeight ?? 0),
      experienceWeight: Number(selected.experienceWeight ?? 0),
    });
  }, [selectedId]); // eslint-disable-line react-hooks/exhaustive-deps

  function localValidate(d) {
    const sw = Number(d.skillsWeight);
    const ew = Number(d.experienceWeight);
    if (!Number.isFinite(sw) || !Number.isFinite(ew)) return "Weights must be numbers";
    if (sw < 0 || ew < 0) return "Weights must be non-negative";
    if (sw + ew !== 100) return "Weights must sum to 100";
    if (!String(d.name || "").trim()) return "Position name is required";
    return "";
  }

  async function onSave() {
    setErr("");
    setInfo("");

    const v = localValidate(draft);
    if (v) {
      setErr(v);
      return;
    }

    setSaving(true);
    try {
      const payload = {
        name: String(draft.name || "").trim(),
        requiredSkills: String(draft.requiredSkills || ""),
        skillsWeight: Number(draft.skillsWeight),
        experienceWeight: Number(draft.experienceWeight),
      };

      const isNew = !draft.id;
      const url = isNew ? "/api/manager/positions" : `/api/manager/positions/${draft.id}`;
      const method = isNew ? "POST" : "PUT";

      const res = await fetch(url, {
        method,
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(parseBackendError(body) || `HTTP ${res.status}`);
      }

      const saved = await res.json();
      // update list
      setPositions((prev) => {
        const arr = Array.isArray(prev) ? [...prev] : [];
        const idx = arr.findIndex((p) => p.id === saved.id);
        if (idx >= 0) arr[idx] = saved;
        else arr.unshift(saved);
        return arr;
      });
      setSelectedId(saved.id);
      setDraft({
        id: saved.id,
        name: saved.name ?? "",
        requiredSkills: saved.requiredSkills ?? "",
        skillsWeight: Number(saved.skillsWeight ?? 0),
        experienceWeight: Number(saved.experienceWeight ?? 0),
      });
      setInfo("Saved");
    } catch (e) {
      setErr(String(e.message || e));
    } finally {
      setSaving(false);
    }
  }

  function onCancel() {
    setErr("");
    setInfo("");
    if (!selected) {
      setDraft(emptyDraft());
      return;
    }
    setDraft({
      id: selected.id,
      name: selected.name ?? "",
      requiredSkills: selected.requiredSkills ?? "",
      skillsWeight: Number(selected.skillsWeight ?? 0),
      experienceWeight: Number(selected.experienceWeight ?? 0),
    });
  }

  function onNew() {
    setErr("");
    setInfo("");
    setSelectedId(null);
    setDraft(emptyDraft());
  }

  async function onDelete() {
    setErr("");
    setInfo("");

    if (!draft.id) {
      // deleting unsaved draft just resets
      setDraft(emptyDraft());
      return;
    }

    if (!confirm("Delete this position?")) return;

    try {
      const res = await fetch(`/api/manager/positions/${draft.id}`, { method: "DELETE" });
      if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(parseBackendError(body) || `HTTP ${res.status}`);
      }

      setPositions((prev) => prev.filter((p) => p.id !== draft.id));

      // pick next
      const remaining = positions.filter((p) => p.id !== draft.id);
      if (remaining.length > 0) {
        setSelectedId(remaining[0].id);
      } else {
        setSelectedId(null);
        setDraft(emptyDraft());
      }
      setInfo("Deleted");
    } catch (e) {
      setErr(String(e.message || e));
    }
  }

  return (
    <div className="container">

      <h1 style={{ marginTop: 24, marginBottom: 18 }}>Position settings</h1>

      {loadingList && <div style={{ marginBottom: 12 }}>Loading…</div>}

      {err && (
        <pre style={{ color: "crimson", whiteSpace: "pre-wrap", marginBottom: 12 }}>
          {err}
        </pre>
      )}
      {info && <div style={{ color: "green", marginBottom: 12 }}>{info}</div>}

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "280px 1fr",
          gap: 24,
          alignItems: "start",
        }}
      >
        {/* Left: list */}
        <div
          style={{
            border: "1px solid #e5e5e5",
            borderRadius: 12,
            padding: 16,
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
            <strong>Positions</strong>
            <button onClick={onNew}>+ New</button>
          </div>

          {positions.length === 0 ? (
            <div style={{ opacity: 0.7 }}>No positions yet</div>
          ) : (
            <div style={{ display: "grid", gap: 8 }}>
              {positions.map((p) => (
                <label
                  key={p.id}
                  style={{
                    display: "flex",
                    gap: 10,
                    alignItems: "center",
                    padding: "8px 10px",
                    borderRadius: 10,
                    cursor: "pointer",
                    background: p.id === selectedId ? "#f2f2f2" : "transparent",
                  }}
                >
                  <input
                    type="radio"
                    name="position"
                    checked={p.id === selectedId}
                    onChange={() => setSelectedId(p.id)}
                  />
                  <span style={{ overflow: "hidden", textOverflow: "ellipsis" }}>
                    {p.name || `Position #${p.id}`}
                  </span>
                </label>
              ))}
            </div>
          )}
        </div>

        {/* Right: editor */}
        <div
          style={{
            border: "1px solid #e5e5e5",
            borderRadius: 12,
            padding: 16,
          }}
        >
          <div style={{ fontSize: 18, fontWeight: 700, marginBottom: 14 }}>
            {draft.id ? `Position #${draft.id}` : "New position"}
          </div>

          <div style={{ display: "grid", gap: 12, maxWidth: 700 }}>
            <label style={{ display: "grid", gap: 6 }}>
              <span>Position name</span>
              <input
                value={draft.name}
                onChange={(e) => setDraft((d) => ({ ...d, name: e.target.value }))}
                placeholder="e.g., Backend Intern"
              />
            </label>

            <label style={{ display: "grid", gap: 6 }}>
              <span>Required skills (one per line)</span>
              <textarea
                rows={7}
                value={draft.requiredSkills}
                onChange={(e) => setDraft((d) => ({ ...d, requiredSkills: e.target.value }))}
                placeholder={"java\nspring\npostgres"}
              />
            </label>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
              <label style={{ display: "grid", gap: 6 }}>
                <span>skills_weight</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={draft.skillsWeight}
                  onChange={(e) => setDraft((d) => ({ ...d, skillsWeight: e.target.value }))}
                />
              </label>

              <label style={{ display: "grid", gap: 6 }}>
                <span>experience_weight</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={draft.experienceWeight}
                  onChange={(e) =>
                    setDraft((d) => ({ ...d, experienceWeight: e.target.value }))
                  }
                />
              </label>
            </div>

            <div style={{ opacity: 0.8 }}>
              Sum must be 100 (now:{" "}
              {Number(draft.skillsWeight || 0) + Number(draft.experienceWeight || 0)})
            </div>

            <div style={{ display: "flex", gap: 12, marginTop: 8 }}>
              <button onClick={onCancel} disabled={saving}>
                Cancel
              </button>
              <button onClick={onDelete} disabled={saving}>
                Delete
              </button>
              <button onClick={onSave} disabled={saving} style={{ marginLeft: "auto" }}>
                {saving ? "Saving…" : "Save"}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
