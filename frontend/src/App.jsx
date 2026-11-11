import { useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import toast, { Toaster } from "react-hot-toast";
import SearchRoomsPage from "./pages/SearchRoomsPage.jsx";
import CreateReservationPage from "./pages/CreateReservationPage.jsx";
import ApprovalsPage from "./pages/ApprovalsPage.jsx";
import ReportsPage from "./pages/ReportsPage.jsx";
import AdminPanelPage from "./pages/AdminPanelPage.jsx";
import {
  Activity,
  ArrowRight,
  BarChart3,
  Boxes,
  CalendarCheck,
  CheckCircle2,
  ClipboardCheck,
  Database,
  DoorOpen,
  LogOut,
  PlusCircle,
  Search,
  ShieldCheck,
  Sparkles,
  UserRound,
  Users,
} from "lucide-react";

import {
  getDashboardStats,
  getHealth,
  getCurrentUser,
  login,
  logout,
} from "./api.js";

function App() {
  const [activeUser, setActiveUser] = useState(null);
  const [authChecking, setAuthChecking] = useState(true);

  useEffect(() => {
    localStorage.removeItem("activeUser");
    getCurrentUser()
        .then(setActiveUser)
        .catch(() => setActiveUser(null))
        .finally(() => setAuthChecking(false));
  }, []);

  const [authView, setAuthView] = useState(() => {
    return localStorage.getItem("authView") || "landing";
  });

  const [activePage, setActivePage] = useState(() => {
    return localStorage.getItem("activePage") || "dashboard";
  });

  function changeAuthView(view) {
    localStorage.setItem("authView", view);
    setAuthView(view);
  }

  function handleLogin(user) {
    localStorage.setItem("activePage", "dashboard");
    localStorage.removeItem("authView");

    setActiveUser(user);
    setActivePage("dashboard");

    toast.success(`Logged in as ${user.fullName}`);
  }

  async function handleLogout() {
    try {
      await logout();
    } catch (error) {
      toast.error(error.message);
      return;
    }
    localStorage.removeItem("activePage");
    localStorage.setItem("authView", "landing");
    setActiveUser(null);
    setAuthView("landing");
    setActivePage("dashboard");
  }

  useEffect(() => {
    if (activeUser) {
      localStorage.setItem("activePage", activePage);
    }
  }, [activePage, activeUser]);

  if (authChecking) {
    return <div className="auth-page">Checking your session...</div>;
  }

  if (!activeUser) {
    return (
        <>
          <Toaster position="top-right" />
          <AnimatePresence mode="wait">
            {authView === "landing" && (
                <PageMotion key="landing">
                  <LandingScreen
                      onLoginClick={() => changeAuthView("login")}
                      onRegisterClick={() => changeAuthView("register")}
                  />
                </PageMotion>
            )}

            {authView === "login" && (
                <PageMotion key="login">
                  <LoginScreen
                      onLogin={handleLogin}
                      onBack={() => changeAuthView("landing")}
                      onRegisterClick={() => changeAuthView("register")}
                  />
                </PageMotion>
            )}

            {authView === "register" && (
                <PageMotion key="register">
                  <RegisterScreen
                      onBack={() => changeAuthView("landing")}
                      onLoginClick={() => changeAuthView("login")}
                  />
                </PageMotion>
            )}
          </AnimatePresence>
        </>
    );
  }

  return (
      <>
        <Toaster position="top-right" />
        <AppShell
            activeUser={activeUser}
            activePage={activePage}
            setActivePage={setActivePage}
            onLogout={handleLogout}
        >
          <AnimatePresence mode="wait">
            {activePage === "dashboard" && (
                <PageMotion key="dashboard">
                  <Dashboard activeUser={activeUser} setActivePage={setActivePage} />
                </PageMotion>
            )}

            {activePage === "search" && (
                <PageMotion key="search">
                  <SearchRoomsPage />
                </PageMotion>
            )}

            {activePage === "create" && (
                <PageMotion key="create">
                  <CreateReservationPage activeUser={activeUser} />
                </PageMotion>
            )}

            {activePage === "approvals" && (
                <PageMotion key="approvals">
                  <ApprovalsPage activeUser={activeUser} />
                </PageMotion>
            )}

            {activePage === "reports" && (
                <PageMotion key="reports">
                  <ReportsPage />
                </PageMotion>
            )}

            {activePage === "admin" && (
                <PageMotion key="admin">
                  <AdminPanelPage activeUser={activeUser} />
                </PageMotion>
            )}
          </AnimatePresence>
        </AppShell>
      </>
  );
}

function LandingScreen({ onLoginClick, onRegisterClick }) {
  return (
      <div className="landing-page">
        <div className="landing-orb orb-one" />
        <div className="landing-orb orb-two" />
        <div className="landing-orb orb-three" />

        <header className="landing-header">
          <div className="landing-brand">
            <img src="/logo.png" alt="Room Reservation logo" />
          </div>

          <div className="landing-header-actions">
            <button className="ghost-button" onClick={onLoginClick}>
              Login
            </button>
            <button className="landing-small-primary" onClick={onRegisterClick}>
              Register
            </button>
          </div>
        </header>

        <main className="landing-main">
          <motion.div
              className="landing-hero-content"
              initial={{ opacity: 0, y: 26 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.55 }}
          >
            <div className="landing-eyebrow">
              <Sparkles size={16} />
              Room Management Platform
            </div>

            <h1>Smart Room Reservation System</h1>

            <p>
              System for searching available rooms, creating reservation requests,
              approving reservations, and analyzing resource usage.
            </p>

            <div className="landing-actions">
              <button className="landing-primary" onClick={onLoginClick}>
                Login to system
                <ArrowRight size={20} />
              </button>

              <button className="landing-secondary" onClick={onRegisterClick}>
                Request access
                <UserRound size={19} />
              </button>
            </div>
          </motion.div>

          <motion.div
              className="landing-preview-card"
              initial={{ opacity: 0, x: 36, scale: 0.96 }}
              animate={{ opacity: 1, x: 0, scale: 1 }}
              transition={{ duration: 0.65, delay: 0.15 }}
          >
            <div className="preview-topbar">
              <span />
              <span />
              <span />
            </div>

            <div className="preview-header">
              <div>
                <span>Live dashboard</span>
                <h3>Reservation Control Center</h3>
              </div>

              <div className="preview-live">
                <span className="status-dot" />
                Online
              </div>
            </div>

            <div className="preview-stats">
              <div>
                <DoorOpen size={22} />
                <strong>Rooms</strong>
                <span>Availability search</span>
              </div>
              <div>
                <ClipboardCheck size={22} />
                <strong>Approvals</strong>
                <span>Role-based decisions</span>
              </div>
              <div>
                <BarChart3 size={22} />
                <strong>Reports</strong>
                <span>Advanced analytics</span>
              </div>
            </div>

            <div className="preview-flow">
              <div className="flow-item">
                <CheckCircle2 size={18} />
                Search available rooms
              </div>
              <div className="flow-item">
                <CheckCircle2 size={18} />
                Create pending reservation
              </div>
              <div className="flow-item">
                <CheckCircle2 size={18} />
                Approve through stored function
              </div>
            </div>
          </motion.div>
        </main>
      </div>
  );
}

function LoginScreen({ onLogin, onBack, onRegisterClick }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setLoading(true);
    try {
      const user = await login(username.trim(), password);
      setPassword("");
      onLogin(user);
    } catch (error) {
      toast.error(error.message);
    } finally {
      setLoading(false);
    }
  }

  return (
      <div className="auth-page">
        <div className="landing-orb orb-one" />
        <div className="landing-orb orb-two" />
        <div className="landing-orb orb-three" />
        <motion.div className="auth-card"
                    initial={{ opacity: 0, y: 28, scale: 0.96 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    transition={{ duration: 0.55, ease: "easeOut" }}>
          <button className="auth-back-button" onClick={onBack}>← Back to home</button>
          <div className="auth-left-panel">
            <div className="auth-logo"><img src="/logo.png" alt="Room Reservation logo" /></div>
            <div className="auth-left-copy">
              <div className="landing-eyebrow"><ShieldCheck size={16} /> Secure access</div>
              <h2>Access the reservation workspace.</h2>
              <p>Sign in with your assigned account to manage rooms, reservations and approvals.</p>
            </div>
          </div>
          <div className="auth-right-panel">
            <div className="auth-form-header">
              <div className="auth-icon-badge"><UserRound size={24} /></div>
              <div><h1>Sign In</h1><p>Enter your account credentials.</p></div>
            </div>
            <form className="login-form polished" onSubmit={submit}>
              <label>Username
                <input type="text" autoComplete="username" required value={username}
                       onChange={(event) => setUsername(event.target.value)} />
              </label>
              <label>Password
                <input type="password" autoComplete="current-password" required value={password}
                       onChange={(event) => setPassword(event.target.value)} />
              </label>
              <button className="primary full auth-submit" disabled={loading || !username.trim() || !password}>
                {loading ? "Signing in..." : "Sign in"}<ArrowRight size={18} />
              </button>
              <button className="auth-link-button" type="button" onClick={onRegisterClick}>
                Request access
              </button>
            </form>
          </div>
        </motion.div>
      </div>
  );
}

function RegisterScreen({ onBack, onLoginClick }) {
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    username: "",
    role: "regular",
  });

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function submit(e) {
    e.preventDefault();

    if (!form.fullName.trim() || !form.email.trim() || !form.username.trim()) {
      toast.error("Please fill in all required fields.");
      return;
    }

    toast.success("Registration request submitted as demo.");
    onLoginClick();
  }

  return (
      <div className="register-page">
        <div className="landing-orb orb-one" />
        <div className="landing-orb orb-two" />

        <motion.div
            className="register-card"
            initial={{ opacity: 0, y: 28, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.5 }}
        >
          <button className="auth-back-button" onClick={onBack}>
            ← Back to home
          </button>

          <div className="register-left">
            <img src="/logo.png" alt="Room Reservation logo" />

            <div>
              <div className="landing-eyebrow">
                <UserRound size={16} />
                Account request
              </div>

              <h1>Request system access</h1>
              <p>
                This screen is a production-style UI demo. Real project access is
                based on predefined database users and roles in the PostgreSQL
                schema.
              </p>
            </div>
          </div>

          <form className="register-form" onSubmit={submit}>
            <label>
              Full name
              <input
                  value={form.fullName}
                  onChange={(e) => updateField("fullName", e.target.value)}
                  placeholder="Example: Nikola Sarafimov"
              />
            </label>

            <label>
              Email
              <input
                  type="email"
                  value={form.email}
                  onChange={(e) => updateField("email", e.target.value)}
                  placeholder="example@finki.ukim.mk"
              />
            </label>

            <label>
              Username
              <input
                  value={form.username}
                  onChange={(e) => updateField("username", e.target.value)}
                  placeholder="example_user"
              />
            </label>

            <label>
              Requested role
              <select value={form.role} onChange={(e) => updateField("role", e.target.value)}>
                <option value="regular">regular</option>
                <option value="approver">approver</option>
              </select>
            </label>

            <button className="landing-primary full" type="submit">
              Submit access request
              <ArrowRight size={19} />
            </button>

            <button className="auth-link-button" type="button" onClick={onLoginClick}>
              Already have access? Login
            </button>
          </form>
        </motion.div>
      </div>
  );
}

