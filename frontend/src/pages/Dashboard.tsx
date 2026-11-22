import { IncidentComparison } from "../components/IncidentComparison";
import { Link } from "react-router-dom";
import {
  Plus,
  ArrowUpRight,
  Ticket,
  Activity,
  ShieldCheck,
  Flame,
  Clock3,
  ArrowRight,
  CheckCircle2,
} from "lucide-react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  Cell,
} from "recharts";
import { useAuth } from "../context/AuthContext";
import { useApi } from "../hooks/useApi";
import type { Overview, Metric, Page, Incident } from "../types";
import { PageHeading, Loading, ErrorState, Empty } from "../components/UI";
import { IncidentTable } from "../components/IncidentTable";
export function Dashboard() {
  const { user } = useAuth();
  const overview = useApi<Overview>("/reports/overview");
  const trends = useApi<Metric[]>("/reports/incident-trends");
  const incidents = useApi<Page<Incident>>("/incidents?size=5");
  const categories = useApi<Metric[]>("/reports/category-analysis");
  const data = overview.data;
  if (overview.loading) return <Loading />;
  if (overview.error || !data)
    return <ErrorState message={overview.error} retry={overview.reload} />;
  const role = user!.role;
  const hour = new Date().getHours();
  return (
    <>
      <PageHeading
        eyebrow="YOUR SERVICE, AT A GLANCE"
        title={`${hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening"}, ${user!.name.split(" ")[0]}`}
        description={
          role === "ADMIN"
            ? "Here’s what’s happening across your service desk today."
            : role === "TECHNICIAN"
              ? "Your queue, priorities, and next steps. All in one place."
              : "Your IT support, without the uncertainty. Let’s get things moving."
        }
        action={
          <Link className="btn primary" to="/app/incidents/new">
            <Plus size={17} />
            Report incident
          </Link>
        }
      />
      <div className="overview-banner">
        <div className="banner-mark">
          <Activity size={24} />
        </div>
        <div>
          <strong>Clarity for every incident.</strong>
          <p>
            {data.open
              ? `${data.open} open incidents in your workspace. Keep the important work moving.`
              : "Your workspace is clear. Report an issue whenever you need a hand."}
          </p>
        </div>
        <Link to="/app/incidents">
          View your queue <ArrowRight size={16} />
        </Link>
        <div className="banner-orbit" />
      </div>
      <div className="stats-grid">
        {[
          {
            name: "Total incidents",
            value: data.total,
            icon: Ticket,
            color: "blue",
            detail: "All reported incidents",
          },
          {
            name: "Open incidents",
            value: data.open,
            icon: Activity,
            color: "purple",
            detail: "Awaiting a resolution",
          },
          {
            name: "Critical incidents",
            value: data.critical,
            icon: Flame,
            color: "orange",
            detail: "Priority one · needs attention",
          },
          {
            name: "SLA compliance",
            value: `${data.slaCompliance}%`,
            icon: ShieldCheck,
            color: "green",
            detail: `${data.slaViolations} incidents with a breach`,
          },
        ].map(({ name, value, icon: Icon, color, detail }) => (
          <article className="stat-card" key={name}>
            <div className="stat-top">
              <span>{name}</span>
              <span className={`stat-icon ${color}`}>
                <Icon size={18} />
              </span>
            </div>
            <strong>{value}</strong>
            <small>{detail}</small>
          </article>
        ))}
      </div>
      <div className="dashboard-charts">
        <section className="panel trend-panel">
          <div className="panel-header">
            <div>
              <h2>Incident activity</h2>
              <p>A view of incoming incidents over time</p>
            </div>
            <span className="period-label">Last 14 days · UTC</span>
          </div>
          <div className="chart-legend">
            <i />
            Reported incidents
          </div>
          <div className="chart-area">
            {trends.loading ? (
              <Loading />
            ) : trends.error ? (
              <ErrorState message={trends.error} />
            ) : (
              <ResponsiveContainer width="100%" height={225}>
                <AreaChart
                  data={trends.data || []}
                  margin={{ top: 12, right: 20, bottom: 0, left: -24 }}
                >
                  <defs>
                    <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
                      <stop
                        offset="0%"
                        stopColor="#7f9ff4"
                        stopOpacity={0.25}
                      />
                      <stop
                        offset="95%"
                        stopColor="#7f9ff4"
                        stopOpacity={0.01}
                      />
                    </linearGradient>
                  </defs>
                  <CartesianGrid
                    vertical={false}
                    stroke="#2a354a"
                    strokeDasharray="4 4"
                  />
                  <XAxis
                    dataKey="name"
                    tickFormatter={(v) =>
                      new Date(v + "T00:00:00").toLocaleDateString(undefined, {
                        month: "short",
                        day: "numeric",
                      })
                    }
                    axisLine={false}
                    tickLine={false}
                    minTickGap={30}
                    tick={{ fontSize: 11, fill: "#92a2bc" }}
                    dy={10}
                  />
                  <YAxis
                    allowDecimals={false}
                    axisLine={false}
                    tickLine={false}
                    tick={{ fontSize: 11, fill: "#92a2bc" }}
                  />
                  <Tooltip
                    contentStyle={{
                      borderRadius: 10,
                      borderColor: "#35445e",
                      backgroundColor: "#1b2639",
                      color: "#e4ecfa",
                      fontSize: 12,
                    }}
                  />
                  <Area
                    name="Reported"
                    type="monotone"
                    dataKey="value"
                    stroke="#89a7ff"
                    strokeWidth={2.5}
                    fill="url(#trendFill)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </div>
        </section>
        <section className="panel priority-panel">
          <div className="panel-header">
            <div>
              <h2>Priority breakdown</h2>
              <p>Know what needs attention</p>
            </div>
            <span className="subtle-icon">
              <LayersIcon />
            </span>
          </div>
          {data.byPriority.length ? (
            <>
              <div className="priority-total">
                <strong>{data.total}</strong>
                <span>total incidents</span>
              </div>
              <div className="stacked-priority">
                {["P1", "P2", "P3", "P4"].map((p, n) => (
                  <div
                    key={p}
                    style={{
                      width: `${((data.byPriority.find((x) => x.name === p)?.value || 0) / Math.max(1, data.total)) * 100}%`,
                      background: ["#e16d71", "#edaa63", "#e5c96e", "#64b6a1"][
                        n
                      ],
                    }}
                  />
                ))}
              </div>
              <div className="priority-legend">
                {["Critical", "High", "Medium", "Low"].map((name, n) => (
                  <div key={name}>
                    <span className={`legend-dot p${n + 1}`} />
                    <span>
                      P{n + 1} · {name}
                    </span>
                    <strong>
                      {data.byPriority.find((x) => x.name === `P${n + 1}`)
                        ?.value || 0}
                    </strong>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <Empty
              title="No incidents yet"
              text="Priority distribution will appear here."
            />
          )}
        </section>
      </div>
      <section className="panel recent-panel">
        <div className="panel-header">
          <div>
            <h2>
              {role === "TECHNICIAN"
                ? "Your incident queue"
                : "Recent incidents"}{" "}
              <span className="count-pill">{data.total}</span>
            </h2>
            <p>The latest activity in your workspace</p>
          </div>
          <Link className="text-link" to="/app/incidents">
            View all incidents <ArrowUpRight size={15} />
          </Link>
        </div>
        {incidents.loading ? (
          <Loading />
        ) : incidents.error ? (
          <ErrorState message={incidents.error} />
        ) : (
          <IncidentTable incidents={incidents.data?.content || []} />
        )}
      </section>
      <div className="bottom-metrics">
        <div>
          <span className="stat-icon blue">
            <Clock3 size={18} />
          </span>
          <div>
            <span>Average response</span>
            <strong>{data.avgResponseMinutes} min</strong>
          </div>
        </div>
        <div>
          <span className="stat-icon green">
            <CheckCircle2 size={18} />
          </span>
          <div>
            <span>Average resolution</span>
            <strong>{data.avgResolutionMinutes} min</strong>
          </div>
        </div>
        <div>
          <span className="stat-icon purple">
            <ShieldCheck size={18} />
          </span>
          <div>
            <span>Verified & closed</span>
            <strong>{data.closed} incidents</strong>
          </div>
        </div>
        <div className="metric-note">
          Every metric comes from your
          <br />
          workspace’s live incident records.
        </div>
      </div>
    </>
  );
}
function LayersIcon() {
  return <Ticket size={18} />;
}
export function Reports() {
  const overview = useApi<Overview>("/reports/overview");
  const categories = useApi<Metric[]>("/reports/category-analysis");
  const services = useApi<Metric[]>("/reports/service-analysis");
  const trends = useApi<Metric[]>("/reports/incident-trends");
  if (overview.loading) return <Loading />;
  if (overview.error) return <ErrorState message={overview.error} />;
  return (
    <>
      <PageHeading
        eyebrow="SERVICE INTELLIGENCE"
        title="Reports & analytics"
        description="Understand demand, service health, and resolution performance."
      />
      <IncidentComparison />
      <div className="stats-grid">
        {[
          ["Resolved incidents", overview.data?.resolved],
          ["Closed incidents", overview.data?.closed],
          ["SLA violations", overview.data?.slaViolations],
          ["Average resolution", `${overview.data?.avgResolutionMinutes} min`],
        ].map(([name, value]) => (
          <article className="stat-card" key={name}>
            <span>{name}</span>
            <strong>{value}</strong>
          </article>
        ))}
      </div>
      <div className="report-grid">
        {[
          { name: "Incidents by category", query: categories },
          { name: "Incidents by service", query: services },
          { name: "Incident trends · last 14 days (UTC)", query: trends },
          { name: "Incidents by priority", query: overview, priority: true },
        ].map(({ name, query, priority }) => (
          <section className="panel" key={name}>
            <div className="panel-header">
              <h2>{name}</h2>
            </div>
            {query.loading ? (
              <Loading />
            ) : query.error ? (
              <ErrorState message={query.error} />
            ) : (
              <div className="report-chart">
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart
                    data={
                      priority
                        ? overview.data?.byPriority
                        : (query.data as Metric[]) || []
                    }
                    margin={{ top: 12, right: 24, bottom: 35, left: -12 }}
                  >
                    <CartesianGrid vertical={false} stroke="#2a354a" />
                    <XAxis
                      dataKey="name"
                      tick={{ fontSize: 10 }}
                      interval={0}
                      angle={-18}
                      textAnchor="end"
                    />
                    <YAxis
                      allowDecimals={false}
                      axisLine={false}
                      tickLine={false}
                    />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: "#1b2639",
                        borderColor: "#35445e",
                        borderRadius: 10,
                        color: "#e4ecfa",
                      }}
                      labelStyle={{ color: "#b9c9e6" }}
                    />
                    <Bar
                      dataKey="value"
                      name="Incidents"
                      fill="#83a1ef"
                      radius={[5, 5, 0, 0]}
                    />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </section>
        ))}
      </div>
    </>
  );
}
