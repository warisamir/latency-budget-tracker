import { RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar, Legend, Tooltip, ResponsiveContainer } from "recharts";
import type { StageStats } from "../../types";

interface Props {
  stageStats: StageStats[];
}

export default function StageRadarChart({ stageStats }: Props) {
  const data = (stageStats || []).map((s) => ({
    stage: s.stage.replace(/_/g, " ").slice(0, 10),
    p95: s.p95Ms,
    budget: s.budgetMs,
    health: s.healthy ? 100 : 50,
  }));

  if (!data || data.length === 0) {
    return (
      <div className="flex h-64 items-center justify-center text-slate-400">
        No health data available
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={300}>
      <RadarChart data={data}>
        <PolarGrid stroke="#334155" />
        <PolarAngleAxis dataKey="stage" stroke="#94a3b8" style={{ fontSize: "11px" }} />
        <PolarRadiusAxis stroke="#94a3b8" />
        <Radar name="P95 (ms)" dataKey="p95" stroke="#f97316" fill="#f97316" fillOpacity={0.6} />
        <Radar name="Budget (ms)" dataKey="budget" stroke="#10b981" fill="#10b981" fillOpacity={0.3} />
        <Tooltip contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #475569" }} />
        <Legend />
      </RadarChart>
    </ResponsiveContainer>
  );
}
