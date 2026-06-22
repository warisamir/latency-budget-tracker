import { PieChart, Pie, Cell, Legend, Tooltip, ResponsiveContainer } from "recharts";

interface Props {
  severityCounts?: Record<string, number>;
}

const SEVERITY_COLORS: Record<string, string> = {
  OK: "#10b981",
  LOW: "#3b82f6",
  MEDIUM: "#fbbf24",
  HIGH: "#f97316",
  CRITICAL: "#ef4444",
};

export default function SeverityPieChart({ severityCounts = {} }: Props) {
  const counts = severityCounts && typeof severityCounts === 'object' ? severityCounts : {};
  const data = (Object.entries(counts) || [])
    .map(([name, value]) => ({
      name,
      value: value || 0,
    }))
    .filter((d) => d.value > 0);

  if (data.length === 0) {
    return (
      <div className="flex h-64 items-center justify-center text-slate-400">
        No severity data available
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={250}>
      <PieChart>
        <Pie
          data={data}
          cx="50%"
          cy="50%"
          innerRadius={50}
          outerRadius={80}
          paddingAngle={2}
          dataKey="value"
        >
          {data.map((entry) => (
            <Cell key={`cell-${entry.name}`} fill={SEVERITY_COLORS[entry.name] || "#64748b"} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #475569" }}
          formatter={(value: any) => value}
        />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}
