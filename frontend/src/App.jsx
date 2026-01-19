import { Routes, Route, Navigate } from "react-router-dom";
import UploadPage from "./pages/UploadPage.jsx";
import CandidateCardPage from "./pages/CandidateCardPage.jsx";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/upload" replace />} />
      <Route path="/upload" element={<UploadPage />} />
      <Route path="/candidates/:id" element={<CandidateCardPage />} />
    </Routes>
  );
}
