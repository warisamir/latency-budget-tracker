import { Link } from "react-router-dom";

export default function Navigation() {
  return (
    <nav className="border-b border-slate-700 bg-slate-900 px-6 py-4">
      <div className="flex items-center justify-between">
        <Link to="/" className="text-2xl font-bold text-blue-400">
          Latency Budget Tracker
        </Link>
        <div className="flex gap-6">
          <Link
            to="/"
            className="text-slate-300 transition-colors hover:text-white"
          >
            Dashboard
          </Link>
          <Link
            to="/performance"
            className="text-slate-300 transition-colors hover:text-white"
          >
            Performance
          </Link>
          <Link
            to="/alerts"
            className="text-slate-300 transition-colors hover:text-white"
          >
            Alerts
          </Link>
          <Link
            to="/monitoring"
            className="text-slate-300 transition-colors hover:text-white"
          >
            Monitoring
          </Link>
        </div>
      </div>
    </nav>
  );
}
