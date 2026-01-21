import React from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import NavBar from "./components/NavBar";
import UploadPage from "./pages/UploadPage";
import CandidateListPage from "./pages/CandidateListPage";
import CandidateCardPage from "./pages/CandidateCardPage";
import ReportsPage from "./pages/ReportsPage";
import PositionSettingsPage from "./pages/PositionSettingsPage";

export default function App() {
  return (
    <BrowserRouter>
      <NavBar />
      <div style={{ padding: "0 24px 24px" }}>
        <Routes>
          <Route path="/" element={<Navigate to="/candidates" replace />} />
          <Route path="/upload" element={<UploadPage />} />
          <Route path="/candidates" element={<CandidateListPage />} />
          <Route path="/candidates/:id" element={<CandidateCardPage />} />

          {/* Separate routes for report 1 and report 2 */}
          <Route path="/reports/1" element={<ReportsPage initialTab="pipeline" />} />
          <Route path="/reports/2" element={<ReportsPage initialTab="top" />} />

          {/* Manager settings page */}
          <Route path="/settings" element={<PositionSettingsPage />} />

          <Route path="*" element={<div>Not found</div>} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}
