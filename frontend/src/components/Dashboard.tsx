import React, { useEffect, useState, useCallback } from "react";
import { api } from "../api/client";
import type { LatencyStats, AlertDto, TransactionResponse, StageStats } from "../types";

// ─── Severity colour mapping ──────────────────────────────────────────────────
const SEV_COLOR: Record<string, string> = {
  OK: "#22c55e",
  LOW: "#facc15",
  MEDIUM: "#f97316",
  HIGH: "#ef4444",
  CRITICAL: "#7c3aed",
};

const STAGE_LABELS: Record<string, string> = {
  AUTHENTICATION:        "Auth",
  VALIDATION:            "Validation",
  BUSINESS_LOGIC:        "Business Logic",
  CACHE_ACCESS:          "Cache",
  DATABASE_QUERY:        "Database",
  RESPONSE_SERIALIZATION:"Response",
};

// ─── Components ──────────────────────────────────────────────────────────────

function SeverityBadge({ value }: { value: string }) {
  return (
    <span style={{
      background: SEV_COLOR[value] || "#6b7280",
      color: "#fff",
      borderRadius: 4,
      padding: "2px 8px",
      fontSize: 12,
      fontWeight: 700,
    }}>
      {value}
    </span>
  );
}

function ProgressBar({ actual, budget }: { actual: number; budget: number }) {
  const pct = Math.min((actual / budget) * 100, 200);
  const color = pct > 150 ? SEV_COLOR.CRITICAL
              : pct > 100 ? SEV_COLOR.HIGH
              : pct > 75  ? SEV_COLOR.MEDIUM
              : SEV_COLOR.OK;
  return (
    <div style={{ background: "#1e293b", borderRadius: 4, height: 8, position: "relative", overflow: "hidden" }}>
      <div style={{ width: `${Math.min(pct, 100)}%`, height: "100%", background: color, transition: "width 0.4s" }} />
      {pct > 100 && (
        <div style={{ position: "absolute", top: 0, left: "100%", width: `${pct - 100}%`, height: "100%", background: SEV_COLOR.CRITICAL, opacity: 0.6 }} />
      )}
    </div>
  );
}

