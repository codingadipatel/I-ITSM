import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { AuthPage } from "../pages/PublicPages";
import { ProtectedRoute } from "../components/ProtectedRoute";
import { IncidentForm, Incidents } from "../pages/Incidents";
import { Dashboard } from "../pages/Dashboard";
import { useAuth } from "../context/AuthContext";
import { useApi } from "../hooks/useApi";
import { api } from "../services/api";
vi.mock("../context/AuthContext", () => ({ useAuth: vi.fn() }));
vi.mock("../hooks/useApi", () => ({ useApi: vi.fn() }));
vi.mock("../services/api", () => ({
  api: { post: vi.fn(), put: vi.fn() },
  errorMessage: (e: Error) => e.message,
}));
vi.mock("recharts", () => ({
  ResponsiveContainer: ({ children }: any) => <div>{children}</div>,
  AreaChart: () => null,
  Area: () => null,
  CartesianGrid: () => null,
  XAxis: () => null,
  YAxis: () => null,
  Tooltip: () => null,
  BarChart: () => null,
  Bar: () => null,
}));
const auth = vi.mocked(useAuth);
const query = vi.mocked(useApi);
const employee = {
  id: 1,
  name: "Jordan Lee",
  email: "e@test.local",
  role: "EMPLOYEE",
  skills: [],
} as any;
function result(data: any) {
  return { data, error: "", loading: false, reload: vi.fn() };
}
function router(ui: React.ReactNode) {
  return render(<MemoryRouter>{ui}</MemoryRouter>);
}
describe("Authentication and route permissions", () => {
  it("submits credentials and displays backend errors", async () => {
    const login = vi
      .fn()
      .mockRejectedValue(new Error("Invalid email or password"));
    auth.mockReturnValue({ user: null, loading: false, login } as any);
    router(<AuthPage />);
    await userEvent.type(
      screen.getByLabelText("Email address"),
      "employee@test.local",
    );
    await userEvent.type(screen.getByLabelText("Password"), "wrong-password");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
    expect(login).toHaveBeenCalledWith("employee@test.local", "wrong-password");
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Invalid email or password",
    );
  });
  it("registration validates a short password before sending", async () => {
    const register = vi.fn();
    auth.mockReturnValue({ user: null, loading: false, register } as any);
    router(<AuthPage register />);
    await userEvent.type(screen.getByLabelText("Full name"), "Jordan");
    await userEvent.type(
      screen.getByLabelText("Email address"),
      "j@test.local",
    );
    await userEvent.type(screen.getByLabelText("Password"), "short");
    await userEvent.click(
      screen.getByRole("button", { name: "Create account" }),
    );
    expect(register).not.toHaveBeenCalled();
    expect(screen.getByLabelText("Password")).toBeInvalid();
  });
  it("employee cannot render an administrator route", () => {
    auth.mockReturnValue({ user: employee, loading: false } as any);
    render(
      <MemoryRouter initialEntries={["/admin"]}>
        <Routes>
          <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
            <Route path="admin" element={<div>Secret settings</div>} />
          </Route>
          <Route path="app" element={<div>Employee workspace</div>} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("Employee workspace")).toBeInTheDocument();
    expect(screen.queryByText("Secret settings")).not.toBeInTheDocument();
  });
  it("unauthenticated users return to sign in", () => {
    auth.mockReturnValue({ user: null, loading: false } as any);
    render(
      <MemoryRouter initialEntries={["/private"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="private" element={<div>Private</div>} />
          </Route>
          <Route path="login" element={<div>Sign-in required</div>} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("Sign-in required")).toBeInTheDocument();
  });
});
describe("Incident reporting and filtering", () => {
  const catalog = [
    {
      id: 1,
      name: "Payment Gateway",
      active: true,
      criticality: 3,
      requiredSkill: "Applications",
    },
  ];
  it("sends validated incident fields to the backend", async () => {
    query.mockReturnValue(result(catalog));
    const saved = vi.fn();
    vi.mocked(api.post).mockResolvedValue({ data: { id: 42 } });
    router(<IncidentForm onSaved={saved} />);
    await userEvent.type(
      screen.getByLabelText(/Incident title/),
      "Checkout is unavailable",
    );
    await userEvent.selectOptions(
      screen.getByLabelText(/Affected IT service/),
      "1",
    );
    await userEvent.selectOptions(screen.getByLabelText(/Category/), "1");
    await userEvent.type(
      screen.getByLabelText(/What went wrong/),
      "Payment checkout returns a connection timeout for all customers.",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Submit incident" }),
    );
    await waitFor(() => expect(saved).toHaveBeenCalled());
    expect(api.post).toHaveBeenCalledWith(
      "/incidents",
      expect.objectContaining({
        serviceId: 1,
        categoryId: 1,
        title: "Checkout is unavailable",
      }),
    );
  });
  it("does not submit an empty incident form", async () => {
    query.mockReturnValue(result(catalog));
    router(<IncidentForm />);
    await userEvent.click(
      screen.getByRole("button", { name: "Submit incident" }),
    );
    expect(api.post).not.toHaveBeenCalled();
    expect(screen.getByLabelText(/Incident title/)).toBeInvalid();
  });
  it("status and priority selections reach the query URL", async () => {
    auth.mockReturnValue({ user: employee } as any);
    query.mockImplementation((path: string | null) =>
      result(
        path?.startsWith("/incidents")
          ? { content: [], totalElements: 0, totalPages: 0 }
          : catalog,
      ),
    );
    router(<Incidents />);
    await userEvent.selectOptions(
      screen.getByLabelText("Filter by priority"),
      "P1",
    );
    await userEvent.selectOptions(
      screen.getByLabelText("Filter by status"),
      "IN_PROGRESS",
    );
    expect(query).toHaveBeenLastCalledWith(
      expect.stringContaining("priority=P1&status=IN_PROGRESS"),
    );
    expect(screen.getByText("No incidents found")).toBeInTheDocument();
  });
});
describe("Dashboard", () => {
  it("renders server metrics and the current user", () => {
    auth.mockReturnValue({ user: employee } as any);
    query.mockImplementation((path: string | null) =>
      result(
        path?.includes("/overview")
          ? {
              total: 12,
              open: 4,
              critical: 2,
              resolved: 8,
              closed: 5,
              slaViolations: 1,
              slaCompliance: 91.7,
              avgResponseMinutes: 7,
              avgResolutionMinutes: 40,
              byPriority: [{ name: "P1", value: 2 }],
            }
          : path?.startsWith("/incidents")
            ? { content: [] }
            : [],
      ),
    );
    router(<Dashboard />);
    expect(screen.getByRole("heading", { name: /Jordan/ })).toBeInTheDocument();
    expect(screen.getByText("91.7%")).toBeInTheDocument();
    expect(screen.getByText("40 min")).toBeInTheDocument();
  });
});
