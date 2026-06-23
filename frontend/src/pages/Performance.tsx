import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { LatencyStats, LatencyHistoryDto } from "../types";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Button } from "../components/ui/button";
import MetricCard from "../components/MetricCard";
import StageLatencyBarChart from "../components/charts/StageLatencyBarChart";
import ViolationRateChart from "../components/charts/ViolationRateChart";
import SeverityPieChart from "../components/charts/SeverityPieChart";
import LatencyTrendChart from "../components/charts/LatencyTrendChart";
import StageRadarChart from "../components/charts/StageRadarChart";
import BudgetComplianceChart from "../components/charts/BudgetComplianceChart";
import P95vsBudgetChart from "../components/charts/P95vsBudgetChart";

type TimeWindow = "1h" | "24h" | "7d";

export default function Performance() {
  const [window, setWindow] = useState<TimeWindow>("24h");

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ["stats", window],
    queryFn: () => api.getStats(window),
    refetchInterval: 15_000,
  });

  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: ["history", window],
    queryFn: () => api.getLatencyHistory(window, window === "1h" ? 12 : window === "24h" ? 24 : 48),
    refetchInterval: 30_000,
  });

  return (
    <div className="min-h-screen bg-slate-950 p-6">
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">📊 Performance Metrics</h1>
          <p className="mt-2 text-sm text-slate-400">Detailed latency analysis and trend monitoring</p>
        </div>
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
      </div>

      {stats && (
        <div className="mb-8 grid grid-cols-4 gap-4">
          <MetricCard label="Total Requests" value={stats.totalRequests} color="text-blue-400" />
          <MetricCard label="Budget Violations" value={stats.budgetViolations} color="text-orange-400" />
          <MetricCard label="Violation Rate" value={`${(stats.violationRate || 0).toFixed(1)}%`} color={(stats.violationRate || 0) > 5 ? "text-red-400" : "text-green-400"} />
          <MetricCard label="Avg Latency" value={`${(stats.stageStats && stats.stageStats.length > 0 ? (stats.stageStats.reduce((a, s) => a + s.avgMs, 0) / stats.stageStats.length) : 0).toFixed(0)}ms`} color="text-purple-400" />
        </div>
      )}

      <div className="space-y-6">
        {/* Stage Latency Comparison */}
        {stats?.stageStats && stats.stageStats.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle>P50/P95/P99 vs Budget by Stage</CardTitle>
            </CardHeader>
            <CardContent>
              <StageLatencyBarChart stageStats={stats.stageStats} />
            </CardContent>
          </Card>
        )}

        {/* Trends + Severity */}
        <div className="grid grid-cols-2 gap-6">
          {history?.dataPoints && history.dataPoints.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Latency Trend Over Time</CardTitle>
              </CardHeader>
              <CardContent>
                <LatencyTrendChart dataPoints={history.dataPoints} />
              </CardContent>
            </Card>
          )}

          {stats?.stageStats && stats.stageStats.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Severity Distribution</CardTitle>
              </CardHeader>
              <CardContent>
                <SeverityPieChart severityCounts={{}} />
              </CardContent>
            </Card>
          )}
        </div>

        {/* Radar + Compliance */}
        <div className="grid grid-cols-2 gap-6">
          {stats?.stageStats && (
            <Card>
              <CardHeader>
                <CardTitle>Stage Health Overview</CardTitle>
              </CardHeader>
              <CardContent>
                <StageRadarChart stageStats={stats.stageStats} />
              </CardContent>
            </Card>
          )}

          {stats?.stageStats && (
            <Card>
              <CardHeader>
                <CardTitle>Budget Compliance %</CardTitle>
              </CardHeader>
              <CardContent>
                <BudgetComplianceChart stageStats={stats.stageStats} />
              </CardContent>
            </Card>
          )}
        </div>

        {/* P95 vs Budget */}
        {stats?.stageStats && (
          <Card>
            <CardHeader>
              <CardTitle>P95 Latency vs Remaining Budget</CardTitle>
            </CardHeader>
            <CardContent>
              <P95vsBudgetChart stageStats={stats.stageStats} />
            </CardContent>
          </Card>
        )}

        {/* Violation Rate by Stage */}
        {stats?.stageStats && (
          <Card>
            <CardHeader>
              <CardTitle>Violation Rate by Stage</CardTitle>
            </CardHeader>
            <CardContent>
              <ViolationRateChart stageStats={stats.stageStats} />
            </CardContent>
          </Card>
        )}
      </div>

      <div className="mt-12 text-center text-xs text-slate-500">
        Performance Metrics · Auto-refreshes every 15s · Window: {window}
      </div>
    </div>
  );
}
