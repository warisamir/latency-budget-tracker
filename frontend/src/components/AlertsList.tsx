import type { AlertDto } from "../types";
import { Badge } from "./ui/badge";
import { Button } from "./ui/button";

const STAGE_LABELS: Record<string, string> = {
  AUTHENTICATION: "Auth",
  VALIDATION: "Validation",
  BUSINESS_LOGIC: "Business Logic",
  CACHE_ACCESS: "Cache",
  DATABASE_QUERY: "Database",
  RESPONSE_SERIALIZATION: "Response",
};

const getSeverityVariant = (severity: string): "default" | "success" | "warning" | "destructive" | "secondary" => {
  switch (severity) {
    case "OK":
      return "success";
    case "LOW":
      return "secondary";
    case "MEDIUM":
      return "warning";
    case "HIGH":
    case "CRITICAL":
      return "destructive";
    default:
      return "secondary";
  }
};

interface AlertsListProps {
  alerts: AlertDto[];
  onResolve: (id: number) => void;
}

export default function AlertsList({ alerts, onResolve }: AlertsListProps) {
  if (alerts.length === 0) {
    return (
      <div className="flex items-center justify-center py-8 text-center">
        <div className="text-sm text-green-400">✓ No active alerts</div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {alerts.slice(0, 8).map((alert) => (
        <div
          key={alert.id}
          className="rounded-lg border-l-4 border-slate-600 bg-slate-800 p-3"
        >
          <div className="mb-2 flex items-center justify-between">
            <span className="text-xs text-slate-400">{STAGE_LABELS[alert.stage]}</span>
            <div className="flex items-center gap-2">
              <Badge variant={getSeverityVariant(alert.severity)} className="text-xs">
                {alert.severity}
              </Badge>
              <Button
                onClick={() => onResolve(alert.id)}
                variant="outline"
                size="sm"
                className="text-xs"
              >
                Resolve
              </Button>
            </div>
          </div>
          <div className="text-xs text-slate-400">
            {alert.actualLatencyMs}ms / {alert.budgetMs}ms budget (+{(alert.deviationPercent || 0).toFixed(1)}%)
          </div>
        </div>
      ))}
    </div>
  );
}
