import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { LatencyStats, AlertDto, TransactionResponse } from "../types";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import StageHealth from "../components/StageHealth";
import TransactionTester from "../components/TransactionTester";
import AlertsList from "../components/AlertsList";
import MetricCard from "../components/MetricCard";

type TimeWindow = "1h" | "24h" | "7d";

export default function Dashboard() {
  const [window, setWindow] = useState<TimeWindow>("1h");
  const [lastTx, setLastTx] = useState<TransactionResponse | null>(null);

  const {
    data: stats,
    isLoading: statsLoading,
    refetch: refetchStats,
  } = useQuery({
    queryKey: ["stats", window],
    queryFn: () => api.getStats(window),
    refetchInterval: 15_000,
  });

  const {
    data: alerts = [],
    refetch: refetchAlerts,
  } = useQuery({
    queryKey: ["alerts"],
    queryFn: api.getCriticalAlerts,
    refetchInterval: 15_000,
  });

  const handleTransactionResult = async (result: TransactionResponse) => {
    setLastTx(result);
    await refetchStats();
    await refetchAlerts();
  };

  const handleResolveAlert = async (id: number) => {
    await api.resolveAlert(id);
    await refetchAlerts();
  };

  return (
    <div className="min-h-screen bg-slate-950 p-6">
      {/* Header */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">⏱ Latency Budget Tracker</h1>
          <p className="mt-2 text-sm text-slate-400">Production-grade observability dashboard</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex gap-2 rounded-lg border border-slate-700 bg-slate-900 p-1">
            {(["1h", "24h", "7d"] as const).map((w) => (
              <Button
                key={w}
                onClick={() => setWindow(w)}
                variant={window === w ? "default" : "ghost"}
                size="sm"
              >
                {w}
              </Button>
            ))}
          </div>
          <Button
            onClick={() => refetchStats()}
            variant="outline"
            size="sm"
            disabled={statsLoading}
          >
            {statsLoading ? "…" : "↻"}
          </Button>
        </div>
      </div>

      {/* Top metrics */}
      {stats && (
        <div className="mb-8 grid grid-cols-4 gap-4">
          <MetricCard
            label="Total Requests"
            value={stats.totalRequests}
            color="text-blue-400"
          />
          <MetricCard
            label="Budget Violations"
            value={stats.budgetViolations}
            color="text-orange-400"
          />
          <MetricCard
            label="Violation Rate"
            value={`${(stats.violationRate || 0).toFixed(1)}%`}
            color={(stats.violationRate || 0) > 5 ? "text-red-400" : "text-green-400"}
          />
          <MetricCard
            label="Active Alerts"
            value={alerts.length}
            color={alerts.length > 0 ? "text-purple-400" : "text-green-400"}
          />
        </div>
      )}

      {/* Main content grid */}
      <div className="grid grid-cols-3 gap-6">
        {/* Left: Stage Health */}
        <div className="col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Stage Health — P95 vs Budget</CardTitle>
            </CardHeader>
            <CardContent>
              {statsLoading && !stats ? (
                <div className="flex items-center justify-center py-12 text-slate-400">
                  Loading stages…
                </div>
              ) : stats?.stageStats ? (
                <div className="space-y-4">
                  {stats.stageStats.map((stage) => (
                    <StageHealth key={stage.stage} stage={stage} />
                  ))}
                </div>
              ) : null}
            </CardContent>
          </Card>
        </div>

        {/* Right column */}
        <div className="flex flex-col gap-6">
          {/* Transaction Tester */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Run Transaction</CardTitle>
            </CardHeader>
            <CardContent>
              <TransactionTester onResult={handleTransactionResult} />
              {lastTx && (
                <div className="mt-4 border-t border-slate-700 pt-4">
                  <div className="text-xs text-slate-400">Transaction ID:</div>
                  <div className="text-sm font-mono text-slate-200">
                    {lastTx.transactionId.slice(0, 16)}…
                  </div>
                  <div className="mt-2 text-xs text-slate-400">
                    {lastTx.fromCurrency} → {lastTx.toCurrency}:{" "}
                    <span className="text-white">{lastTx.convertedAmount}</span>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Critical Alerts */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-lg">Critical Alerts</CardTitle>
              {alerts.length > 0 && (
                <Badge variant="destructive">{alerts.length}</Badge>
              )}
            </CardHeader>
            <CardContent>
              <AlertsList
                alerts={alerts}
                onResolve={handleResolveAlert}
              />
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Footer */}
      <div className="mt-12 text-center text-xs text-slate-500">
        Latency Budget Tracker · Auto-refreshes every 15s · Window: {window}
        {stats && ` · Last updated: ${new Date(stats.generatedAt).toLocaleTimeString()}`}
      </div>
    </div>
  );
}