function AppShell({ activeUser, activePage, setActivePage, onLogout, children }) {
  const items = useMemo(() => {
    const base = [
      { key: "dashboard", label: "Dashboard", icon: Activity, roles: ["regular", "approver", "admin"] },
      { key: "search", label: "Search Rooms", icon: Search, roles: ["regular", "approver", "admin"] },
      { key: "create", label: "Create Reservation", icon: PlusCircle, roles: ["regular", "approver", "admin"] },
      { key: "approvals", label: "Approvals", icon: ClipboardCheck, roles: ["approver", "admin"] },
      { key: "reports", label: "Reports", icon: BarChart3, roles: ["regular", "approver", "admin"] },
      { key: "admin", label: "Admin Panel", icon: ShieldCheck, roles: ["admin"] },
    ];

    return base.filter((item) => item.roles.includes(activeUser.role));
  }, [activeUser.role]);

  const activeItem = items.find((item) => item.key === activePage);

  return (
      <div className="app-shell">
        <aside className="sidebar">
          <div className="brand brand-only-logo">
            <div className="brand-logo">
              <img src="/logo.png" alt="Room Reservation logo" />
            </div>
          </div>

          <nav className="nav-menu">
            {items.map((item) => {
              const Icon = item.icon;

              return (
                  <button
                      key={item.key}
                      className={activePage === item.key ? "active" : ""}
                      onClick={() => setActivePage(item.key)}
                  >
                    <Icon size={19} />
                    <span>{item.label}</span>
                  </button>
              );
            })}
          </nav>

          <div className="sidebar-footer">
            <div className="connection-card">
              <Database size={18} />
              <div>
                <strong>Database</strong>
                <span>PostgreSQL</span>
              </div>
            </div>

            <div className="user-card">
              <div className="avatar small">{getInitials(activeUser.fullName)}</div>

              <div>
                <strong>{activeUser.fullName}</strong>
                <span>{activeUser.role}</span>
              </div>

              <button className="icon-button" onClick={onLogout} title="Logout">
                <LogOut size={17} />
              </button>
            </div>
          </div>
        </aside>

        <main className="main-area">
          <header className="topbar">
            <div>
              <span className="topbar-label">Current module</span>
              <h2>{activeItem?.label || "Room Reservation"}</h2>
            </div>

            <div className="topbar-actions">
              <div className="status-pill">
                <span className="status-dot" />
                Live database
              </div>

              <div className="top-user">
                <UserRound size={18} />
                <div>
                  <strong>{activeUser.fullName}</strong>
                  <span>{activeUser.role}</span>
                </div>
              </div>
            </div>
          </header>

          {children}
        </main>
      </div>
  );
}

