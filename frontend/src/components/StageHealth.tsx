import type { StageStats } from "../types";
import { Badge } from "./ui/badge";
import { cn } from "../lib/utils";

const STAGE_LABELS: Record<string, string> = {
  AUTHENTICATION: "Auth",
  VALIDATION: "Validation",
  BUSINESS_LOGIC: "Business Logic",
  CACHE_ACCESS: "Cache",
  DATABASE_QUERY: "Database",
  RESPONSE_SERIALIZATION: "Response",
};

const getSeverityColor = (pct: number): string => {
  if (pct > 150) return "bg-purple-600";
  if (pct > 100) return "bg-red-600";
  if (pct > 75) return "bg-orange-600";
  return "bg-green-600";
};

const getSeverityVariant = (healthy: boolean): "success" | "destructive" => {
  return healthy ? "success" : "destructive";
};

export default function StageHealth({ stage }: { stage: StageStats }) {
  const pct = Math.min((stage.p95Ms / stage.budgetMs) * 100, 200);

  return (
    <div className={cn(
      "rounded-lg border-l-4 border-slate-700 bg-slate-800 p-4",
      stage.healthy ? "border-l-green-600" : "border-l-red-600"
    )}>
      <div className="mb-3 flex items-center justify-between">
        <span className="font-semibold text-white">{STAGE_LABELS[stage.stage]}</span>
        <Badge variant={getSeverityVariant(stage.healthy)}>
          {stage.healthy ? "✓ HEALTHY" : "✗ DEGRADED"}
        </Badge>
      </div>

      {/* Progress bar */}
      <div className="mb-3 overflow-hidden rounded bg-slate-700">
        <div
          className={`h-2 transition-all ${getSeverityColor(pct)}`}
          style={{ width: `${Math.min(pct, 100)}%` }}
        />
        {pct > 100 && (
          <div
            className="h-2 bg-purple-600 opacity-60"
            style={{ marginLeft: `${100}%`, width: `${Math.min(pct - 100, 100)}%` }}
          />
        )}
      </div>

      {/* Stats */}
      <div className="mb-2 flex gap-6 text-xs text-slate-400">
        <span>P50: <span className="font-semibold text-white">{stage.p50Ms.toFixed(1)}ms</span></span>
        <span>P95: <span className="font-semibold text-white">{stage.p95Ms.toFixed(1)}ms</span></span>
        <span>P99: <span className="font-semibold text-white">{stage.p99Ms.toFixed(1)}ms</span></span>
        <span>Budget: <span className="font-semibold text-white">{stage.budgetMs}ms</span></span>
      </div>

      {/* Violations */}
      {stage.violations > 0 && (
        <div className="text-xs text-orange-400">
          ⚠ {stage.violations} violations ({stage.violationRate.toFixed(1)}% rate)
        </div>
      )}
    </div>
  );
}
