import React, { useEffect, useMemo, useState } from "react";
import { apiFetch } from "../api";

export default function PositionSettingsPage() {
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState(null);

  const [name, setName] = useState("");
  const [requiredSkills, setRequiredSkills] = useState("");
  const [skillsWeight, setSkillsWeight] = useState(50);
  const [experienceWeight, setExperienceWeight] = useState(50);

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  const sumOk = useMemo(() => Number(skillsWeight) + Number(experienceWeight) === 100, [skillsWeight, experienceWeight]);

  async function loadPositions() {
    setLoading(true);
    setErr("");
    try {
      // manager-only list is optional; fallback to public /api/positions
      let res = await apiFetch("/api/manager/positions", { headers: { Accept: "application/json" } });
      if (!res.ok) res = await apiFetch("/api/positions", { headers: { Accept: "application/json" } });

      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      const arr = Array.isArray(data) ? data : [];
      setItems(arr);
      if (arr.length && selectedId == null) setSelectedId(arr[0].id);
    } catch (e) {
      console.error(e);
      setItems([]);
      setErr("Failed to load positions (check Manager credentials / backend).");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPositions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const p = items.find((x) => x.id === selectedId);
    if (!p) return;
    setName(p.name ?? "");
    // backend field names: requiredSkills / required_skills
    setRequiredSkills((p.requiredSkills ?? p.required_skills ?? "").toString().replaceAll(",", "\n"));
    setSkillsWeight(Number(p.skillsWeight ?? p.skills_weight ?? 50));
    setExperienceWeight(Number(p.experienceWeight ?? p.experience_weight ?? 50));
  }, [selectedId, items]);

  function onNew() {
    setSelectedId(null);
    setName("");
    setRequiredSkills("");
    setSkillsWeight(50);
    setExperienceWeight(50);
  }

  async function onSave() {
    if (!sumOk) {
      alert("Weights sum must be 100");
      return;
    }
    setLoading(true);
    setErr("");
    try {
      const payload = {
        name,
        requiredSkills: requiredSkills
          .split("\n")
          .map((s) => s.trim())
          .filter(Boolean)
          .join(", "),
        skillsWeight: Number(skillsWeight),
        experienceWeight: Number(experienceWeight),
      };

      if (selectedId == null) {
        const res = await apiFetch("/api/manager/positions", {
          method: "POST",
          headers: { "Content-Type": "application/json", Accept: "application/json" },
          body: JSON.stringify(payload),
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
      } else {
        const res = await apiFetch(`/api/manager/positions/${selectedId}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json", Accept: "application/json" },
          body: JSON.stringify(payload),
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
      }

      await loadPositions();
    } catch (e) {
      console.error(e);
      setErr("Save failed (are you logged in as Manager?)");
    } finally {
      setLoading(false);
    }
  }

  async function onDelete() {
    if (selectedId == null) return;
    if (!confirm("Delete this position?")) return;

    setLoading(true);
    setErr("");
    try {
      const res = await apiFetch(`/api/manager/positions/${selectedId}`, { method: "DELETE" });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setSelectedId(null);
      await loadPositions();
    } catch (e) {
      console.error(e);
      setErr("Delete failed (are you logged in as Manager?)");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: 56, margin: "20px 0 20px" }}>Position settings</h1>

      {err ? <div style={{ color: "crimson", marginBottom: 12 }}>{err}</div> : null}
      {loading ? <div style={{ marginBottom: 12 }}>Loading…</div> : null}

      <div style={{ display: "grid", gridTemplateColumns: "320px 1fr", gap: 18, maxWidth: 980 }}>
        <div style={{ border: "1px solid #eee", borderRadius: 16, padding: 14 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
            <div style={{ fontWeight: 800 }}>Positions</div>
            <button onClick={onNew} style={{ padding: "8px 12px", fontWeight: 800 }}>+ New</button>
          </div>

          {items.length === 0 ? (
            <div style={{ color: "#667" }}>No positions yet</div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {items.map((p) => (
                <button
                  key={p.id}
                  onClick={() => setSelectedId(p.id)}
                  style={{
                    textAlign: "left",
                    padding: "10px 12px",
                    borderRadius: 12,
                    border: "1px solid #ddd",
                    fontWeight: 800,
                    background: selectedId === p.id ? "#1d4ed8" : "transparent",
                    color: selectedId === p.id ? "white" : "black",
                  }}
                >
                  {p.name}
                </button>
              ))}
            </div>
          )}
        </div>

        <div style={{ border: "1px solid #eee", borderRadius: 16, padding: 18 }}>
          <div style={{ fontWeight: 900, textAlign: "center", marginBottom: 12 }}>
            {selectedId == null ? "New position" : "Edit position"}
          </div>

          <label style={{ display: "block", fontWeight: 800, marginTop: 10 }}>Position name</label>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g., Backend Intern"
                 style={{ width: "100%", padding: 10 }} />

          <label style={{ display: "block", fontWeight: 800, marginTop: 14 }}>Required skills (one per line)</label>
          <textarea value={requiredSkills} onChange={(e) => setRequiredSkills(e.target.value)} rows={8}
                    style={{ width: "100%", padding: 10 }} />

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginTop: 14 }}>
            <div>
              <label style={{ display: "block", fontWeight: 800 }}>skills_weight</label>
              <input value={skillsWeight} onChange={(e) => setSkillsWeight(e.target.value)} style={{ width: "100%", padding: 10 }} />
            </div>
            <div>
              <label style={{ display: "block", fontWeight: 800 }}>experience_weight</label>
              <input value={experienceWeight} onChange={(e) => setExperienceWeight(e.target.value)} style={{ width: "100%", padding: 10 }} />
            </div>
          </div>

          <div style={{ marginTop: 10, color: sumOk ? "#1b5" : "crimson", fontWeight: 800 }}>
            Sum must be 100 (now: {Number(skillsWeight) + Number(experienceWeight)})
          </div>

          <div style={{ display: "flex", gap: 10, justifyContent: "space-between", marginTop: 18 }}>
            <button onClick={onNew} style={{ padding: "10px 16px", fontWeight: 900 }}>Cancel</button>
            <button onClick={onDelete} style={{ padding: "10px 16px", fontWeight: 900 }}>Delete</button>
            <button onClick={onSave} style={{ padding: "10px 16px", fontWeight: 900 }} disabled={!sumOk}>
              Save
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
