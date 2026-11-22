import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ToastProvider } from "./context/ToastContext";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AppLayout } from "./layouts/AppLayout";
import { Landing, AuthPage } from "./pages/PublicPages";
import { Dashboard, Reports } from "./pages/Dashboard";
import { Incidents, CreateIncident } from "./pages/Incidents";
import { IncidentDetail } from "./pages/IncidentDetail";
import {
  People,
  CatalogManagement,
  SlaPolicies,
  Automation,
  Workload,
  AuditTrail,
} from "./pages/Management";
import {
  Notifications,
  Profile,
  NotificationSettings,
} from "./pages/AccountPages";
import { Empty } from "./components/UI";
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/login" element={<AuthPage />} />
            <Route path="/register" element={<AuthPage register />} />
            <Route element={<ProtectedRoute />}>
              <Route path="/app" element={<AppLayout />}>
                <Route index element={<Dashboard />} />
                <Route path="incidents" element={<Incidents />} />
                <Route path="incidents/new" element={<CreateIncident />} />
                <Route path="incidents/:id" element={<IncidentDetail />} />
                <Route path="reports" element={<Reports />} />
                <Route path="notifications" element={<Notifications />} />
                <Route path="profile" element={<Profile />} />
                <Route
                  element={<ProtectedRoute roles={["TECHNICIAN", "ADMIN"]} />}
                >
                  <Route path="workload" element={<Workload />} />
                </Route>
                <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
                  <Route path="users" element={<People />} />
                  <Route
                    path="technicians"
                    element={<People techniciansOnly />}
                  />
                  <Route
                    path="services"
                    element={<CatalogManagement kind="services" />}
                  />
                  <Route
                    path="categories"
                    element={<CatalogManagement kind="categories" />}
                  />
                  <Route path="sla" element={<SlaPolicies />} />
                  <Route path="automation" element={<Automation />} />

                  <Route path="audit" element={<AuditTrail />} />
                  <Route
                    path="notification-settings"
                    element={<NotificationSettings />}
                  />
                </Route>
              </Route>
            </Route>
            <Route
              path="*"
              element={
                <Empty
                  title="Page not found"
                  text="This page isn’t part of the workspace."
                  action={
                    <Link className="btn primary" to="/app">
                      Back to workspace
                    </Link>
                  }
                />
              }
            />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
