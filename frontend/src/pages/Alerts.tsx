import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { AlertDto, Severity } from "../types";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import ViolationRateChart from "../components/charts/ViolationRateChart";
import SeverityPieChart from "../components/charts/SeverityPieChart";

type SeverityFilter = "ALL" | Severity;
type BadgeVariant = "default" | "success" | "warning" | "destructive" | "secondary";

const SEVERITY_COLORS = {
  OK: "success",
  LOW: "default",
  MEDIUM: "warning",
  HIGH: "warning",
  CRITICAL: "destructive",
} as const satisfies Record<Severity, BadgeVariant>;

const getSeverityVariant = (severity: Severity): BadgeVariant => {
  return SEVERITY_COLORS[severity] as BadgeVariant;
};

export default function Alerts() {
  const [page, setPage] = useState(0);
  const [severityFilter, setSeverityFilter] = useState<SeverityFilter>("ALL");
  const [resolvedFilter, setResolvedFilter] = useState<"ALL" | "ACTIVE" | "RESOLVED">("ACTIVE");

  const { data: alertsData, refetch: refetchAlerts } = useQuery({
    queryKey: ["alerts", page],
    queryFn: () => api.getAlerts(page, 20),
    refetchInterval: 15_000,
  });

  const { data: alertCount } = useQuery({
    queryKey: ["alertCount"],
    queryFn: api.getAlertCount,
    refetchInterval: 15_000,
  });

  const { data: stats } = useQuery({
    queryKey: ["stats"],
    queryFn: () => api.getStats("24h"),
  });

  const alerts = alertsData?.content || [];
  const filteredAlerts = alerts.filter((a) => {
    const severityMatch = severityFilter === "ALL" || a.severity === severityFilter;
    const resolvedMatch =
      resolvedFilter === "ALL" || (resolvedFilter === "ACTIVE" && !a.resolved) || (resolvedFilter === "RESOLVED" && a.resolved);
    return severityMatch && resolvedMatch;
  });

  const handleResolve = async (id: number) => {
    await api.resolveAlert(id);
    await refetchAlerts();
  };

  const severityCounts = (stats?.stageStats && Array.isArray(stats.stageStats))
    ? stats.stageStats.reduce((acc, s) => {
        acc[s.healthy ? "OK" : "HIGH"] = (acc[s.healthy ? "OK" : "HIGH"] || 0) + 1;
        return acc;
      }, {} as Record<string, number>)
    : {};

  return (
    <div className="min-h-screen bg-slate-950 p-6">
      <div className="mb-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-white">🚨 Alerts & Violations</h1>
            <p className="mt-2 text-sm text-slate-400">Monitor and manage active alerts</p>
          </div>
          {alertCount !== undefined && (
            <div className="text-right">
              <div className="text-4xl font-bold text-white">{alertCount}</div>
              <div className="text-sm text-slate-400">Active Alerts</div>
            </div>
          )}
        </div>
      </div>

      {/* Charts */}
      <div className="mb-8 grid grid-cols-2 gap-6">
        {stats?.stageStats && (
          <>
            <Card>
              <CardHeader>
                <CardTitle>Violation Rate by Stage</CardTitle>
              </CardHeader>
              <CardContent>
                <ViolationRateChart stageStats={stats.stageStats} />
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Severity Distribution</CardTitle>
              </CardHeader>
              <CardContent>
                <SeverityPieChart severityCounts={severityCounts} />
              </CardContent>
            </Card>
          </>
        )}
      </div>

      {/* Filters */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-lg">Filters</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-4">
            <div>
              <div className="mb-2 text-sm text-slate-400">Severity</div>
              <div className="flex gap-2">
                {(["ALL", "LOW", "MEDIUM", "HIGH", "CRITICAL"] as const).map((sev) => (
                  <Button
                    key={sev}
                    onClick={() => setSeverityFilter(sev)}
                    variant={severityFilter === sev ? "default" : "outline"}
                    size="sm"
                  >
                    {sev}
                  </Button>
                ))}
              </div>
            </div>
            <div>
              <div className="mb-2 text-sm text-slate-400">Status</div>
              <div className="flex gap-2">
                {(["ALL", "ACTIVE", "RESOLVED"] as const).map((status) => (
                  <Button
                    key={status}
                    onClick={() => setResolvedFilter(status)}
                    variant={resolvedFilter === status ? "default" : "outline"}
                    size="sm"
                  >
                    {status}
                  </Button>
                ))}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Alerts Table */}
      <Card>
        <CardHeader>
          <CardTitle>Alerts ({filteredAlerts.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {filteredAlerts.length === 0 ? (
            <div className="py-12 text-center text-slate-400">No alerts match your filters</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-700">
                    <th className="px-4 py-3 text-left text-slate-300">Stage</th>
                    <th className="px-4 py-3 text-left text-slate-300">Severity</th>
                    <th className="px-4 py-3 text-right text-slate-300">Actual / Budget</th>
                    <th className="px-4 py-3 text-right text-slate-300">Deviation</th>
                    <th className="px-4 py-3 text-left text-slate-300">Message</th>
                    <th className="px-4 py-3 text-left text-slate-300">Timestamp</th>
                    <th className="px-4 py-3 text-center text-slate-300">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAlerts.map((alert) => (
                    <tr key={alert.id} className="border-b border-slate-800 hover:bg-slate-900">
                      <td className="px-4 py-3 font-mono text-slate-200">{alert.stage.replace(/_/g, " ")}</td>
                      <td className="px-4 py-3">
                        <Badge variant={getSeverityVariant(alert.severity)}>{alert.severity}</Badge>
                      </td>
                      <td className="px-4 py-3 text-right text-slate-300">
                        {alert.actualLatencyMs}ms / {alert.budgetMs}ms
                      </td>
                      <td className="px-4 py-3 text-right text-orange-400">{(alert.deviationPercent || 0).toFixed(1)}%</td>
                      <td className="max-w-xs px-4 py-3 text-slate-400 truncate">{alert.message}</td>
                      <td className="px-4 py-3 text-slate-500 text-xs">
                        {new Date(alert.createdAt).toLocaleTimeString()}
                      </td>
                      <td className="px-4 py-3 text-center">
                        {!alert.resolved && (
                          <Button
                            onClick={() => handleResolve(alert.id)}
                            size="sm"
                            variant="outline"
                            className="text-xs"
                          >
                            Resolve
                          </Button>
                        )}
                        {alert.resolved && <span className="text-green-400 text-xs">✓ Resolved</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Pagination */}
      {alertsData && (
        <div className="mt-6 flex items-center justify-between">
          <div className="text-sm text-slate-400">
            Page {page + 1} of {Math.ceil((alertsData.totalElements || 0) / 20)}
          </div>
          <div className="flex gap-2">
            <Button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              variant="outline"
              size="sm"
            >
              ← Previous
            </Button>
            <Button
              onClick={() => setPage(page + 1)}
              disabled={!alertsData.content || alertsData.content.length < 20}
              variant="outline"
              size="sm"
            >
              Next →
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
