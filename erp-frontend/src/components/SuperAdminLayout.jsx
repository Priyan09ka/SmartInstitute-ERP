import { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";

function SuperAdminLayout({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const navigate = useNavigate();
  const location = useLocation();

  const isActive = (path) => location.pathname === path;

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Collapsible sidebar - same on all Super Admin pages */}
      <div
        className={`bg-indigo-800 text-white flex flex-col transition-all duration-200 ${
          sidebarOpen ? "w-64 p-5" : "w-16 p-2 items-center"
        }`}
      >
        <button
          type="button"
          onClick={() => setSidebarOpen((o) => !o)}
          className="flex items-center justify-center w-10 h-10 rounded hover:bg-indigo-600 mb-4 flex-shrink-0"
          aria-label={sidebarOpen ? "Collapse menu" : "Expand menu"}
        >
          <span className="flex flex-col gap-1.5">
            <span className="block w-6 h-0.5 bg-white rounded" />
            <span className="block w-6 h-0.5 bg-white rounded" />
            <span className="block w-6 h-0.5 bg-white rounded" />
          </span>
        </button>

        {sidebarOpen && (
          <>
            <h2 className="text-2xl font-bold mb-6">Super Admin</h2>
            <ul className="space-y-3">
              <li
                onClick={() => navigate("/super-admin")}
                className={`p-3 rounded cursor-pointer ${
                  isActive("/super-admin") ? "bg-indigo-600" : "hover:bg-indigo-600"
                }`}
              >
                Dashboard
              </li>
              <li
                onClick={() => navigate("/institutes")}
                className={`p-3 rounded cursor-pointer ${
                  isActive("/institutes") ? "bg-indigo-600" : "hover:bg-indigo-600"
                }`}
              >
                Institutes
              </li>
              <li
                onClick={() => navigate("/requests")}
                className={`p-3 rounded cursor-pointer ${
                  isActive("/requests") ? "bg-indigo-600" : "hover:bg-indigo-600"
                }`}
              >
                Requests
              </li>
              <li
                onClick={() => navigate("/profile")}
                className={`p-3 rounded cursor-pointer ${
                  isActive("/profile") ? "bg-indigo-600" : "hover:bg-indigo-600"
                }`}
              >
                Profile
              </li>
            </ul>
          </>
        )}
      </div>

      {/* Main content area */}
      <div className="flex-1 overflow-auto">
        {children}
      </div>
    </div>
  );
}

export default SuperAdminLayout;
