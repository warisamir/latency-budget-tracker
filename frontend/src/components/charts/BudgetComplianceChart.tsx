import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from "recharts";
import type { StageStats } from "../../types";

interface Props {
  stageStats: StageStats[];
}

export default function BudgetComplianceChart({ stageStats }: Props) {
  const data = stageStats.map((s) => ({
    stage: s.stage.replace(/_/g, " ").slice(0, 12),
    compliance: s.p95Ms <= s.budgetMs ? 100 : Math.round((s.budgetMs / s.p95Ms) * 100),
    healthy: s.healthy,
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data} margin={{ top: 20, right: 30, left: 0, bottom: 80 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
        <XAxis dataKey="stage" stroke="#94a3b8" style={{ fontSize: "12px" }} angle={-45} textAnchor="end" height={90} />
        <YAxis stroke="#94a3b8" label={{ value: "Compliance (%)", angle: -90, position: "insideLeft" }} />
        <Tooltip
          contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #475569" }}
          formatter={(value: any) => `${Math.min(100, value)}%`}
        />
        <Bar dataKey="compliance" radius={[8, 8, 0, 0]}>
          {data.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={entry.healthy ? "#10b981" : "#ef4444"} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
