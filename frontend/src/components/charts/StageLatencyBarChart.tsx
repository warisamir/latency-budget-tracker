import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";
import type { StageStats } from "../../types";

interface Props {
  stageStats: StageStats[];
}

const COLORS = { p50: "#3b82f6", p95: "#f97316", p99: "#ef4444", budget: "#10b981" };

export default function StageLatencyBarChart({ stageStats }: Props) {
  const data = stageStats.map((s) => ({
    stage: s.stage.replace(/_/g, " "),
    P50: s.p50Ms,
    P95: s.p95Ms,
    P99: s.p99Ms,
    Budget: s.budgetMs,
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
        <XAxis dataKey="stage" stroke="#94a3b8" style={{ fontSize: "12px" }} angle={-45} textAnchor="end" height={80} />
        <YAxis stroke="#94a3b8" label={{ value: "Latency (ms)", angle: -90, position: "insideLeft" }} />
        <Tooltip
          contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #475569" }}
          formatter={(value: any) => typeof value === 'number' ? value.toFixed(2) : value}
        />
        <Legend />
        <Bar dataKey="P50" fill={COLORS.p50} />
        <Bar dataKey="P95" fill={COLORS.p95} />
        <Bar dataKey="P99" fill={COLORS.p99} />
        <Bar dataKey="Budget" fill={COLORS.budget} />
      </BarChart>
    </ResponsiveContainer>
  );
}
