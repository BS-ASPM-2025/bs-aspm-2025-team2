import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import NavBar from "../components/NavBar.jsx";

export default function UploadPage() {
  const navigate = useNavigate();

  const [file, setFile] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setMessage("");
    setError("");

    if (!file) {
      setError("Please choose a file");
      return;
    }

    // client-side checks (MVP)
    if (file.type !== "application/pdf") {
      setError("Only PDF files are accepted");
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      setError("File is too large (max 10MB)");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    setLoading(true);
    try {
      const res = await fetch("/api/hr/candidates/upload-resume", {
        method: "POST",
        body: formData,
      });

      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        // backend usually returns { error, message }
        setError(data.message || `HTTP ${res.status}`);
        return;
      }

      const candidateId = data.candidate_id ?? data.candidateId;
      setMessage(data.message || "Uploaded successfully");

      if (candidateId) {
        navigate(`/candidates/${candidateId}`);
      }
    } catch (e2) {
      setError("Network error");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h1>Upload CV</h1>

      <form onSubmit={onSubmit}>
        <input
          type="file"
          accept="application/pdf"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
        />

        <div style={{ marginTop: 12 }}>
          <button type="submit" disabled={loading} style={{ padding: "8px 16px" }}>
            {loading ? "Uploading..." : "Upload"}
          </button>
        </div>
      </form>

      {message && <div style={{ marginTop: 12, color: "green" }}>{message}</div>}
      {error && <div style={{ marginTop: 12, color: "crimson" }}>{error}</div>}
    </div>
  );
}
