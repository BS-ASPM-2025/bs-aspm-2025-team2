import { useState } from "react";
import { useNavigate } from "react-router-dom";

function validateFile(file) {
  if (!file) return "Please select a file.";
  if (file.size > 10 * 1024 * 1024) return "File is too large. Max size is 10MB.";
  const isPdfByMime = file.type === "application/pdf";
  const isPdfByExt = file.name.toLowerCase().endsWith(".pdf");
  if (!isPdfByMime && !isPdfByExt) return "Only PDF files are accepted.";
  return null;
}

export default function UploadPage() {
  const [file, setFile] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function onUpload() {
    setError(null);

    const validation = validateFile(file);
    if (validation) {
      setError(validation);
      return;
    }

    setLoading(true);
    try {
      const fd = new FormData();
      fd.append("file", file);

      const res = await fetch("/api/hr/candidates/upload-resume", {
        method: "POST",
        body: fd,
      });

      const data = await res.json();

      if (!res.ok) {
        setError(data.message || "Upload failed");
        return;
      }

      navigate(`/candidates/${data.candidate_id}`);
    } catch (e) {
      setError("Network error");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ maxWidth: 600, margin: "40px auto", fontFamily: "Arial" }}>
      <h1>Upload CV</h1>

      <input
        type="file"
        accept="application/pdf"
        onChange={(e) => setFile(e.target.files[0])}
      />

      <div style={{ marginTop: 12 }}>
        <button onClick={onUpload} disabled={loading || !file}>
          {loading ? "Uploading..." : "Upload"}
        </button>
      </div>

      {error && <p style={{ color: "crimson", marginTop: 12 }}>{error}</p>}
    </div>
  );
}
