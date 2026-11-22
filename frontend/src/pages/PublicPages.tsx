import { useState, type FormEvent } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import {
  Check,
  ArrowRight,
  ShieldCheck,
  Workflow,
  Clock3,
  ArrowUpRight,
  Layers,
  Activity,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { errorMessage } from "../services/api";
export function Landing() {
  const { user } = useAuth();
  return (
    <div className="public-page">
      <nav className="public-nav">
        <Link className="brand" to="/">
          <span className="brand-icon">
            <Check size={23} />
          </span>
          I-ITSM
        </Link>
        <div>
          <Link className="btn secondary" to={user ? "/app" : "/login"}>
            {user ? "Open workspace" : "Sign in"}
          </Link>
          <Link className="btn primary" to="/register">
            Get started <ArrowUpRight size={16} />
          </Link>
        </div>
      </nav>
      <section className="landing-hero">
        <div className="landing-copy">
          <div className="eyebrow">
            <span className="live-dot" /> INTELLIGENT SERVICE OPERATIONS
          </div>
          <h1>
            Less downtime.
            <br />
            More <em>momentum.</em>
          </h1>
          <p>
            Bring every incident, every technician, and every resolution into
            one clear workspace. Give your team the context to move forward.
          </p>
          <div className="hero-buttons">
            <Link
              to={user ? "/app" : "/register"}
              className="btn primary large"
            >
              Enter your workspace <ArrowRight size={18} />
            </Link>
            <Link to="/login" className="text-link">
              Already a member? Sign in
            </Link>
          </div>
          <div className="hero-proof">
            <ShieldCheck size={17} /> Role-based access <span>·</span>{" "}
            Explainable automation <span>·</span> Full audit history
          </div>
        </div>
        <div
          className="hero-visual"
          aria-label="Incident lifecycle illustration"
        >
          <div className="visual-top">
            <span className="brand-icon">
              <Check size={22} />
            </span>
            <div>
              One incident. Clear ownership.
              <small>From the first report to the final resolution</small>
            </div>
            <span className="status-dot" />
          </div>
          <div className="flow-card">
            <span className="flow-icon blue">
              <Layers size={22} />
            </span>
            <div>
              <strong>Report & classify</strong>
              <p>Context captured. Priority calculated.</p>
            </div>
            <span className="flow-number">01</span>
          </div>
          <div className="flow-connector" />
          <div className="flow-card">
            <span className="flow-icon purple">
              <Workflow size={22} />
            </span>
            <div>
              <strong>Match the right expertise</strong>
              <p>Skills, availability, and workload aligned.</p>
            </div>
            <span className="flow-number">02</span>
          </div>
          <div className="flow-connector" />
          <div className="flow-card">
            <span className="flow-icon green">
              <Check size={22} />
            </span>
            <div>
              <strong>Resolve with confidence</strong>
              <p>Verified by the person who reported it.</p>
            </div>
            <span className="flow-number">03</span>
          </div>
          <div className="visual-foot">
            <Activity size={16} />
            <span>Every step connected. Every change recorded.</span>
          </div>
        </div>
      </section>
      <section className="features-strip">
        {[
          {
            icon: Workflow,
            title: "The right person, automatically",
            text: "Explainable skill and workload strategies route incidents with purpose.",
          },
          {
            icon: Clock3,
            title: "Stay ahead of the deadline",
            text: "Persistent SLA tracking and escalation keep time-sensitive work visible.",
          },
          {
            icon: ShieldCheck,
            title: "A record you can trust",
            text: "Permission checks, immutable audit history, and durable PostgreSQL storage.",
          },
        ].map(({ icon: Icon, title, text }) => (
          <article key={title}>
            <Icon size={24} />
            <h3>{title}</h3>
            <p>{text}</p>
          </article>
        ))}
      </section>
      <footer className="public-footer">
        I-ITSM{" "}
        <span>Intelligent IT Service Management & Incident Resolution</span>
        <Link to="/login">
          Open workspace <ArrowUpRight size={14} />
        </Link>
      </footer>
    </div>
  );
}
export function AuthPage({ register = false }: { register?: boolean }) {
  const { user, login, register: signUp } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  if (user) return <Navigate to="/app" replace />;
  async function submit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      if (register) await signUp(name, email, password);
      else await login(email, password);
      navigate("/app");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="auth-page">
      <aside className="auth-story">
        <Link className="brand" to="/">
          <span className="brand-icon">
            <Check size={23} />
          </span>
          I-ITSM
        </Link>
        <div>
          <span className="eyebrow">ONE CONNECTED WORKSPACE</span>
          <h1>
            Great service
            <br />
            starts with clarity.
          </h1>
          <p>
            Give every incident a path forward.
            <br />
            And every person the support they need.
          </p>
          <div className="auth-quote">
            <span>
              <Workflow size={23} />
            </span>
            <div>
              Built around your team
              <small>Report. Investigate. Resolve. Improve.</small>
            </div>
          </div>
        </div>
        <small>Intelligent IT Service Management</small>
      </aside>
      <main className="auth-form-wrap">
        <form onSubmit={submit} className="auth-form">
          <div className="eyebrow">WELCOME TO YOUR WORKSPACE</div>
          <h1>{register ? "Create your account" : "Welcome back"}</h1>
          <p>
            {register
              ? "Report issues and stay connected with your IT team."
              : "Sign in to keep your service moving."}
          </p>
          {!register && (
            <div
              className="role-shortcuts"
              aria-label="Demo account email shortcuts"
            >
              {["employee", "technician", "admin"].map((r) => (
                <button
                  type="button"
                  key={r}
                  onClick={() => setEmail(`${r}@iitsm.local`)}
                >
                  {r === "admin"
                    ? "Administrator"
                    : r[0].toUpperCase() + r.slice(1)}
                </button>
              ))}
            </div>
          )}
          {register && (
            <label>
              Full name
              <input
                required
                maxLength={120}
                autoComplete="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Your full name"
              />
            </label>
          )}
          <label>
            Email address
            <input
              required
              type="email"
              maxLength={200}
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
            />
          </label>
          <label>
            Password
            <input
              required
              type="password"
              pattern={register ? ".{10,72}" : undefined}
              minLength={register ? 10 : 1}
              maxLength={72}
              autoComplete={register ? "new-password" : "current-password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={
                register ? "At least 10 characters" : "Enter your password"
              }
            />
          </label>
          {register && (
            <small className="muted">
              New accounts receive employee access. Administrators manage other
              roles.
            </small>
          )}
          {error && (
            <div role="alert" className="inline-error">
              {error}
            </div>
          )}
          <button className="btn primary large full" disabled={busy}>
            {busy ? "Please wait…" : register ? "Create account" : "Sign in"}
            <ArrowRight size={17} />
          </button>
          <p className="auth-switch">
            {register ? "Already have an account?" : "New to the workspace?"}{" "}
            <Link to={register ? "/login" : "/register"}>
              {register ? "Sign in" : "Create an account"}
            </Link>
          </p>
          <div className="auth-secure">
            <ShieldCheck size={16} /> Secure, role-based access
          </div>
        </form>
      </main>
    </div>
  );
}