function Dashboard({ activeUser, setActivePage }) {
  const [stats, setStats] = useState(null);
  const [health, setHealth] = useState(null);

  useEffect(() => {
    getDashboardStats()
        .then(setStats)
        .catch((err) => toast.error(err.message));

    getHealth()
        .then(setHealth)
        .catch((err) => toast.error(err.message));
  }, []);

  return (
      <section>
        <Hero
            eyebrow="System overview"
            title={`Welcome back, ${activeUser.fullName}`}
            subtitle="Control center for reservations, approvals, database reports and live project data."
        />

        {!stats && <SkeletonGrid />}

        {stats && (
            <div className="stats-grid">
              <StatCard icon={DoorOpen} label="Rooms" value={stats.totalRooms} accent="blue" />
              <StatCard icon={Boxes} label="Equipment Types" value={stats.totalEquipmentTypes} accent="violet" />
              <StatCard icon={Users} label="Users" value={stats.totalUsers} accent="cyan" />
              <StatCard icon={CalendarCheck} label="Pending Reservations" value={stats.pendingReservations} accent="amber" />
              <StatCard icon={CheckCircle2} label="Approved Reservations" value={stats.approvedReservations} accent="green" />
            </div>
        )}

        <div className="dashboard-grid">
          <div className="panel wide">
            <div className="panel-header">
              <div>
                <h3>Quick actions</h3>
                <p>Common workflows used during the project demonstration.</p>
              </div>
            </div>

            <div className="action-grid">
              <ActionCard
                  icon={Search}
                  title="Search available rooms"
                  description="Check capacity, type, equipment and time overlap."
                  onClick={() => setActivePage("search")}
              />

              <ActionCard
                  icon={PlusCircle}
                  title="Create reservation"
                  description="Create a pending request using a transaction."
                  onClick={() => setActivePage("create")}
              />

              {(activeUser.role === "approver" || activeUser.role === "admin") && (
                  <ActionCard
                      icon={ClipboardCheck}
                      title="Review approvals"
                      description="Approve or reject pending reservations."
                      onClick={() => setActivePage("approvals")}
                  />
              )}

              <ActionCard
                  icon={BarChart3}
                  title="Open reports"
                  description="View advanced SQL analytical reports."
                  onClick={() => setActivePage("reports")}
              />
            </div>
          </div>

          <div className="panel">
            <div className="panel-header">
              <div>
                <h3>Database status</h3>
                <p>Live connection check from backend.</p>
              </div>
            </div>

            <div className="role-card">
              <Database size={34} />
              <div>
                <span>Status</span>
                <strong>{health ? `${health.status} · ${health.database}` : "Loading..."}</strong>
                {health && <small>Schema: {health.schema} · Rooms: {health.roomCount}</small>}
              </div>
            </div>

            <div className="role-card second">
              <ShieldCheck size={34} />
              <div>
                <span>Active role</span>
                <strong>{activeUser.role}</strong>
                <small>Navigation is filtered by role.</small>
              </div>
            </div>
          </div>
        </div>
      </section>
  );
}

