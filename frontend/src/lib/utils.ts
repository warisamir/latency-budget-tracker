export function cn(...classes: any[]): string {
  return classes
    .flat()
    .filter((x): x is string => typeof x === "string" && x.length > 0)
    .join(" ");
}
