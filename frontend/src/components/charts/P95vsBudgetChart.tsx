import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";
import type { StageStats } from "../../types";

interface Props {
  stageStats: StageStats[];
}

export default function P95vsBudgetChart({ stageStats }: Props) {
  const data = stageStats.map((s) => ({
    stage: s.stage.replace(/_/g, " "),
    "P95 Latency": s.p95Ms,
    "Remaining Budget": Math.max(0, s.budgetMs - s.p95Ms),
  }));

  return (
    <ResponsiveContainer width="100%" height={320}>
      <BarChart data={data} margin={{ top: 20, right: 30, left: 0, bottom: 80 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
        <XAxis dataKey="stage" stroke="#94a3b8" style={{ fontSize: "12px" }} angle={-45} textAnchor="end" height={90} />
        <YAxis stroke="#94a3b8" label={{ value: "Latency (ms)", angle: -90, position: "insideLeft" }} />
        <Tooltip contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #475569" }} />
        <Legend />
        <Bar dataKey="P95 Latency" stackId="a" fill="#f97316" />
        <Bar dataKey="Remaining Budget" stackId="a" fill="#10b981" />
      </BarChart>
    </ResponsiveContainer>
  );
}
