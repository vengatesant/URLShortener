import { NavLink, Route, Routes } from "react-router-dom";
import CreateLinkPage from "./pages/CreateLinkPage";
import MyLinksPage from "./pages/MyLinksPage";
import LinkAnalyticsPage from "./pages/LinkAnalyticsPage";

export default function App() {
  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar-inner">
          <span className="brand">
            <span className="brand-mark">/r/</span>URL Shortener
          </span>
          <nav className="nav">
            <NavLink to="/" end className={({ isActive }) => (isActive ? "active" : "")}>
              Create
            </NavLink>
            <NavLink to="/links" className={({ isActive }) => (isActive ? "active" : "")}>
              My Links
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<CreateLinkPage />} />
          <Route path="/links" element={<MyLinksPage />} />
          <Route path="/links/:shortCode/stats" element={<LinkAnalyticsPage />} />
        </Routes>
      </main>
    </div>
  );
}
