import React from "react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import NavBar from "../components/NavBar.jsx";


function Input({
  label,
  required,
  highlight,
  value,
  onChange,
  type = "text",
  placeholder,
}) {
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
          borderRadius: 4,
          border: "1px solid",
          borderColor: highlight ? "crimson" : "#ccc",
          background: highlight ? "#ffecec" : "white",
        }}
      />
    </div>
  );
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

  // Load candidate
  useEffect(() => {
    let cancelled = false;

    async function load() {
      setError(null);
      setSavedMsg(null);

      try {
        const res = await fetch(`/api/hr/candidates/${id}`);
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
          full_name: f.full_name ?? "",
          email: f.email ?? "",
          phone: f.phone ?? "",
          skills: f.skills ?? "",
          years_of_experience:
            f.years_of_experience === null || f.years_of_experience === undefined
              ? ""
              : String(f.years_of_experience),
        });

        setValidation(
          json.validation || {
            email_required_missing: false,
            phone_required_missing: false,
          }
        );
      } catch (e) {
        if (!cancelled) setError(e.message);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [id]);

  function updateField(name, value) {
    setSavedMsg(null);
    setError(null);

    setForm((prev) => ({ ...prev, [name]: value }));

    // Live required highlighting
    if (name === "email") {
      setValidation((prev) => ({
        ...prev,
        email_required_missing: value.trim() === "",
      }));
    }
    if (name === "phone") {
      setValidation((prev) => ({
        ...prev,
        phone_required_missing: value.trim() === "",
      }));
    }
  }

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
      const res = await fetch(`/api/hr/candidates/${id}`, {
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
        const msg =
          (json && (json.message || json.error)) ||
          raw ||
          `Save failed (${res.status})`;
        setError(`HTTP ${res.status}: ${msg}`);
        return;
      }

      // Success
      setSavedMsg("Saved");

      // Update local state from server response 
      if (json) {
        setLoaded(json);
        const f = json.fields || {};
        setForm({
          full_name: f.full_name ?? "",
          email: f.email ?? "",
          phone: f.phone ?? "",
          skills: f.skills ?? "",
          years_of_experience:
            f.years_of_experience === null || f.years_of_experience === undefined
              ? ""
              : String(f.years_of_experience),
        });
        setValidation(
          json.validation || {
            email_required_missing: false,
            phone_required_missing: false,
          }
        );
      }
    } catch (e) {
      setError(e?.message || "Network error");
    } finally {
      setSaving(false);
    }
  }

  if (!loaded && !error) return <div style={{ padding: 40 }}>Loading...</div>;
  if (!loaded && error) return <div style={{ padding: 40 }}>Error: {error}</div>;

  return (
    <div style={{ maxWidth: 700, margin: "40px auto", fontFamily: "Arial" }}>
      <h1>Candidate Card</h1>

      <p>
        <b>ID:</b> {loaded.candidate_id}
      </p>
      <p>
        <b>Status:</b> {loaded.status}
      </p>

      <div style={{ marginBottom: 12 }}>
  <strong>Status *</strong>
  <select
    value={form.status}
    onChange={(e) => updateField("status", e.target.value)}
    style={{
      width: "100%",
      padding: 10,
      marginTop: 6,
      borderRadius: 4,
      border: "1px solid #ccc",
      background: "white",
    }}
  >
    <option value="NEW">New</option>
    <option value="IN_REVIEW">In Review</option>
    <option value="REJECTED">Rejected</option>
    <option value="HIRED">Hired</option>
  </select>
</div>

      <Input
        label="Full name"
        value={form.full_name}
        onChange={(v) => updateField("full_name", v)}
        placeholder="e.g., John Doe"
      />

      <Input
        label="Email"
        required
        highlight={validation.email_required_missing}
        value={form.email}
        onChange={(v) => updateField("email", v)}
        type="email"
        placeholder="e.g., john.doe@email.com"
      />

      <Input
        label="Phone"
        required
        highlight={validation.phone_required_missing}
        value={form.phone}
        onChange={(v) => updateField("phone", v)}
        placeholder="e.g., +972501234567"
      />

      <Input
        label="Skills"
        value={form.skills}
        onChange={(v) => updateField("skills", v)}
        placeholder="e.g., Java, Spring, Docker"
      />

      <Input
        label="Years of experience"
        value={form.years_of_experience}
        onChange={(v) => updateField("years_of_experience", v)}
        placeholder="e.g., 5"
      />

      <div style={{ marginTop: 16 }}>
        <button onClick={onSave} disabled={saving} style={{ padding: "10px 16px" }}>
          {saving ? "Saving..." : "Save"}
        </button>
      </div>

      {error && <p style={{ color: "crimson", marginTop: 12 }}>{error}</p>}
      {savedMsg && <p style={{ color: "green", marginTop: 12 }}>{savedMsg}</p>}
    </div>
  );
}
