import { useState, useEffect } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  Ticket,
  Users,
  UserRoundCog,
  Layers,
  Shapes,
  Clock3,
  Workflow,
  ChartNoAxesCombined,
  ScrollText,
  Bell,
  LogOut,
  ChevronDown,
  Plus,
  Menu,
  X,
  Check,
  ArrowUpRight,
  Activity,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useApi } from "../hooks/useApi";
import type { Notification } from "../types";
import { initials, label } from "../utils/format";
export function AppLayout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const { data: notifications, reload: reloadNotifications } =
    useApi<Notification[]>("/notifications");
  useEffect(() => {
    reloadNotifications();
    const timer = setInterval(reloadNotifications, 30000);
    return () => clearInterval(timer);
  }, [location.pathname, reloadNotifications]);
  if (!user) return null;
  const unread = notifications?.filter((n) => !n.read).length || 0;
  const main = [
    { to: "/app", name: "Overview", icon: LayoutDashboard, end: true },
    {
      to: "/app/incidents",
      name:
        user.role === "EMPLOYEE"
          ? "My incidents"
          : user.role === "TECHNICIAN"
            ? "Assigned incidents"
            : "All incidents",
      icon: Ticket,
    },
    { to: "/app/notifications", name: "Notifications", icon: Bell },
  ];
  if (user.role !== "EMPLOYEE")
    main.push({ to: "/app/workload", name: "Workload", icon: Activity });
  if (user.role !== "ADMIN") main.push({ to: "/app/reports", name: "Reports & analytics", icon: ChartNoAxesCombined });
  const management = [
    { to: "/app/users", name: "People", icon: Users },
    { to: "/app/technicians", name: "Technicians", icon: UserRoundCog },
    { to: "/app/services", name: "IT services", icon: Layers },
    { to: "/app/categories", name: "Categories", icon: Shapes },
  ];
  const operations = [
    { to: "/app/sla", name: "SLA policies", icon: Clock3 },
    { to: "/app/automation", name: "Automation", icon: Workflow },
    {
      to: "/app/reports",
      name: "Reports & analytics",
      icon: ChartNoAxesCombined,
    },
    { to: "/app/audit", name: "Audit trail", icon: ScrollText },
    {
      to: "/app/notification-settings",
      name: "Notification settings",
      icon: Bell,
    },
  ];
  const nav = (items: typeof main) =>
    items.map(({ to, name, icon: Icon, end }) => (
      <NavLink
        key={to}
        to={to}
        end={end}
        onClick={() => setOpen(false)}
        className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}
      >
        <Icon size={18} />
        <span>{name}</span>
        {name === "Notifications" && unread > 0 && (
          <b className="nav-count">{unread}</b>
        )}
      </NavLink>
    ));
  const current =
    [...main, ...management, ...operations].find(
      (x) => x.to === location.pathname,
    )?.name ||
    (location.pathname.includes("/incidents/new")
      ? "Report incident"
      : location.pathname.includes("/incidents/")
        ? "Incident workspace"
        : "Profile");
  return (
    <div className="app-shell">
      <aside className={`sidebar ${open ? "is-open" : ""}`}>
        <Link to="/app" className="brand">
          <span className="brand-icon">
            <Check size={23} strokeWidth={3} />
          </span>
          <div>
            I-ITSM<small>Service operations</small>
          </div>
        </Link>
        <div className="workspace-switch">
          <span className="workspace-logo">AC</span>
          <div>
            Acme workspace<small>{label(user.role)} workspace</small>
          </div>
        </div>
        <div className="nav-section-label">WORKSPACE</div>
        <nav>{nav(main)}</nav>
        {user.role === "ADMIN" && (
          <>
            <div className="nav-section-label">MANAGEMENT</div>
            <nav>{nav(management)}</nav>
            <div className="nav-section-label">OPERATIONS</div>
            <nav>{nav(operations)}</nav>
          </>
        )}
        <div className="sidebar-bottom">
          <div className="system-status">
            <i />
            Service workspace
          </div>
          <Link to="/app/profile" className="account">
            <span className="avatar">{initials(user.name)}</span>
            <div>
              <strong>{user.name}</strong>
              <small>{label(user.role)}</small>
            </div>
            <ArrowUpRight size={16} />
          </Link>
          <button className="signout" onClick={logout}>
            <LogOut size={15} /> Sign out
          </button>
        </div>
      </aside>
      {open && (
        <button
          className="sidebar-scrim"
          aria-label="Close navigation"
          onClick={() => setOpen(false)}
        />
      )}
      <div className="main-shell">
        <header className="topbar">
          <div className="breadcrumbs">
            <button
              className="icon-button mobile-menu"
              aria-label={open ? "Close navigation" : "Open navigation"}
              onClick={() => setOpen(!open)}
            >
              {open ? <X size={20} /> : <Menu size={20} />}
            </button>
            <span>Workspace</span>
            <span className="slash">/</span>
            <strong>{current}</strong>
          </div>
          <div className="topbar-actions">
            <span className="environment">
              <i />
              Live workspace
            </span>
            <Link
              className="icon-button notification-bell"
              to="/app/notifications"
              aria-label={`${unread} unread notifications`}
            >
              <Bell size={19} />
              {unread > 0 && <i />}
            </Link>
            <Link
              to="/app/profile"
              className="avatar small"
              aria-label="Your profile"
            >
              {initials(user.name)}
            </Link>
          </div>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
        <footer className="app-footer">
          <span>I-ITSM · Intelligent service management</span>
          <span>
            Built for better service. <span className="footer-dot">●</span>{" "}
            Connected to PostgreSQL
          </span>
        </footer>
      </div>
    </div>
  );
}
