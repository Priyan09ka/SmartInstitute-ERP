import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import ForgotPassword from "./pages/ForgotPassword";
import Profile from "./pages/Profile";
import InstituteRequestForm from "./pages/InstituteRequestForm";
import SuperAdminDashboard from "./pages/superAdmin/SuperAdminDashboard";
import InstituteAdminDashboard from "./pages/instituteAdmin/InstituteAdminDashboard";
import TeacherDashboard from "./pages/teacher/TeacherDashboard";
import StudentDashboard from "./pages/student/StudentDashboard";
import PrincipalDashboard from "./pages/principal/PrincipalDashboard";
import Institutes from "./pages/Institutes";
import InstituteRequests from "./pages/superAdmin/InstituteRequests";
import SuperAdminLayout from "./components/SuperAdminLayout";

import ProtectedRoute from "./components/ProtectedRoute";

/** Resolves legacy `/dashboard` links to the correct role home. */
function DashboardRedirect() {
  const token = localStorage.getItem("token");
  const role = localStorage.getItem("role");
  if (!token) return <Navigate to="/" replace />;
  switch (role) {
    case "SUPER_ADMIN":
      return <Navigate to="/super-admin" replace />;
    case "INSTITUTE_ADMIN":
      return <Navigate to="/institute-admin" replace />;
    case "PRINCIPAL":
      return <Navigate to="/principal" replace />;
    case "TEACHER":
      return <Navigate to="/teacher" replace />;
    case "STUDENT":
      return <Navigate to="/student" replace />;
    default:
      return <Navigate to="/" replace />;
  }
}

function App() {
  return (
    <BrowserRouter>

      <Routes>

        {/* Public */}
        <Route path="/" element={<Login />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/institute-request" element={<InstituteRequestForm />} />
        <Route path="/dashboard" element={<DashboardRedirect />} />
        <Route
          path="/profile"
          element={
            <ProtectedRoute
              allowedRoles={["SUPER_ADMIN", "INSTITUTE_ADMIN", "PRINCIPAL", "TEACHER", "STUDENT"]}
            >
              <Profile />
            </ProtectedRoute>
          }
        />

        {/* SUPER ADMIN - same sidebar on all pages */}
        <Route
          path="/super-admin"
          element={
            <ProtectedRoute roleRequired="SUPER_ADMIN">
              <SuperAdminLayout>
                <SuperAdminDashboard />
              </SuperAdminLayout>
            </ProtectedRoute>
          }
        />

        <Route
          path="/institutes"
          element={
            <ProtectedRoute roleRequired="SUPER_ADMIN">
              <SuperAdminLayout>
                <Institutes />
              </SuperAdminLayout>
            </ProtectedRoute>
          }
        />

        <Route
          path="/requests"
          element={
            <ProtectedRoute roleRequired="SUPER_ADMIN">
              <SuperAdminLayout>
                <InstituteRequests />
              </SuperAdminLayout>
            </ProtectedRoute>
          }
        />

        <Route
          path="/platform-users"
          element={
            <ProtectedRoute roleRequired="SUPER_ADMIN">
              <SuperAdminLayout>
                <Institutes />
              </SuperAdminLayout>
            </ProtectedRoute>
          }
        />

        {/* INSTITUTE ADMIN */}
        <Route
          path="/institute-admin"
          element={
            <ProtectedRoute roleRequired="INSTITUTE_ADMIN">
              <InstituteAdminDashboard />
            </ProtectedRoute>
          }
        />

        {/* TEACHER */}
        <Route
          path="/teacher"
          element={
            <ProtectedRoute roleRequired="TEACHER">
              <TeacherDashboard />
            </ProtectedRoute>
          }
        />

        {/* STUDENT */}
        <Route
          path="/student"
          element={
            <ProtectedRoute roleRequired="STUDENT">
              <StudentDashboard />
            </ProtectedRoute>
          }
        />

        {/* PRINCIPAL */}
        <Route
          path="/principal"
          element={
            <ProtectedRoute roleRequired="PRINCIPAL">
              <PrincipalDashboard />
            </ProtectedRoute>
          }
        />


      </Routes>

    </BrowserRouter>
  );
}

export default App;