function PlaceholderPage({ icon: Icon, title, subtitle }) {
  return (
      <section>
        <Hero
            eyebrow="Next implementation phase"
            title={title}
            subtitle={subtitle}
        />

        <div className="panel placeholder-panel">
          <Icon size={42} />
          <h3>{title}</h3>
          <p>
            The UI shell is ready. This module will be connected to the backend
            endpoints in the next phase.
          </p>
        </div>
      </section>
  );
}

function PageMotion({ children }) {
  return (
      <motion.div
          initial={{ opacity: 0, y: 18, filter: "blur(6px)" }}
          animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
          exit={{ opacity: 0, y: -12, filter: "blur(4px)" }}
          transition={{ duration: 0.28, ease: "easeOut" }}
      >
        {children}
      </motion.div>
  );
}

function Hero({ eyebrow, title, subtitle }) {
  return (
      <div className="hero">
        <div>
          <div className="eyebrow">
            <Sparkles size={16} />
            {eyebrow}
          </div>
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>
      </div>
  );
}

function StatCard({ icon: Icon, label, value, accent }) {
  return (
      <motion.div className={`stat-card ${accent}`} whileHover={{ y: -6 }}>
        <div className="stat-icon">
          <Icon size={24} />
        </div>
        <span>{label}</span>
        <strong>{value}</strong>
      </motion.div>
  );
}

function ActionCard({ icon: Icon, title, description, onClick }) {
  return (
      <motion.button className="action-card" whileHover={{ y: -4 }} onClick={onClick}>
        <Icon size={24} />
        <strong>{title}</strong>
        <span>{description}</span>
      </motion.button>
  );
}

function Badge({ value }) {
  const normalized = String(value || "").toLowerCase();
  return <span className={`badge ${normalized}`}>{value}</span>;
}

function SkeletonGrid() {
  return (
      <div className="stats-grid">
        {[1, 2, 3, 4, 5].map((item) => (
            <div className="skeleton-card" key={item} />
        ))}
      </div>
  );
}

function getInitials(name) {
  if (!name) {
    return "U";
  }

  return name
      .split(" ")
      .map((part) => part[0])
      .slice(0, 2)
      .join("")
      .toUpperCase();
}

export default App;