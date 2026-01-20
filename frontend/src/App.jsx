import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import UploadPage from "./pages/UploadPage.jsx";
import CandidateCardPage from "./pages/CandidateCardPage.jsx";
import CandidateListPage from "./pages/CandidateListPage.jsx";
import PositionSettingsPage from "./pages/PositionSettingsPage.jsx";

import "./App.css";
import NavBar from "./components/NavBar.jsx";

export default function App() {
  return (
    <div className="container">
      <NavBar/>
      <Routes>
        <Route path="/" element={<Navigate to="/candidates" replace />} />
        <Route path="/upload" element={<UploadPage />} />
        <Route path="/candidates/:id" element={<CandidateCardPage />} />
        <Route path="/candidates" element={<CandidateListPage />} />
        <Route path="/settings" element={<PositionSettingsPage />} />
        <Route path="/reports/1" element={<div className="container"><h1>Report #1</h1><p>Coming soon</p></div>} />
        <Route path="/reports/2" element={<div className="container"><h1>Report #2</h1><p>Coming soon</p></div>} />
      </Routes>
    </div>
  );
}
