import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";
import type { DataPoint } from "../../types";

interface Props {
  dataPoints: DataPoint[];
}

export default function LatencyTrendChart({ dataPoints }: Props) {
  const data = dataPoints.map((d) => ({
    time: new Date(d.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    requests: d.totalRequests || 0,
    violationRate: d.violationRate ? parseFloat(d.violationRate.toFixed(2)) : 0,
    avgLatency: d.avgLatencyMs ? parseFloat(d.avgLatencyMs.toFixed(2)) : 0,
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 5 }}>
        <defs>
          <linearGradient id="colorRequests" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8} />
            <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
          </linearGradient>
          <linearGradient id="colorViolation" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#ef4444" stopOpacity={0.8} />
            <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
        <XAxis dataKey="time" stroke="#94a3b8" style={{ fontSize: "12px" }} />
        <YAxis stroke="#94a3b8" yAxisId="left" label={{ value: "Requests", angle: -90, position: "insideLeft" }} />
        <YAxis yAxisId="right" orientation="right" stroke="#94a3b8" label={{ value: "Violation Rate (%)", angle: 90, position: "insideRight" }} />
        <Tooltip contentStyle={{ backgroundColor: "#1e293b", border: "1px solid #475569" }} />
        <Legend />
        <Area
          yAxisId="left"
          type="monotone"
          dataKey="requests"
          stroke="#3b82f6"
          fillOpacity={1}
          fill="url(#colorRequests)"
          name="Total Requests"
        />
        <Area
          yAxisId="right"
          type="monotone"
          dataKey="violationRate"
          stroke="#ef4444"
          fillOpacity={1}
          fill="url(#colorViolation)"
          name="Violation Rate (%)"
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
