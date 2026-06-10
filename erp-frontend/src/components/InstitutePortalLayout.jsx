import { useState } from "react";
import { useNavigate } from "react-router-dom";

/**
 * @param {{
 *   portalTitle: string;
 *   portalTagline?: string;
 *   children: import("react").ReactNode;
 *   sidebarNav?: { id: string; label: string }[];
 *   activeNavId?: string;
 *   onNavClick?: (id: string) => void;
 * }} props
 */
function InstitutePortalLayout({
  portalTitle,
  portalTagline = "Institute portal",
  children,
  sidebarNav,
  activeNavId,
  onNavClick,
}) {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const navigate = useNavigate();

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    navigate("/");
  };

  const hasSidebarNav = Array.isArray(sidebarNav) && sidebarNav.length > 0 && typeof onNavClick === "function";

  return (
    <div className="flex h-screen bg-slate-100">
      <aside
        className={`bg-slate-900 text-white flex flex-col h-full min-h-0 transition-all duration-200 ${
          sidebarOpen ? "w-64 p-5" : "w-16 p-2 items-center"
        }`}
      >
        <button
          type="button"
          onClick={() => setSidebarOpen((o) => !o)}
          className="flex items-center justify-center w-10 h-10 rounded hover:bg-slate-700 mb-4 flex-shrink-0"
          aria-label={sidebarOpen ? "Collapse menu" : "Expand menu"}
        >
          <span className="flex flex-col gap-1.5">
            <span className="block w-6 h-0.5 bg-white rounded" />
            <span className="block w-6 h-0.5 bg-white rounded" />
            <span className="block w-6 h-0.5 bg-white rounded" />
          </span>
        </button>

        {sidebarOpen && (
          <div className="flex flex-col flex-1 min-h-0 w-full">
            <h2 className="text-xl font-bold mb-1 shrink-0">{portalTitle}</h2>
            <p className="text-slate-400 text-xs mb-4 shrink-0 leading-snug">{portalTagline}</p>

            {hasSidebarNav && (
              <nav
                className="w-full space-y-1 flex-1 min-h-0 overflow-y-auto pr-1 -mr-1"
                aria-label="Primary sections"
              >
                {sidebarNav.map((item) => {
                  const active = activeNavId === item.id;
                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => onNavClick(item.id)}
                      className={`w-full text-left px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                        active ? "bg-indigo-600 text-white" : "text-slate-200 hover:bg-slate-800"
                      }`}
                    >
                      {item.label}
                    </button>
                  );
                })}
              </nav>
            )}

            <button
              type="button"
              onClick={() => navigate("/profile")}
              className="mt-3 mb-2 shrink-0 w-full text-left p-3 rounded bg-slate-800 hover:bg-slate-700 text-sm"
            >
              Profile
            </button>

            <button
              type="button"
              onClick={logout}
              className="mt-auto shrink-0 w-full text-left p-3 rounded bg-slate-800 hover:bg-slate-700 text-sm"
            >
              Log out
            </button>
          </div>
        )}
        {!sidebarOpen && (
          <div className="flex flex-col flex-1 min-h-0 w-full">
            <div className="flex flex-col flex-1 min-h-0 gap-1 overflow-y-auto items-center">
              {hasSidebarNav &&
                sidebarNav.map((item) => {
                  const active = activeNavId === item.id;
                  const abbr =
                    item.label.length <= 3
                      ? item.label.toUpperCase()
                      : item.label
                          .split(/[\s↔\-–]+/)
                          .filter((w) => w && /[A-Za-z0-9]/.test(w))
                          .map((w) => w[0])
                          .join("")
                          .slice(0, 3)
                          .toUpperCase() || item.id.slice(0, 3).toUpperCase();
                  return (
                    <button
                      key={item.id}
                      type="button"
                      title={item.label}
                      onClick={() => onNavClick(item.id)}
                      className={`w-10 h-10 shrink-0 rounded text-[10px] leading-tight font-semibold flex items-center justify-center px-0.5 ${
                        active ? "bg-indigo-600 text-white" : "bg-slate-800 text-slate-200 hover:bg-slate-700"
                      }`}
                    >
                      {abbr}
                    </button>
                  );
                })}
            </div>
            <button
              type="button"
              onClick={() => navigate("/profile")}
              className="text-[10px] text-slate-300 hover:text-white shrink-0 py-1 mx-auto"
              title="Profile"
            >
              PR
            </button>
            <button
              type="button"
              onClick={logout}
              className="text-xs text-slate-400 hover:text-white shrink-0 py-2 mx-auto"
              title="Log out"
            >
              ⎋
            </button>
          </div>
        )}
      </aside>

      <main className="flex-1 overflow-auto">{children}</main>
    </div>
  );
}

export default InstitutePortalLayout;
