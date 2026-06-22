import { cn } from "../../lib/utils";

interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: "default" | "success" | "warning" | "destructive" | "secondary";
}

export function Badge({ className, variant = "default", ...props }: BadgeProps) {
  return (
    <div
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold",
        {
          "bg-blue-600 text-white": variant === "default",
          "bg-green-600 text-white": variant === "success",
          "bg-yellow-600 text-white": variant === "warning",
          "bg-red-600 text-white": variant === "destructive",
          "bg-slate-700 text-slate-100": variant === "secondary",
        },
        className
      )}
      {...props}
    />
  );
}
