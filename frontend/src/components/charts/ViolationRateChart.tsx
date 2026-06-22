import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import type { StageStats } from "../../types";

interface Props {
  stageStats: StageStats[];
}

export default function ViolationRateChart({ stageStats }: Props) {
  const data = stageStats.map((s) => ({
    stage: s.stage.replace(/_/g, " ").slice(0, 12),
    rate: parseFloat(s.violationRate.toFixed(2)),
  }));

  return (
    <ResponsiveContainer width="100%" height={250}>
      <BarChart data={data} layout="vertical" margin={{ top: 5, right: 30, left: 120, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
        <XAxis type="number" stroke="#94a3b8" />
        <YAxis dataKey="stage" type="category" stroke="#94a3b8" width={110} style={{ fontSize: "12px" }} />
        <Tooltip
          contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #475569" }}
          formatter={(value: any) => `${value.toFixed(2)}%`}
        />
        <Bar dataKey="rate" fill="#f97316" radius={[0, 8, 8, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
