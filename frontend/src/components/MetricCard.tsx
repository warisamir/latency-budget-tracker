import { Card, CardContent } from "./ui/card";

interface MetricCardProps {
  label: string;
  value: string | number;
  color?: string;
}

export default function MetricCard({ label, value, color = "text-blue-400" }: MetricCardProps) {
  return (
    <Card>
      <CardContent className="flex flex-col items-center justify-center py-6">
        <div className={`text-3xl font-bold ${color}`}>{value}</div>
        <div className="mt-2 text-xs text-slate-400">{label}</div>
      </CardContent>
    </Card>
  );
}
