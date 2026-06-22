import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";

export default function Monitoring() {
  const { data: health } = useQuery({
    queryKey: ["health"],
    queryFn: api.getHealth,
    refetchInterval: 10_000,
  });

  const grafanaUrl = "http://localhost:3000";
  const kibanaUrl = "http://localhost:5601";
  const prometheusUrl = "http://localhost:9090";

  const getStatusColor = (status?: string) => {
    if (!status) return "secondary";
    return status.toLowerCase() === "up" ? "success" : "destructive";
  };

  return (
    <div className="min-h-screen bg-slate-950 p-6">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-white">🔍 Monitoring & Observability</h1>
        <p className="mt-2 text-sm text-slate-400">Central hub for observability tools and dashboards</p>
      </div>

      {/* System Health */}
      {health && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>System Health Status</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-3 gap-6">
              <div className="rounded-lg border border-slate-700 bg-slate-900 p-4">
                <div className="text-sm text-slate-400">Overall Status</div>
                <div className="mt-2 flex items-center gap-2">
                  <div className="text-2xl font-bold text-white">{health.status}</div>
                  <Badge variant={getStatusColor(health.status)}>{health.status}</Badge>
                </div>
              </div>

              <div className="rounded-lg border border-slate-700 bg-slate-900 p-4">
                <div className="text-sm text-slate-400">Database</div>
                <div className="mt-2 flex items-center gap-2">
                  <div className="text-lg font-mono text-slate-200">{health.components?.db?.status || "Unknown"}</div>
                  <Badge variant={getStatusColor(health.components?.db?.status)}>
                    {health.components?.db?.status || "Unknown"}
                  </Badge>
                </div>
              </div>

              <div className="rounded-lg border border-slate-700 bg-slate-900 p-4">
                <div className="text-sm text-slate-400">Redis Cache</div>
                <div className="mt-2 flex items-center gap-2">
                  <div className="text-lg font-mono text-slate-200">{health.components?.redis?.status || "Unknown"}</div>
                  <Badge variant={getStatusColor(health.components?.redis?.status)}>
                    {health.components?.redis?.status || "Unknown"}
                  </Badge>
                </div>
              </div>
            </div>
            {health.timestamp && (
              <div className="mt-4 text-xs text-slate-500">
                Last updated: {new Date(health.timestamp).toLocaleTimeString()}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Quick Links */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>External Observability Tools</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-4">
            <a
              href={grafanaUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex flex-col gap-3 rounded-lg border border-slate-700 bg-slate-900 p-4 hover:border-blue-500 transition"
            >
              <div className="text-lg font-bold text-blue-400">📊 Grafana</div>
              <div className="text-sm text-slate-400">Metrics visualization & dashboards</div>
              <Button size="sm" variant="outline" className="w-full justify-center">
                Open Grafana →
              </Button>
            </a>

            <a
              href={kibanaUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex flex-col gap-3 rounded-lg border border-slate-700 bg-slate-900 p-4 hover:border-yellow-500 transition"
            >
              <div className="text-lg font-bold text-yellow-400">📝 Kibana</div>
              <div className="text-sm text-slate-400">Log search & analysis</div>
              <Button size="sm" variant="outline" className="w-full justify-center">
                Open Kibana →
              </Button>
            </a>

            <a
              href={prometheusUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex flex-col gap-3 rounded-lg border border-slate-700 bg-slate-900 p-4 hover:border-green-500 transition"
            >
              <div className="text-lg font-bold text-green-400">📈 Prometheus</div>
              <div className="text-sm text-slate-400">Time-series metrics DB</div>
              <Button size="sm" variant="outline" className="w-full justify-center">
                Open Prometheus →
              </Button>
            </a>
          </div>
        </CardContent>
      </Card>

      {/* Kibana Pre-built Searches */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Kibana Saved Searches</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-4">
            <a
              href={`${kibanaUrl}/app/discover#/?_a=(query:(language:kuery,query:'level:ERROR'))`}
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-lg border border-slate-700 bg-slate-900 p-4 hover:bg-slate-800 transition"
            >
              <div className="font-semibold text-red-400">🔴 Error Logs</div>
              <div className="mt-1 text-sm text-slate-400">All ERROR level logs</div>
            </a>

            <a
              href={`${kibanaUrl}/app/discover#/?_a=(query:(language:kuery,query:'logger:%22AlertService%22'))`}
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-lg border border-slate-700 bg-slate-900 p-4 hover:bg-slate-800 transition"
            >
              <div className="font-semibold text-orange-400">⚠️ Alert Service</div>
              <div className="mt-1 text-sm text-slate-400">Alerts from AlertService</div>
            </a>

            <a
              href={`${kibanaUrl}/app/discover#/?_a=(query:(language:kuery,query:'logger:%22LatencyBudgetEngine%22'))`}
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-lg border border-slate-700 bg-slate-900 p-4 hover:bg-slate-800 transition"
            >
              <div className="font-semibold text-purple-400">💔 Budget Violations</div>
              <div className="mt-1 text-sm text-slate-400">Latency budget violations</div>
            </a>

            <a
              href={`${kibanaUrl}/app/discover`}
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-lg border border-slate-700 bg-slate-900 p-4 hover:bg-slate-800 transition"
            >
              <div className="font-semibold text-blue-400">🔎 Advanced Search</div>
              <div className="mt-1 text-sm text-slate-400">Custom trace ID / query search</div>
            </a>
          </div>
        </CardContent>
      </Card>

      {/* Grafana Dashboards */}
      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Embedded Grafana Dashboards</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-slate-400 mb-4">
              Grafana dashboards are embedded below. Use the time selector to adjust the time range across all dashboards.
            </p>
          </CardContent>
        </Card>

        {/* Latency Budget Overview */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Latency Budget Overview</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <iframe
              src={`${grafanaUrl}/d/latency-overview?kiosk=tv&refresh=30s`}
              width="100%"
              height="600"
              frameBorder="0"
              className="rounded-lg"
              title="Latency Budget Overview"
            />
          </CardContent>
        </Card>

        {/* API Health & Circuit Breakers */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">API Health & Circuit Breakers</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <iframe
              src={`${grafanaUrl}/d/api-health?kiosk=tv&refresh=30s`}
              width="100%"
              height="600"
              frameBorder="0"
              className="rounded-lg"
              title="API Health & Circuit Breakers"
            />
          </CardContent>
        </Card>

        {/* JVM Metrics */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">JVM Metrics</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <iframe
              src={`${grafanaUrl}/d/jvm-metrics?kiosk=tv&refresh=30s`}
              width="100%"
              height="600"
              frameBorder="0"
              className="rounded-lg"
              title="JVM Metrics"
            />
          </CardContent>
        </Card>

        {/* Database & Cache Monitoring */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Database & Cache Monitoring</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <iframe
              src={`${grafanaUrl}/d/database-redis?kiosk=tv&refresh=30s`}
              width="100%"
              height="600"
              frameBorder="0"
              className="rounded-lg"
              title="Database & Cache Monitoring"
            />
          </CardContent>
        </Card>
      </div>

      <div className="mt-12 text-center text-xs text-slate-500">
        Observability Hub · Real-time monitoring · All external links open in new window
      </div>
    </div>
  );
}