function StageCard({ s }: { s: StageStats }) {
  return (
    <div style={{
      background: "#1e293b",
      borderRadius: 10,
      padding: "14px 18px",
      borderLeft: `4px solid ${s.healthy ? SEV_COLOR.OK : SEV_COLOR.HIGH}`,
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
        <strong style={{ color: "#e2e8f0" }}>{STAGE_LABELS[s.stage]}</strong>
        <span style={{ color: s.healthy ? SEV_COLOR.OK : SEV_COLOR.HIGH, fontSize: 12, fontWeight: 700 }}>
          {s.healthy ? "✓ HEALTHY" : "✗ DEGRADED"}
        </span>
      </div>
      <ProgressBar actual={s.p95Ms} budget={s.budgetMs} />
      <div style={{ display: "flex", gap: 16, marginTop: 8, fontSize: 12, color: "#94a3b8" }}>
        <span>P50: <strong style={{ color: "#e2e8f0" }}>{(s.p50Ms || 0).toFixed(1)}ms</strong></span>
        <span>P95: <strong style={{ color: "#e2e8f0" }}>{(s.p95Ms || 0).toFixed(1)}ms</strong></span>
        <span>P99: <strong style={{ color: "#e2e8f0" }}>{(s.p99Ms || 0).toFixed(1)}ms</strong></span>
        <span>Budget: <strong style={{ color: "#e2e8f0" }}>{s.budgetMs}ms</strong></span>
      </div>
      {s.violations > 0 && (
        <div style={{ marginTop: 6, fontSize: 11, color: SEV_COLOR.MEDIUM }}>
          ⚠ {s.violations} violations ({(s.violationRate || 0).toFixed(1)}% rate)
        </div>
      )}
    </div>
  );
}

function TransactionForm({ onResult }: { onResult: (r: TransactionResponse) => void }) {
  const [form, setForm] = useState({ userId: "user-demo", fromCurrency: "BTC", toCurrency: "USD", amount: "1.0" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const result = await api.createTransaction({
        ...form,
        amount: parseFloat(form.amount),
      });
      onResult(result);
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message);
    } finally {
      setLoading(false);
    }
  };

  const inp: React.CSSProperties = {
    background: "#1e293b", border: "1px solid #334155", borderRadius: 6,
    color: "#e2e8f0", padding: "8px 12px", fontSize: 14, width: "100%",
  };

  return (
    <form onSubmit={submit} style={{ display: "flex", flexDirection: "column", gap: 10 }}>
      {["userId", "fromCurrency", "toCurrency", "amount"].map(field => (
        <div key={field}>
          <label style={{ color: "#94a3b8", fontSize: 12, display: "block", marginBottom: 4 }}>
            {field.replace(/([A-Z])/g, " $1").trim()}
          </label>
          <input
            style={inp}
            value={(form as any)[field]}
            onChange={e => setForm(f => ({ ...f, [field]: e.target.value }))}
            placeholder={field}
          />
        </div>
      ))}
      {error && <div style={{ color: SEV_COLOR.HIGH, fontSize: 12 }}>⚠ {error}</div>}
      <button type="submit" disabled={loading} style={{
        background: loading ? "#334155" : "#3b82f6",
        color: "#fff", border: "none", borderRadius: 6,
        padding: "10px 16px", fontWeight: 700, cursor: loading ? "not-allowed" : "pointer",
      }}>
        {loading ? "Processing…" : "Run Transaction"}
      </button>
    </form>
  );
}

function ResultCard({ result }: { result: TransactionResponse }) {
  const r = result.latencyReport;
  return (
    <div style={{ background: "#1e293b", borderRadius: 10, padding: 14, marginTop: 12 }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
        <strong style={{ color: "#e2e8f0" }}>Transaction Result</strong>
        <SeverityBadge value={r.withinBudget ? "OK" : r.worstSeverity} />
      </div>
      <div style={{ fontSize: 12, color: "#94a3b8", marginBottom: 8 }}>
        ID: <span style={{ color: "#e2e8f0" }}>{(result.transactionId || "").slice(0, 16)}…</span>
      </div>
      <div style={{ fontSize: 13, color: "#94a3b8" }}>
        {result.fromCurrency} → {result.toCurrency}: <strong style={{ color: "#e2e8f0" }}>
          {result.convertedAmount} @ {result.exchangeRate}
        </strong>
      </div>
      <div style={{ fontSize: 12, color: "#94a3b8", marginTop: 8 }}>
        Total: <strong>{r.totalLatencyMs}ms</strong> / budget {r.budgetMs}ms
        {!r.withinBudget && <span style={{ color: SEV_COLOR.HIGH }}> (+{(r.overallDeviationPercent || 0).toFixed(1)}% over)</span>}
      </div>
      <div style={{ marginTop: 10, display: "flex", flexDirection: "column", gap: 4 }}>
        {(r.stages || []).map(s => (
          <div key={s.stage} style={{ display: "flex", justifyContent: "space-between", fontSize: 12 }}>
            <span style={{ color: "#94a3b8" }}>{STAGE_LABELS[s.stage]}</span>
            <span style={{ color: s.exceeded ? SEV_COLOR.MEDIUM : SEV_COLOR.OK }}>
              {s.actualMs}ms / {s.budgetMs}ms {s.exceeded && `(+${(s.deviationPercent || 0).toFixed(1)}%)`}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Main Dashboard ───────────────────────────────────────────────────────────

export default function Dashboard() {
  const [stats, setStats]       = useState<LatencyStats | null>(null);
  const [alerts, setAlerts]     = useState<AlertDto[]>([]);
  const [window, setWindow]     = useState<"1h" | "24h" | "7d">("1h");
  const [lastTx, setLastTx]     = useState<TransactionResponse | null>(null);
  const [loading, setLoading]   = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const [s, a] = await Promise.all([api.getStats(window), api.getCriticalAlerts()]);
      setStats(s);
      setAlerts(a);
    } catch { /* ignore; show stale data */ }
    finally { setLoading(false); }
  }, [window]);

  useEffect(() => { refresh(); }, [refresh]);
  useEffect(() => {
    const id = setInterval(refresh, 15_000);
    return () => clearInterval(id);
  }, [refresh]);

  const resolveAlert = async (id: number) => {
    await api.resolveAlert(id);
    setAlerts(a => a.filter(x => x.id !== id));
  };

  const card: React.CSSProperties = {
    background: "#0f172a", borderRadius: 12, padding: 20, color: "#e2e8f0",
    border: "1px solid #1e293b",
  };

  return (
    <div style={{ minHeight: "100vh", background: "#020617", fontFamily: "Inter, sans-serif", padding: 24 }}>
      {/* Header */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
        <div>
          <h1 style={{ color: "#e2e8f0", margin: 0, fontSize: 22 }}>⏱ Latency Budget Tracker</h1>
          <div style={{ color: "#64748b", fontSize: 13, marginTop: 4 }}>Production-grade observability dashboard</div>
        </div>
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          {(["1h", "24h", "7d"] as const).map(w => (
            <button key={w} onClick={() => setWindow(w)} style={{
              background: window === w ? "#3b82f6" : "#1e293b",
              color: "#fff", border: "none", borderRadius: 6,
              padding: "6px 14px", fontWeight: 600, cursor: "pointer",
            }}>{w}</button>
          ))}
          <button onClick={refresh} disabled={loading} style={{
            background: "#1e293b", color: "#94a3b8", border: "none",
            borderRadius: 6, padding: "6px 12px", cursor: "pointer",
          }}>{loading ? "…" : "↻"}</button>
        </div>
      </div>

      {/* Top metrics */}
      {stats && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 16, marginBottom: 24 }}>
          {[
            { label: "Total Requests", value: stats.totalRequests, color: "#3b82f6" },
            { label: "Budget Violations", value: stats.budgetViolations, color: SEV_COLOR.MEDIUM },
            { label: "Violation Rate", value: (stats.violationRate || 0).toFixed(1) + "%", color: (stats.violationRate || 0) > 5 ? SEV_COLOR.HIGH : SEV_COLOR.OK },
            { label: "Active Alerts", value: alerts.length, color: alerts.length > 0 ? SEV_COLOR.CRITICAL : SEV_COLOR.OK },
          ].map(m => (
            <div key={m.label} style={{ ...card, textAlign: "center" }}>
              <div style={{ fontSize: 28, fontWeight: 800, color: m.color }}>{m.value}</div>
              <div style={{ fontSize: 12, color: "#64748b", marginTop: 4 }}>{m.label}</div>
            </div>
          ))}
        </div>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 20 }}>
        {/* Stage cards */}
        <div style={card}>
          <h3 style={{ margin: "0 0 16px", color: "#e2e8f0", fontSize: 15 }}>Stage Health — P95 vs Budget</h3>
          {stats && stats.stageStats && stats.stageStats.length > 0 ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              {stats.stageStats.map(s => <StageCard key={s.stage} s={s} />)}
            </div>
          ) : (
            <div style={{ color: "#64748b", textAlign: "center", padding: 40 }}>Loading…</div>
          )}
        </div>

        {/* Right column */}
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          {/* Transaction tester */}
          <div style={card}>
            <h3 style={{ margin: "0 0 16px", color: "#e2e8f0", fontSize: 15 }}>Run Transaction</h3>
            <TransactionForm onResult={r => { setLastTx(r); refresh(); }} />
            {lastTx && <ResultCard result={lastTx} />}
          </div>

          {/* Critical alerts */}
          <div style={card}>
            <h3 style={{ margin: "0 0 12px", color: "#e2e8f0", fontSize: 15 }}>
              Critical Alerts
              {alerts.length > 0 && (
                <span style={{ background: SEV_COLOR.CRITICAL, borderRadius: 10, padding: "2px 8px", fontSize: 11, marginLeft: 8 }}>
                  {alerts.length}
                </span>
              )}
            </h3>
            {!alerts || alerts.length === 0 ? (
              <div style={{ color: SEV_COLOR.OK, fontSize: 13, textAlign: "center", padding: 20 }}>✓ No active alerts</div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                {(alerts || []).slice(0, 8).map(a => (
                  <div key={a.id} style={{ background: "#1e293b", borderRadius: 8, padding: "10px 12px", borderLeft: `3px solid ${SEV_COLOR[a.severity]}` }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontSize: 12, color: "#94a3b8" }}>{STAGE_LABELS[a.stage]}</span>
                      <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                        <SeverityBadge value={a.severity} />
                        <button onClick={() => resolveAlert(a.id)} style={{
                          background: "none", border: "1px solid #334155", borderRadius: 4,
                          color: "#94a3b8", fontSize: 11, cursor: "pointer", padding: "2px 6px",
                        }}>Resolve</button>
                      </div>
                    </div>
                    <div style={{ fontSize: 11, color: "#64748b", marginTop: 4 }}>
                      {a.actualLatencyMs}ms / {a.budgetMs}ms budget (+{(a.deviationPercent || 0).toFixed(1)}%)
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Footer */}
      <div style={{ textAlign: "center", color: "#334155", fontSize: 11, marginTop: 32 }}>
        Latency Budget Tracker · Auto-refreshes every 15s · Window: {window}
        {stats && ` · Last updated: ${new Date(stats.generatedAt).toLocaleTimeString()}`}
      </div>
    </div>
  );
}
